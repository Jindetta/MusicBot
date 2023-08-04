/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot

import com.jagrosh.jmusicbot.utils.OtherUtil
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class Listener(private val bot: Bot) : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) {
        if (event.jda.guildCache.isEmpty) {
            val log = LoggerFactory.getLogger("MusicBot")
            log.warn("This bot is not on any guilds! Use the following link to add the bot to your guilds!")
            log.warn(event.jda.getInviteUrl(*JMusicBot.RECOMMENDED_PERMS))
        }
        credit(event.jda)
        event.jda.guilds.forEach(Consumer { guild: Guild ->
            try {
                val defpl = bot.settingsManager.getSettings(guild).defaultPlaylist
                val vc = bot.settingsManager.getSettings(guild).getVoiceChannel(guild)
                if (defpl != null && vc != null && bot.playerManager.setUpHandler(guild)?.playFromDefault() == true) {
                    guild.audioManager.openAudioConnection(vc)
                }
            } catch (ignore: Exception) {
            }
        })
        if (bot.config.useUpdateAlerts()) {
            bot.threadPool.scheduleWithFixedDelay({
                try {
                    val owner = bot.jda?.retrieveUserById(bot.config.ownerId)?.complete()
                    if (owner != null) {
                        val currentVersion = OtherUtil.currentVersion
                        val latestVersion = OtherUtil.latestVersion
                        if (latestVersion != null && !currentVersion.equals(latestVersion, ignoreCase = true)) {
                            val msg = String.format(OtherUtil.NEW_VERSION_AVAILABLE, currentVersion, latestVersion)
                            owner.openPrivateChannel().queue { pc: PrivateChannel -> pc.sendMessage(msg).queue() }
                        }
                    }
                } catch (_: Exception) {
                } // ignored
            }, 0, 24, TimeUnit.HOURS)
        }
    }

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        bot.nowPlayingHandler.onMessageDelete(event.guild, event.messageIdLong)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        bot.aloneInVoiceHandler.onVoiceUpdate(event)
    }

    override fun onShutdown(event: ShutdownEvent) {
        bot.shutdown()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        credit(event.jda)
    }

    // make sure people aren't adding clones to dbots
    private fun credit(jda: JDA) {
        val dbots = jda.getGuildById(110373943822540800L) ?: return
        if (bot.config.dBots) return
        jda.getTextChannelById(119222314964353025L)
            ?.sendMessage("This account is running JMusicBot. Please do not list bot clones on this server, <@" + bot.config.ownerId + ">.")
            ?.complete()

        dbots.leave().queue()
    }
}
