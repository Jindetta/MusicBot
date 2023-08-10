/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio

import com.jagrosh.jmusicbot.Bot
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.exceptions.RateLimitedException
import java.util.concurrent.TimeUnit

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class NowPlayingHandler(private val bot: Bot) {
    private val mostRecentlyPlayed // guild -> channel,message
            : HashMap<Long, Pair<Long, Long>> = HashMap()

    fun init() {
        if (!bot.config.useNPImages()) {
            bot.threadPool.scheduleWithFixedDelay({ updateAll() }, 0, 5, TimeUnit.SECONDS)
        }
    }

    fun setLastNPMessage(message: Message) {
        mostRecentlyPlayed[message.guild.idLong] = Pair(message.textChannel.idLong, message.idLong)
    }

    fun clearLastNPMessage(guild: Guild) {
        mostRecentlyPlayed.remove(guild.idLong)
    }

    private fun updateAll() {
        val toRemove: MutableSet<Long> = HashSet()
        for (guildId in mostRecentlyPlayed.keys) {
            val guild = bot.jda?.getGuildById(guildId)
            if (guild == null) {
                toRemove.add(guildId)
                continue
            }
            val pair = mostRecentlyPlayed[guildId]!!
            val tc = guild.getTextChannelById(pair.first)
            if (tc == null) {
                toRemove.add(guildId)
                continue
            }
            val handler = guild.audioManager.sendingHandler as AudioHandler?
            var msg = handler!!.getNowPlaying(bot.jda)
            if (msg == null) {
                msg = handler.getNoMusicPlaying(bot.jda)
                toRemove.add(guildId)
            }
            try {
                tc.editMessageById(pair.second, msg)
                    .queue({ m: Message? -> }) { t: Throwable? -> mostRecentlyPlayed.remove(guildId) }
            } catch (e: Exception) {
                toRemove.add(guildId)
            }
        }
        toRemove.forEach { id: Long -> mostRecentlyPlayed.remove(id) }
    }

    fun updateTopic(guildId: Long, handler: AudioHandler, wait: Boolean) {
        val guild = bot.jda?.getGuildById(guildId) ?: return
        val settings = bot.settingsManager.getSettings(guildId)
        val tchan = settings.getTextChannel(guild)
        if (tchan != null && guild.selfMember.hasPermission(tchan, Permission.MANAGE_CHANNEL)) {
            val otherText: String
            val topic = tchan.topic
            otherText =
                if (topic.isNullOrEmpty()) "\u200B" else if (topic.contains("\u200B")) topic.substring(
                    topic.lastIndexOf("\u200B")
                ) else "\u200B\n $topic"
            val text = handler.getTopicFormat(bot.jda) + otherText
            if (text != tchan.topic) {
                try {
                    // normally here if 'wait' was false, we'd want to queue, however,
                    // new discord ratelimits specifically limiting changing channel topics
                    // mean we don't want a backlog of changes piling up, so if we hit a 
                    // ratelimit, we just won't change the topic this time
                    tchan.manager.setTopic(text).complete(wait)
                } catch (ignore: PermissionException) {
                } catch (ignore: RateLimitedException) {
                }
            }
        }
    }

    // "event"-based methods
    fun onTrackUpdate(guildId: Long, track: AudioTrack?, handler: AudioHandler) {
        // update bot status if applicable
        if (bot.config.songInStatus) {
            if (track != null && (bot.jda?.guilds?.stream()
                    ?.filter { g: Guild -> g.selfMember.voiceState!!.inVoiceChannel() }?.count() ?: 0) <= 1
            ) bot.jda?.presence?.activity = Activity.listening(track.info.title) else bot.resetGame()
        }

        // update channel topic if applicable
        updateTopic(guildId, handler, false)
    }

    fun onMessageDelete(guild: Guild, messageId: Long) {
        val pair = mostRecentlyPlayed[guild.idLong] ?: return
        if (pair.second == messageId) {
            mostRecentlyPlayed.remove(guild.idLong)
        }
    }
}
