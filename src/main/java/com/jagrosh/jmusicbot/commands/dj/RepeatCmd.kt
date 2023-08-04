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
import com.jagrosh.jmusicbot.commands.DJCommand
import com.jagrosh.jmusicbot.settings.RepeatMode
import com.jagrosh.jmusicbot.settings.Settings

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class RepeatCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "repeat"
        help = "re-adds music to the queue when finished"
        arguments = "[off|all|single]"
        aliases = bot.config.getAliases(name)
        guildOnly = true
    }

    // override musiccommand's execute because we don't actually care where this is used
    override fun execute(event: CommandEvent) {
        val args = event.args
        val value: RepeatMode
        val settings = event.client.getSettingsFor<Settings>(event.guild)
        value = if (args.isEmpty()) {
            if (settings.repeatMode == RepeatMode.OFF) RepeatMode.ALL else RepeatMode.OFF
        } else if (args.equals("false", ignoreCase = true) || args.equals("off", ignoreCase = true)) {
            RepeatMode.OFF
        } else if (args.equals("true", ignoreCase = true) || args.equals("on", ignoreCase = true) || args.equals(
                "all",
                ignoreCase = true
            )
        ) {
            RepeatMode.ALL
        } else if (args.equals("one", ignoreCase = true) || args.equals("single", ignoreCase = true)) {
            RepeatMode.SINGLE
        } else {
            event.replyError("Valid options are `off`, `all` or `single` (or leave empty to toggle between `off` and `all`)")
            return
        }
        settings.repeatMode = value
        event.replySuccess("Repeat mode is now `" + value.userFriendlyName + "`")
    }

    override fun runCommand(event: CommandEvent) { /* Intentionally Empty */
    }
}
