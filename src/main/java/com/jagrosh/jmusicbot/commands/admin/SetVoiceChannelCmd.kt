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
class SetVoiceChannelCmd(bot: Bot) : AdminCommand() {
    init {
        name = "setvc"
        help = "sets the voice channel for playing music"
        arguments = "<channel|NONE>"
        aliases = bot.config.getAliases(name)
    }

    override fun execute(event: CommandEvent) {
        if (event.args.isEmpty()) {
            event.reply(event.client.error + " Please include a voice channel or NONE")
            return
        }
        val settings = event.client.getSettingsFor<Settings>(event.guild)
        if (event.args.equals("none", ignoreCase = true)) {
            settings.setVoiceChannel(null)
            event.reply(event.client.success + " Music can now be played in any channel")
        } else {
            val list = FinderUtil.findVoiceChannels(event.args, event.guild)
            if (list.isEmpty()) event.reply(event.client.warning + " No Voice Channels found matching \"" + event.args + "\"") else if (list.size > 1) event.reply(
                event.client.warning + FormatUtil.listVoiceChannels(list, event.args)
            ) else {
                settings.setVoiceChannel(list[0])
                event.reply(event.client.success + " Music can now only be played in " + list[0].asMention)
            }
        }
    }
}
