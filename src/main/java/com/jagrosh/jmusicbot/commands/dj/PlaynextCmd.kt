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
package com.jagrosh.jmusicbot.commands.dj

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audio.AudioHandler
import com.jagrosh.jmusicbot.audio.QueuedTrack
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand
import com.jagrosh.jmusicbot.filter
import com.jagrosh.jmusicbot.utils.FormatUtil
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Message

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class PlaynextCmd(bot: Bot) : DJCommand(bot) {
    private val loadingEmoji: String

    init {
        loadingEmoji = bot.config.loading ?: ""
        name = "playnext"
        arguments = "<title|URL>"
        help = "plays a single song next"
        aliases = bot.config.getAliases(name)
        beListening = true
        bePlaying = false
    }

    override fun runCommand(event: CommandEvent) {
        if (event.args.isEmpty() && event.message.attachments.isEmpty()) {
            event.replyWarning("Please include a song title or URL!")
            return
        }
        val args = if (event.args.startsWith("<") && event.args.endsWith(">")) event.args.substring(
            1,
            event.args.length - 1
        ) else if (event.args.isEmpty()) event.message.attachments[0].url else event.args
        event.reply("$loadingEmoji Loading... `[$args]`") { m: Message? ->
            bot.playerManager.loadItemOrdered(
                event.guild,
                args,
                ResultHandler(m!!, event, false)
            )
        }
    }

    private inner class ResultHandler(
        private val m: Message,
        private val event: CommandEvent,
        private val ytsearch: Boolean
    ) : AudioLoadResultHandler {
        private fun loadSingle(track: AudioTrack) {
            if (bot.config.isTooLong(track)) {
                m.editMessage(
                        "${event.client.warning} This track (**${track.info.title}**) is longer than the allowed maximum: `${FormatUtil.formatTime(track.duration)}` > `${FormatUtil.formatTime(bot.config.maxSeconds * 1000)}`".filter()
                ).queue()
                return
            }
            val handler = event.audioHandler()
            val pos = handler.addTrackToFront(QueuedTrack(track, event.author)) + 1
            val addMsg = ("${event.client.success} Added **${track.info.title}** (`${FormatUtil.formatTime(track.duration)}`) " + if (pos == 0) "to begin playing" else " to the queue at position $pos").filter()
            m.editMessage(addMsg).queue()
        }

        override fun trackLoaded(track: AudioTrack) {
            loadSingle(track)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            val single: AudioTrack
            single =
                if (playlist.tracks.size == 1 || playlist.isSearchResult) if (playlist.selectedTrack == null) playlist.tracks[0] else playlist.selectedTrack else if (playlist.selectedTrack != null) playlist.selectedTrack else playlist.tracks[0]
            loadSingle(single)
        }

        override fun noMatches() {
            if (ytsearch) m.editMessage((event.client.warning + " No results found for `" + event.args + "`.").filter())
                .queue() else bot.playerManager.loadItemOrdered(
                event.guild,
                "ytsearch:" + event.args,
                ResultHandler(m, event, true)
            )
        }

        override fun loadFailed(throwable: FriendlyException) {
            if (throwable.severity == FriendlyException.Severity.COMMON) m.editMessage(event.client.error + " Error loading: " + throwable.message)
                .queue() else m.editMessage(event.client.error + " Error loading track.").queue()
        }
    }
}
