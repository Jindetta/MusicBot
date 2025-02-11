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
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.MusicCommand
import java.util.function.Consumer

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class PlaylistsCmd(bot: Bot) : MusicCommand(bot) {
    init {
        name = "playlists"
        help = "shows the available playlists"
        aliases = bot.config.getAliases(name)
        guildOnly = true
        beListening = false
    }

    override fun runCommand(event: CommandEvent) {
        if (!bot.playlistLoader.folderExists()) {
            bot.playlistLoader.createFolder()
        }

        if (!bot.playlistLoader.folderExists()) {
            event.reply("${event.client.warning} Playlists folder does not exist and could not be created!")
            return
        }

        val list = bot.playlistLoader.playlistNames

        if (list.isEmpty()) {
            event.reply("${event.client.warning} There are no playlists in the Playlists folder!")
        } else {
            val stringBuilder = StringBuilder("${event.client.success} Available playlists:\n")

            for (name in list) {
                stringBuilder.append("`$name` ")
            }

            event.reply(stringBuilder.append("\nType `").append(event.client.textualPrefix)
                .append("play playlist <name>` to play a playlist").toString())
        }
    }
}
