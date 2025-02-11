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
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class StopCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "stop"
        help = "stops the current song and clears the queue"
        aliases = bot.config.getAliases(name)
        bePlaying = false
    }

    override fun runCommand(event: CommandEvent) {
        val handler = event.audioHandler()

        handler.stopAndClear()
        event.guild.audioManager.closeAudioConnection()
        event.reply("${event.client.success} The player has stopped and the queue has been cleared.")
    }
}
