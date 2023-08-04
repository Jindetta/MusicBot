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
package com.jagrosh.jmusicbot.commands.music

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.menu.Paginator
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audio.AudioHandler
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.MusicCommand
import com.jagrosh.jmusicbot.settings.RepeatMode
import com.jagrosh.jmusicbot.settings.Settings
import com.jagrosh.jmusicbot.utils.FormatUtil
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.exceptions.PermissionException
import java.util.concurrent.TimeUnit

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class QueueCmd(bot: Bot) : MusicCommand(bot) {
    private val builder: Paginator.Builder

    init {
        name = "queue"
        help = "shows the current queue"
        arguments = "[pagenum]"
        aliases = bot.config.getAliases(name)
        bePlaying = true
        botPermissions = arrayOf(Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS)
        builder = Paginator.Builder()
            .setColumns(1)
            .setFinalAction { m: Message ->
                try {
                    m.clearReactions().queue()
                } catch (ignore: PermissionException) {
                }
            }
            .setItemsPerPage(10)
            .waitOnSinglePage(false)
            .useNumberedItems(true)
            .showPageNumbers(true)
            .wrapPageEnds(true)
            .setEventWaiter(bot.waiter)
            .setTimeout(1, TimeUnit.MINUTES)
    }

    override fun runCommand(event: CommandEvent) {
        var pagenum = 1
        try {
            pagenum = event.args.toInt()
        } catch (ignore: NumberFormatException) {
        }
        val ah = event.audioHandler()
        if (ah != null) {
            val list = ah.queue.getList()
            if (list.isEmpty()) {
                val nowp = ah.getNowPlaying(event.jda)
                val nonowp = ah.getNoMusicPlaying(event.jda)
                val built = MessageBuilder()
                    .setContent(event.client.warning + " There is no music in the queue!")
                    .setEmbeds((nowp ?: nonowp).embeds[0]).build()
                event.reply(built) { m: Message -> if (nowp != null) bot.nowPlayingHandler.setLastNPMessage(m) }
                return
            }
            val songs = arrayOfNulls<String>(list.size)
            var total: Long = 0
            for (i in list.indices) {
                total += list[i].track.duration
                songs[i] = list[i].toString()
            }
            val settings = event.client.getSettingsFor<Settings>(event.guild)
            val fintotal = total
            builder.setText { i1: Int?, i2: Int? ->
                getQueueTitle(
                    ah,
                    event.client.success,
                    songs.size,
                    fintotal,
                    settings.repeatMode
                )
            }
                .setItems(*songs)
                .setUsers(event.author)
                .setColor(event.selfMember.color)
            builder.build().paginate(event.channel, pagenum)
        }
    }

    private fun getQueueTitle(
        ah: AudioHandler?,
        success: String,
        songslength: Int,
        total: Long,
        repeatmode: RepeatMode
    ): String {
        val sb = StringBuilder()
        if (ah?.player?.playingTrack != null) {
            sb.append(ah.statusEmoji).append(" **")
                .append(ah.player.playingTrack.info.title).append("**\n")
        }
        return FormatUtil.filter(
            sb.append(success).append(" Current Queue | ").append(songslength)
                .append(" entries | `").append(FormatUtil.formatTime(total)).append("` ")
                .append(if (repeatmode.emoji != null) "| " + repeatmode.emoji else "").toString()
        )
    }
}
