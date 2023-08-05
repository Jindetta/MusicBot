package com.jagrosh.jmusicbot.commands.music

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.MusicCommand
import com.jagrosh.jmusicbot.timeToLong

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SeekToCmd(bot: Bot) : MusicCommand(bot) {
    init {
        name = "seek"
        help = "seeks to specified position"
        aliases = bot.config.getAliases(name)
        bePlaying = true
    }

    override fun runCommand(event: CommandEvent) {
        val audioHandler = event.audioHandler()

        if (event.args.isNotEmpty()) {
            runCatching {
                val position = event.args.timeToLong()

                event.reply(audioHandler.seekTrack(position))
            }.onFailure {
                event.reply(it.message)
            }
        } else {
            event.reply("Missing time format argument")
        }
    }
}
