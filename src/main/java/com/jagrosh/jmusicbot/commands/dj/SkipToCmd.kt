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

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SkipToCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "skipto"
        help = "skips to the specified song"
        arguments = "<position>"
        aliases = bot.config.getAliases(name)
        bePlaying = true
    }

    override fun runCommand(event: CommandEvent) {
        val index = event.args.toIntOrNull()

        if (index == null) {
            event.reply("${event.client.error} `${event.args}` is not a valid integer!")
        } else {
            val handler = event.audioHandler()

            if (index < 1 || index > handler.queue.size()) {
                event.reply(
                    "${event.client.error} Position must be a valid integer between 1 and ${handler.queue.size()}!"
                )
                return
            }

            handler.queue.skip(index - 1)
            event.reply("${event.client.success} Skipped to **${handler.queue[0].track.info.title}**")
            handler.player.stopTrack()
        }
    }
}
