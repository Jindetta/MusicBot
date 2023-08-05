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
package com.jagrosh.jmusicbot.commands.music

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.MusicCommand

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class ShuffleCmd(bot: Bot) : MusicCommand(bot) {
    init {
        name = "shuffle"
        help = "shuffles songs you have added"
        aliases = bot.config.getAliases(name)
        beListening = true
        bePlaying = true
    }

    override fun runCommand(event: CommandEvent) {
        val handler = event.audioHandler()

        when (val shuffledTracks = handler.queue.shuffle(event.author.idLong)) {
            0 -> event.replyError("You don't have any music in the queue to shuffle!")
            1 -> event.replyWarning("You only have one song in the queue!")
            else -> event.replySuccess("You successfully shuffled your $shuffledTracks entries.")
        }
    }
}
