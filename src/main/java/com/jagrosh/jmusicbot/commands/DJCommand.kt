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
package com.jagrosh.jmusicbot.commands

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.settings.Settings
import net.dv8tion.jda.api.Permission

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
abstract class DJCommand(bot: Bot) : MusicCommand(bot) {
    init {
        category = Category("DJ") { event ->
            checkDJPermission(event)
        }
    }

    companion object {
        fun checkDJPermission(event: CommandEvent): Boolean {
            if (event.guild == null ||
                event.author.id == event.client.ownerId ||
                event.member.hasPermission(Permission.MANAGE_SERVER)) {
                return true
            }

            val role = event.client.getSettingsFor<Settings>(event.guild).getRole(event.guild)
            return event.member.roles.contains(role) || role?.idLong == event.guild.idLong
        }
    }
}
