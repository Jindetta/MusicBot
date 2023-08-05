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
import com.jagrosh.jmusicbot.settings.Settings

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
open class AutoPlaylistCmd(private val bot: Bot) : OwnerCommand() {
    init {
        guildOnly = true
        name = "autoplaylist"
        arguments = "<name|NONE>"
        help = "sets the default playlist for the server"
        aliases = bot.config.getAliases(name)
    }

    public override fun execute(event: CommandEvent) {
        if (event.args.isEmpty()) {
            event.reply(event.client.error + " Please include a playlist name or NONE")
            return
        }
        if (event.args.equals("none", ignoreCase = true)) {
            val settings = event.client.getSettingsFor<Settings>(event.guild)
            settings.defaultPlaylist = null
            event.reply(event.client.success + " Cleared the default playlist for **" + event.guild.name + "**")
            return
        }
        val pname = event.args.replace("\\s+".toRegex(), "_")
        if (bot.playlistLoader.getPlaylist(pname) == null) {
            event.reply(event.client.error + " Could not find `" + pname + ".txt`!")
        } else {
            val settings = event.client.getSettingsFor<Settings>(event.guild)
            settings.defaultPlaylist = pname
            event.reply(event.client.success + " The default playlist for **" + event.guild.name + "** is now `" + pname + "`")
        }
    }
}
