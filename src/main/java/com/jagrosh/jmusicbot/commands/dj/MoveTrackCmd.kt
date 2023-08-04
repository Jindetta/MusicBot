package com.jagrosh.jmusicbot.commands.dj

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audio.AudioHandler
import com.jagrosh.jmusicbot.audio.QueuedTrack
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand
import com.jagrosh.jmusicbot.queue.FairQueue

/**
 * Command that provides users the ability to move a track in the playlist.
 */
class MoveTrackCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "movetrack"
        help = "move a track in the current queue to a different position"
        arguments = "<from> <to>"
        aliases = bot.config.getAliases(name)
        bePlaying = true
    }

    override fun runCommand(event: CommandEvent) {
        val from: Int
        val to: Int
        val parts = event.args.split("\\s+".toRegex(), limit = 2).toTypedArray()
        if (parts.size < 2) {
            event.replyError("Please include two valid indexes.")
            return
        }
        try {
            // Validate the args
            from = parts[0].toInt()
            to = parts[1].toInt()
        } catch (e: NumberFormatException) {
            event.replyError("Please provide two valid indexes.")
            return
        }
        if (from == to) {
            event.replyError("Can't move a track to the same position.")
            return
        }

        // Validate that from and to are available
        val handler = event.audioHandler()
        if (handler != null) {
            val queue = handler.queue
            if (isUnavailablePosition(queue, from)) {
                val reply = String.format("`%d` is not a valid position in the queue!", from)
                event.replyError(reply)
                return
            }
            if (isUnavailablePosition(queue, to)) {
                val reply = String.format("`%d` is not a valid position in the queue!", to)
                event.replyError(reply)
                return
            }

            // Move the track
            val track = queue.moveItem(from - 1, to - 1)
            val trackTitle = track.track.info.title
            val reply = String.format("Moved **%s** from position `%d` to `%d`.", trackTitle, from, to)
            event.replySuccess(reply)
        }
    }

    companion object {
        private fun isUnavailablePosition(queue: FairQueue<QueuedTrack>?, position: Int): Boolean {
            return position < 1 || position > queue!!.size()
        }
    }
}