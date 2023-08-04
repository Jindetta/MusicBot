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
package com.jagrosh.jmusicbot.commands.admin

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.AdminCommand
import com.jagrosh.jmusicbot.settings.Settings

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class SkipratioCmd(bot: Bot) : AdminCommand() {
    init {
        name = "setskip"
        help = "sets a server-specific skip percentage"
        arguments = "<0 - 100>"
        aliases = bot.config.getAliases(name)
    }

    override fun execute(event: CommandEvent) {
        try {
            val `val` =
                (if (event.args.endsWith("%")) event.args.substring(0, event.args.length - 1) else event.args).toInt()
            if (`val` < 0 || `val` > 100) {
                event.replyError("The provided value must be between 0 and 100!")
                return
            }
            val s = event.client.getSettingsFor<Settings>(event.guild)
            s._skipRatio = `val` / 100.0
            event.replySuccess("Skip percentage has been set to `" + `val` + "%` of listeners on *" + event.guild.name + "*")
        } catch (ex: NumberFormatException) {
            event.replyError("Please include an integer between 0 and 100 (default is 55). This number is the percentage of listening users that must vote to skip a song.")
        }
    }
}
