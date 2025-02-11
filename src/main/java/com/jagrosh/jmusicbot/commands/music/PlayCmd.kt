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
package com.jagrosh.jmusicbot.commands.music

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.menu.ButtonMenu
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audio.QueuedTrack
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand
import com.jagrosh.jmusicbot.commands.MusicCommand
import com.jagrosh.jmusicbot.filter
import com.jagrosh.jmusicbot.playlist.PlaylistLoader.PlaylistLoadError
import com.jagrosh.jmusicbot.utils.FormatUtil
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote
import net.dv8tion.jda.api.exceptions.PermissionException
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class PlayCmd(bot: Bot) : MusicCommand(bot) {
    private val loadingEmoji: String?

    init {
        loadingEmoji = bot.config.loading
        name = "play"
        arguments = "<title|URL|subcommand>"
        help = "plays the provided song"
        aliases = bot.config.getAliases(name)
        beListening = true
        bePlaying = false
        children = arrayOf(PlaylistCmd(bot))
    }

    override fun runCommand(event: CommandEvent) {
        if (event.args.isEmpty() && event.message.attachments.isEmpty()) {
            val handler = event.audioHandler()

            if (handler.player.playingTrack != null && handler.player.isPaused) {
                if (DJCommand.checkDJPermission(event)) {
                    handler.player.isPaused = false
                    event.replySuccess("Resumed **${handler.player.playingTrack.info.title}**.")
                } else {
                    event.replyError("Only DJs can unpause the player!")
                }
            } else {
                val stringBuilder = StringBuilder("${event.client.warning} Play Commands:\n")
                    .append("\n`")
                    .append(event.client.prefix)
                    .append(name)
                    .append(" <song title>` - plays the first result from Youtube")
                    .append("\n`").append(event.client.prefix).append(name)
                    .append(" <URL>` - plays the provided song, playlist, or stream")

                for (command in children) {
                    stringBuilder.append("\n`")
                        .append(event.client.prefix)
                        .append(name).append(" ")
                        .append(command.name)
                        .append(" ")
                        .append(command.arguments)
                        .append("` - ")
                        .append(command.help)
                }

                event.reply(stringBuilder.toString())
            }

            return
        }

        val args = if (event.args.startsWith("<") && event.args.endsWith(">")) event.args.substring(
            1,
            event.args.length - 1
        ) else if (event.args.isEmpty()) event.message.attachments[0].url else event.args

        event.reply("$loadingEmoji Loading... `[$args]`") { message ->
            bot.playerManager.loadItemOrdered(
                event.guild,
                args,
                ResultHandler(message!!, event, false)
            )
        }
    }

    private inner class ResultHandler(
        private val message: Message,
        private val event: CommandEvent,
        private val searchYT: Boolean
    ) : AudioLoadResultHandler {
        private fun loadSingle(track: AudioTrack, playlist: AudioPlaylist?) {
            if (bot.config.isTooLong(track)) {
                message.editMessage(
                    FormatUtil.filter(
                        event.client.warning + " This track (**" + track.info.title + "**) is longer than the allowed maximum: `"
                                + FormatUtil.formatTime(track.duration) + "` > `" + FormatUtil.formatTime(bot.config.maxSeconds * 1000) + "`"
                    )
                ).queue()
                return
            }
            val handler = event.audioHandler()
            val pos = handler.addTrack(QueuedTrack(track, event.author)) + 1
            val addMsg = FormatUtil.filter(
                "${event.client.success} Added **${track.info.title}** (`${FormatUtil.formatTime(track.duration)}`) " + if (pos == 0) "to begin playing" else " to the queue at position $pos"
            )
            if (playlist == null || !event.selfMember.hasPermission(
                    event.textChannel,
                    Permission.MESSAGE_ADD_REACTION
                )
            ) message.editMessage(addMsg).queue() else {
                ButtonMenu.Builder()
                    .setText(
                        """$addMsg
${event.client.warning} This track has a playlist of **${playlist.tracks.size}** tracks attached. Select $LOAD to load playlist."""
                    )
                    .setChoices(LOAD, CANCEL)
                    .setEventWaiter(bot.waiter)
                    .setTimeout(30, TimeUnit.SECONDS)
                    .setAction { reactionEmote ->
                        if (reactionEmote.name == LOAD) message.editMessage(
                            """$addMsg
${event.client.success} Loaded **${loadPlaylist(playlist, track)}** additional tracks!"""
                        ).queue() else message.editMessage(addMsg).queue()
                    }.setFinalAction { message ->
                        try {
                            message.clearReactions().queue()
                        } catch (ignore: PermissionException) {
                        }
                    }.build().display(message)
            }
        }

        private fun loadPlaylist(playlist: AudioPlaylist, exclude: AudioTrack?): Int {
            val handler = event.audioHandler()
            var count = 0

            for (track in playlist.tracks) {
                if (!bot.config.isTooLong(track) && track != exclude) {
                    handler.addTrack(QueuedTrack(track, event.author))
                    count++
                }
            }

            return count
        }

        override fun trackLoaded(track: AudioTrack) {
            loadSingle(track, null)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.tracks.size == 1 || playlist.isSearchResult) {
                val single = if (playlist.selectedTrack == null) playlist.tracks[0] else playlist.selectedTrack
                loadSingle(single, null)
            } else if (playlist.selectedTrack != null) {
                loadSingle(playlist.selectedTrack, playlist)
            } else {
                val count = loadPlaylist(playlist, null)

                if (playlist.tracks.isEmpty()) {
                    message.editMessage(
                        FormatUtil.filter(
                            "${event.client.warning} The playlist " + (if (playlist.name == null) "" else ("(**" + playlist.name
                                    + "**) ")) + " could not be loaded or contained 0 entries"
                        )
                    ).queue()
                } else if (count == 0) {
                    message.editMessage(
                        FormatUtil.filter(
                            "${event.client.warning} All entries in this playlist " + (if (playlist.name == null) "" else ("(**" + playlist.name
                                    + "**) ")) + "were longer than the allowed maximum (`" + bot.config.maxTime + "`)"
                        )
                    ).queue()
                } else {
                    message.editMessage(
                        FormatUtil.filter(
                            "${event.client.success} Found "
                                    + (if (playlist.name == null) "a playlist" else "playlist **" + playlist.name + "**") + " with `"
                                    + playlist.tracks.size + "` entries; added to the queue!"
                                    + if (count < playlist.tracks.size) """
${event.client.warning} Tracks longer than the allowed maximum (`${bot.config.maxTime}`) have been omitted.""" else ""
                        )
                    ).queue()
                }
            }
        }

        override fun noMatches() {
            if (searchYT) {
                message.editMessage("${event.client.warning} No results found for `${event.args}`.".filter()).queue()
            } else {
                bot.playerManager.loadItemOrdered(
                    event.guild,
                    "ytsearch: ${event.args}",
                    ResultHandler(message, event, true)
                )
            }
        }

        override fun loadFailed(throwable: FriendlyException) {
            if (throwable.severity == FriendlyException.Severity.COMMON) {
                message.editMessage("${event.client.error} Error loading: ${throwable.message}").queue()
            } else {
                message.editMessage("${event.client.error} Error loading track.").queue()
            }
        }
    }

    inner class PlaylistCmd(bot: Bot) : MusicCommand(bot) {
        init {
            this.name = "playlist"
            this.aliases = arrayOf("pl")
            this.arguments = "<name>"
            this.help = "plays the provided playlist"
            this.beListening = true
            this.bePlaying = false
        }

        override fun runCommand(event: CommandEvent) {
            if (event.args.isEmpty()) {
                event.reply("${event.client.error} Please include a playlist name.")
                return
            }

            val playlist = bot.playlistLoader.getPlaylist(event.args)
            if (playlist == null) {
                event.replyError("I could not find `" + event.args + ".txt` in the Playlists folder.")
                return
            }

            event.channel.sendMessage(loadingEmoji + " Loading playlist **" + event.args + "**... (" + playlist.items.size + " items)")
                .queue { m: Message ->
                    val handler = event.audioHandler()
                    playlist.loadTracks(
                        bot.playerManager,
                        { at: AudioTrack? -> handler.addTrack(QueuedTrack(at!!, event.author)) }) {
                        val builder =
                            StringBuilder(if (playlist.tracks.isEmpty()) event.client.warning + " No tracks were loaded!" else event.client.success + " Loaded **" + playlist.tracks.size + "** tracks!")
                        if (playlist.errors.isNotEmpty()) builder.append("\nThe following tracks failed to load:")
                        playlist.errors.forEach(Consumer { err: PlaylistLoadError ->
                            builder.append("\n`[").append(err.index + 1).append("]` **").append(err.item).append("**: ")
                                .append(err.reason)
                        })
                        var str = builder.toString()
                        if (str.length > 2000) str = str.substring(0, 1994) + " (...)"
                        m.editMessage(FormatUtil.filter(str)).queue()
                    }
                }
        }
    }

    companion object {
        private const val LOAD = "\uD83D\uDCE5" // 📥
        private const val CANCEL = "\uD83D\uDEAB" // 🚫
    }
}
