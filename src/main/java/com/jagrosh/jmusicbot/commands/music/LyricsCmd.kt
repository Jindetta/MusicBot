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
import com.jagrosh.jlyrics.Lyrics
import com.jagrosh.jlyrics.LyricsClient
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.MusicCommand
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class LyricsCmd(bot: Bot) : MusicCommand(bot) {
    private val client = LyricsClient()

    init {
        name = "lyrics"
        arguments = "[song name]"
        help = "shows the lyrics of a song"
        aliases = bot.config.getAliases(name)
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun runCommand(event: CommandEvent) {
        val title: String = event.args.ifEmpty {
            val handler = event.audioHandler()

            if (handler.isMusicPlaying(event.jda)) handler.player.playingTrack.info.title else {
                event.replyError("There must be music playing to use that!")
                return
            }
        }
        event.channel.sendTyping().queue()
        client.getLyrics(title).thenAccept { lyrics: Lyrics? ->
            if (lyrics == null) {
                event.replyError("Lyrics for `" + title + "` could not be found!" + if (event.args.isEmpty()) " Try entering the song name manually (`lyrics [song name]`)" else "")
                return@thenAccept
            }
            val eb = EmbedBuilder()
                .setAuthor(lyrics.author)
                .setColor(event.selfMember.color)
                .setTitle(lyrics.title, lyrics.url)
            if (lyrics.content.length > 15000) {
                event.replyWarning("Lyrics for `" + title + "` found but likely not correct: " + lyrics.url)
            } else if (lyrics.content.length > 2000) {
                var content = lyrics.content.trim { it <= ' ' }
                while (content.length > 2000) {
                    var index = content.lastIndexOf("\n\n", 2000)
                    if (index == -1) index = content.lastIndexOf("\n", 2000)
                    if (index == -1) index = content.lastIndexOf(" ", 2000)
                    if (index == -1) index = 2000
                    event.reply(eb.setDescription(content.substring(0, index).trim { it <= ' ' }).build())
                    content = content.substring(index).trim { it <= ' ' }
                    eb.setAuthor(null).setTitle(null, null)
                }
                event.reply(eb.setDescription(content).build())
            } else event.reply(eb.setDescription(lyrics.content).build())
        }
    }
}
