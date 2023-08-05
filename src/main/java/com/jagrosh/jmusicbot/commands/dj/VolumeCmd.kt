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
package com.jagrosh.jmusicbot.commands.dj

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand
import com.jagrosh.jmusicbot.settings.Settings
import com.jagrosh.jmusicbot.utils.FormatUtil

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class VolumeCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "volume"
        aliases = bot.config.getAliases(name)
        help = "sets or shows volume"
        arguments = "[0-150]"
    }

    override fun runCommand(event: CommandEvent) {
        val handler = event.audioHandler()
        val settings = event.client.getSettingsFor<Settings>(event.guild)
        val volume = handler.player.volume
        if (event.args.isEmpty()) {
            event.reply(FormatUtil.volumeIcon(volume) + " Current volume is `" + volume + "`")
        } else {
            val nvolume: Int = try {
                event.args.toInt()
            } catch (e: NumberFormatException) {
                -1
            }
            if (nvolume < 0 || nvolume > 150) event.reply(event.client.error + " Volume must be a valid integer between 0 and 150!") else {
                handler.player.volume = nvolume
                settings.volume = nvolume
                event.reply(FormatUtil.volumeIcon(nvolume) + " Volume changed from `" + volume + "` to `" + nvolume + "`")
            }
        }
    }
}
