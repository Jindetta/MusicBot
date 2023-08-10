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
class PrefixCmd(bot: Bot) : AdminCommand() {
    init {
        name = "prefix"
        help = "sets a server-specific prefix"
        arguments = "<prefix|NONE>"
        aliases = bot.config.getAliases(name)
    }

    override fun execute(event: CommandEvent) {
        if (event.args.isEmpty()) {
            event.replyError("Please include a prefix or NONE")
            return
        }

        val settings = event.client.getSettingsFor<Settings>(event.guild)
        if (event.args.equals("none", ignoreCase = true)) {
            settings.prefix = null
            event.replySuccess("Prefix cleared.")
        } else {
            settings.prefix = event.args
            event.replySuccess("Custom prefix set to `${event.args}` on *${event.guild.name}*")
        }
    }
}
