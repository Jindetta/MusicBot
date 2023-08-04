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
import com.jagrosh.jmusicbot.audio.AudioHandler
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class PauseCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "pause"
        help = "pauses the current song"
        aliases = bot.config.getAliases(name)
        bePlaying = true
    }

    override fun runCommand(event: CommandEvent) {
        val handler = event.audioHandler()
        if (handler != null) {
            if (handler.player.isPaused) {
                event.replyWarning("The player is already paused! Use `" + event.client.prefix + "play` to unpause!")
                return
            }
            handler.player.isPaused = true
            event.replySuccess("Paused **" + handler.player.playingTrack.info.title + "**. Type `" + event.client.prefix + "play` to unpause!")
        }
    }
}
