/*
 * Copyright 2017 John Grosh <john.a.grosh@gmail.com>.
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
import net.dv8tion.jda.api.entities.Activity
import java.util.*

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SetGameCmd(bot: Bot) : OwnerCommand() {
    init {
        name = "setgame"
        help = "sets the game the bot is playing"
        arguments = "[action] [game]"
        aliases = bot.config.getAliases(name)
        guildOnly = false
        children = arrayOf(
            SetlistenCmd(),
            SetstreamCmd(),
            SetwatchCmd()
        )
    }

    override fun execute(event: CommandEvent) {
        val title = if (event.args.lowercase(Locale.getDefault()).startsWith("playing")) event.args.substring(7)
            .trim { it <= ' ' } else event.args
        try {
            event.jda.presence.activity = if (title.isEmpty()) null else Activity.playing(title)
            event.reply(
                event.client.success + " **" + event.selfUser.name
                        + "** is " + if (title.isEmpty()) "no longer playing anything." else "now playing `$title`"
            )
        } catch (e: Exception) {
            event.reply(event.client.error + " The game could not be set!")
        }
    }

    private inner class SetstreamCmd : OwnerCommand() {
        init {
            this.name = "stream"
            this.aliases = arrayOf("twitch", "streaming")
            this.help = "sets the game the bot is playing to a stream"
            this.arguments = "<username> <game>"
            this.guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            val parts = event.args.split("\\s+".toRegex(), limit = 2).toTypedArray()
            if (parts.size < 2) {
                event.replyError("Please include a twitch username and the name of the game to 'stream'")
                return
            }
            try {
                event.jda.presence.activity = Activity.streaming(parts[1], "https://twitch.tv/" + parts[0])
                event.replySuccess(
                    "**" + event.selfUser.name
                            + "** is now streaming `" + parts[1] + "`"
                )
            } catch (e: Exception) {
                event.reply(event.client.error + " The game could not be set!")
            }
        }
    }

    private inner class SetlistenCmd : OwnerCommand() {
        init {
            this.name = "listen"
            this.aliases = arrayOf("listening")
            this.help = "sets the game the bot is listening to"
            this.arguments = "<title>"
            this.guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            if (event.args.isEmpty()) {
                event.replyError("Please include a title to listen to!")
                return
            }
            val title = if (event.args.lowercase(Locale.getDefault()).startsWith("to")) event.args.substring(2)
                .trim { it <= ' ' } else event.args
            try {
                event.jda.presence.activity = Activity.listening(title)
                event.replySuccess("**" + event.selfUser.name + "** is now listening to `" + title + "`")
            } catch (e: Exception) {
                event.reply(event.client.error + " The game could not be set!")
            }
        }
    }

    private inner class SetwatchCmd : OwnerCommand() {
        init {
            this.name = "watch"
            this.aliases = arrayOf("watching")
            this.help = "sets the game the bot is watching"
            this.arguments = "<title>"
            this.guildOnly = false
        }

        override fun execute(event: CommandEvent) {
            if (event.args.isEmpty()) {
                event.replyError("Please include a title to watch!")
                return
            }
            val title = event.args
            try {
                event.jda.presence.activity = Activity.watching(title)
                event.replySuccess("**" + event.selfUser.name + "** is now watching `" + title + "`")
            } catch (e: Exception) {
                event.reply(event.client.error + " The game could not be set!")
            }
        }
    }
}
