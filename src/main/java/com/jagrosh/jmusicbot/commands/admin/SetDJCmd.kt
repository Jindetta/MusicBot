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
import com.jagrosh.jdautilities.commons.utils.FinderUtil
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.AdminCommand
import com.jagrosh.jmusicbot.settings.Settings
import com.jagrosh.jmusicbot.utils.FormatUtil

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SetDJCmd(bot: Bot) : AdminCommand() {
    init {
        name = "setdj"
        help = "sets the DJ role for certain music commands"
        arguments = "<rolename|NONE>"
        aliases = bot.config.getAliases(name)
    }

    override fun execute(event: CommandEvent) {
        if (event.args.isEmpty()) {
            event.reply("${event.client.error} Please include a role name or NONE")
            return
        }

        val settings = event.client.getSettingsFor<Settings>(event.guild)
        if (event.args.equals("none", ignoreCase = true)) {
            settings.setDJRole(null)
            event.reply("${event.client.success} DJ role cleared; Only Admins can use the DJ commands.")
        } else {
            val list = FinderUtil.findRoles(event.args, event.guild)

            if (list.isEmpty()) {
                event.reply("${event.client.warning} No Roles found matching \"${event.args}\"")
            } else if (list.size > 1) {
                event.reply(
                    event.client.warning + FormatUtil.listOfRoles(list, event.args)
                )
            } else {
                val firstItem = list.first()

                settings.setDJRole(firstItem)
                event.reply("${event.client.success} DJ commands can now be used by users with the **${firstItem.name}** role.")
            }
        }
    }
}
