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
import com.jagrosh.jmusicbot.audio.AudioHandler
import com.jagrosh.jmusicbot.commands.MusicCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class NowplayingCmd(bot: Bot) : MusicCommand(bot) {
    init {
        name = "nowplaying"
        help = "shows the song that is currently playing"
        aliases = bot.config.getAliases(name)
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun runCommand(event: CommandEvent) {
        val handler = event.guild.audioManager.sendingHandler as? AudioHandler
        if (handler != null) {
            val m = handler.getNowPlaying(event.jda)
            if (m == null) {
                event.reply(handler.getNoMusicPlaying(event.jda))
                bot.nowPlayingHandler.clearLastNPMessage(event.guild)
            } else {
                event.reply(m) { msg: Message -> bot.nowPlayingHandler.setLastNPMessage(msg) }
            }
        }
    }
}
