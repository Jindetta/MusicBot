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
package com.jagrosh.jmusicbot.commands.owner

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.OwnerCommand
import java.io.IOException
import java.util.function.Consumer

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class PlaylistCmd(private val bot: Bot) : OwnerCommand() {
    init {
        guildOnly = false
        name = "playlist"
        arguments = "<append|delete|make|setdefault>"
        help = "playlist management"
        aliases = bot.config.getAliases(name)
        children = arrayOf(
            ListCmd(),
            AppendlistCmd(),
            DeletelistCmd(),
            MakelistCmd(),
            DefaultlistCmd(bot)
        )
    }

    public override fun execute(event: CommandEvent) {
        val builder = StringBuilder(event.client.warning + " Playlist Management Commands:\n")
        for (cmd in children) builder.append("\n`").append(event.client.prefix).append(name).append(" ")
            .append(cmd.name)
            .append(" ").append(if (cmd.arguments == null) "" else cmd.arguments).append("` - ").append(cmd.help)
        event.reply(builder.toString())
    }

    inner class MakelistCmd : OwnerCommand() {
        init {
            this.name = "make"
            this.aliases = arrayOf("create")
            this.help = "makes a new playlist"
            this.arguments = "<name>"
            this.guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            var pname = event.args.replace("\\s+".toRegex(), "_")
            pname = pname.replace("[*?|\\/\":<>]".toRegex(), "")
            if (pname == null || pname.isEmpty()) {
                event.replyError("Please provide a name for the playlist!")
            } else if (bot.playlistLoader.getPlaylist(pname) == null) {
                try {
                    bot.playlistLoader.createPlaylist(pname)
                    event.reply(event.client.success + " Successfully created playlist `" + pname + "`!")
                } catch (e: IOException) {
                    event.reply(event.client.error + " I was unable to create the playlist: " + e.localizedMessage)
                }
            } else event.reply(event.client.error + " Playlist `" + pname + "` already exists!")
        }
    }

    inner class DeletelistCmd : OwnerCommand() {
        init {
            this.name = "delete"
            this.aliases = arrayOf("remove")
            this.help = "deletes an existing playlist"
            this.arguments = "<name>"
            this.guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            val pname = event.args.replace("\\s+".toRegex(), "_")
            if (bot.playlistLoader.getPlaylist(pname) == null) event.reply(event.client.error + " Playlist `" + pname + "` doesn't exist!") else {
                try {
                    bot.playlistLoader.deletePlaylist(pname)
                    event.reply(event.client.success + " Successfully deleted playlist `" + pname + "`!")
                } catch (e: IOException) {
                    event.reply(event.client.error + " I was unable to delete the playlist: " + e.localizedMessage)
                }
            }
        }
    }

    inner class AppendlistCmd : OwnerCommand() {
        init {
            this.name = "append"
            this.aliases = arrayOf("add")
            this.help = "appends songs to an existing playlist"
            this.arguments = "<name> <URL> | <URL> | ..."
            this.guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            val parts = event.args.split("\\s+".toRegex(), limit = 2).toTypedArray()
            if (parts.size < 2) {
                event.reply(event.client.error + " Please include a playlist name and URLs to add!")
                return
            }
            val pname = parts[0]
            val playlist = bot.playlistLoader.getPlaylist(pname)
            if (playlist == null) event.reply(event.client.error + " Playlist `" + pname + "` doesn't exist!") else {
                val builder = StringBuilder()
                playlist.items.forEach(Consumer { item: String? -> builder.append("\r\n").append(item) })
                val urls = parts[1].split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (url in urls) {
                    var u = url.trim { it <= ' ' }
                    if (u.startsWith("<") && u.endsWith(">")) u = u.substring(1, u.length - 1)
                    builder.append("\r\n").append(u)
                }
                try {
                    bot.playlistLoader.writePlaylist(pname, builder.toString())
                    event.reply(event.client.success + " Successfully added " + urls.size + " items to playlist `" + pname + "`!")
                } catch (e: IOException) {
                    event.reply(event.client.error + " I was unable to append to the playlist: " + e.localizedMessage)
                }
            }
        }
    }

    inner class DefaultlistCmd(bot: Bot) : AutoPlaylistCmd(bot) {
        init {
            this.name = "setdefault"
            this.aliases = arrayOf("default")
            this.arguments = "<playlistname|NONE>"
            this.guildOnly = true
        }
    }

    inner class ListCmd : OwnerCommand() {
        init {
            this.name = "all"
            this.aliases = arrayOf("available", "list")
            this.help = "lists all available playlists"
            this.guildOnly = true
        }

        override fun execute(event: CommandEvent) {
            if (!bot.playlistLoader.folderExists()) bot.playlistLoader.createFolder()
            if (!bot.playlistLoader.folderExists()) {
                event.reply(event.client.warning + " Playlists folder does not exist and could not be created!")
                return
            }
            val list = bot.playlistLoader.playlistNames
            if (list.isEmpty()) event.reply(
                event.client.warning + " There are no playlists in the Playlists folder!"
            ) else {
                val builder = StringBuilder(event.client.success + " Available playlists:\n")
                list.forEach(Consumer { str: String? -> builder.append("`").append(str).append("` ") })
                event.reply(builder.toString())
            }
        }
    }
}
