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
import com.jagrosh.jdautilities.menu.OrderedMenu
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audio.QueuedTrack
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.MusicCommand
import com.jagrosh.jmusicbot.utils.FormatUtil
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.util.concurrent.TimeUnit

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
open class SearchCmd(bot: Bot) : MusicCommand(bot) {
    protected var searchPrefix = "ytsearch:"
    private val builder: OrderedMenu.Builder
    private val searchingEmoji: String

    init {
        searchingEmoji = bot.config.searching ?: ""
        name = "search"
        aliases = bot.config.getAliases(name)
        arguments = "<query>"
        help = "searches Youtube for a provided query"
        beListening = true
        bePlaying = false
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        builder = OrderedMenu.Builder()
            .allowTextInput(true)
            .useNumbers()
            .useCancelButton(true)
            .setEventWaiter(bot.waiter)
            .setTimeout(1, TimeUnit.MINUTES)
    }

    override fun runCommand(event: CommandEvent) {
        if (event.args.isEmpty()) {
            event.replyError("Please include a query.")
            return
        }

        event.reply("$searchingEmoji Searching... `[${event.args}]`") { message ->
            bot.playerManager.loadItemOrdered(event.guild, searchPrefix + event.args, ResultHandler(message, event))
        }
    }

    private inner class ResultHandler(
        private val message: Message,
        private val event: CommandEvent
    ) : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            if (bot.config.isTooLong(track)) {
                message.editMessage(
                    FormatUtil.filter(
                        event.client.warning + " This track (**" + track.info.title + "**) is longer than the allowed maximum: `"
                                + FormatUtil.formatTime(track.duration) + "` > `" + bot.config.maxTime + "`"
                    )
                ).queue()
                return
            }
            val handler = event.audioHandler()
            val pos = handler.addTrack(QueuedTrack(track, event.author)) + 1
            message.editMessage(
                FormatUtil.filter(
                    event.client.success + " Added **" + track.info.title
                            + "** (`" + FormatUtil.formatTime(track.duration) + "`) " + if (pos == 0) "to begin playing" else " to the queue at position $pos"
                )
            ).queue()
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            builder.setColor(event.selfMember.color)
                .setText(FormatUtil.filter(event.client.success + " Search results for `" + event.args + "`:"))
                .setChoices(*arrayOfNulls(0))
                .setSelection { _: Message?, i: Int ->
                    val track = playlist.tracks[i - 1]
                    if (bot.config.isTooLong(track)) {
                        event.replyWarning(
                            "This track (**" + track.info.title + "**) is longer than the allowed maximum: `"
                                    + FormatUtil.formatTime(track.duration) + "` > `" + bot.config.maxTime + "`"
                        )
                        return@setSelection
                    }
                    val handler = event.audioHandler()
                    val pos = handler.addTrack(QueuedTrack(track, event.author)) + 1
                    event.replySuccess(
                        "Added **" + FormatUtil.filter(track.info.title)
                                + "** (`" + FormatUtil.formatTime(track.duration) + "`) " + if (pos == 0) "to begin playing" else " to the queue at position $pos"
                    )
                }
                .setCancel { msg: Message? -> }
                .setUsers(event.author)
            var i = 0
            while (i < 4 && i < playlist.tracks.size) {
                val track = playlist.tracks[i]
                builder.addChoices("`[" + FormatUtil.formatTime(track.duration) + "]` [**" + track.info.title + "**](" + track.info.uri + ")")
                i++
            }
            builder.build().display(message)
        }

        override fun noMatches() {
            message.editMessage(FormatUtil.filter(event.client.warning + " No results found for `" + event.args + "`."))
                .queue()
        }

        override fun loadFailed(throwable: FriendlyException) {
            if (throwable.severity == FriendlyException.Severity.COMMON) message.editMessage(event.client.error + " Error loading: " + throwable.message)
                .queue() else message.editMessage(event.client.error + " Error loading track.").queue()
        }
    }
}
