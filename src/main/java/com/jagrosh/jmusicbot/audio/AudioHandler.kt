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
package com.jagrosh.jmusicbot.audio

import com.jagrosh.jmusicbot.filter
import com.jagrosh.jmusicbot.queue.FairQueue
import com.jagrosh.jmusicbot.settings.RepeatMode
import com.jagrosh.jmusicbot.utils.FormatUtil
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.audio.AudioSendHandler
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import java.nio.ByteBuffer
import java.util.*

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class AudioHandler(private val manager: PlayerManager, guild: Guild, val player: AudioPlayer) : AudioEventAdapter(),
    AudioSendHandler {
    val queue = FairQueue<QueuedTrack>()
    private val defaultQueue: MutableList<AudioTrack> = LinkedList<AudioTrack>()
    val votes: MutableSet<String?> = HashSet()
    private val guildId: Long
    private var lastFrame: AudioFrame? = null

    init {
        guildId = guild.idLong
    }

    fun addTrackToFront(track: QueuedTrack): Int {
        return if (player.playingTrack == null) {
            player.playTrack(track.track)
            -1
        } else {
            queue.addAt(0, track)
            0
        }
    }

    fun addTrack(track: QueuedTrack): Int {
        return if (player.playingTrack == null) {
            player.playTrack(track.track)
            -1
        } else queue.add(track)
    }

    fun stopAndClear() {
        queue.clear()
        defaultQueue.clear()
        player.stopTrack()
    }

    fun isMusicPlaying(jda: JDA?): Boolean {
        return guild(jda)?.selfMember?.voiceState?.inVoiceChannel() == true && player.playingTrack != null
    }

    val requestMetadata: RequestMetadata
        get() {
            if (player.playingTrack == null) return RequestMetadata.EMPTY
            val rm = player.playingTrack.getUserData(RequestMetadata::class.java)
            return rm ?: RequestMetadata.EMPTY
        }

    fun playFromDefault(): Boolean {
        if (defaultQueue.isNotEmpty()) {
            player.playTrack(defaultQueue.removeAt(0))
            return true
        }
        val settings = manager.bot.settingsManager.getSettings(guildId)
        if (settings.defaultPlaylist == null) return false
        val pl = manager.bot.playlistLoader.getPlaylist(settings.defaultPlaylist!!)
        if (pl == null || pl.items.isEmpty()) return false
        pl.loadTracks(
            manager,
            { at: AudioTrack? -> if (player.playingTrack == null) player.playTrack(at) else defaultQueue.add(at!!) }) {
            if (pl.tracks.isEmpty() && !manager.bot.config.stay) manager.bot.closeAudioConnection(
                guildId
            )
        }
        return true
    }

    // Audio Events
    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        val repeatMode = manager.bot.settingsManager.getSettings(guildId).repeatMode
        // if the track ended normally, and we're in repeat mode, re-add it to the queue
        if (endReason == AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF) {
            val clone = QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata::class.java))
            if (repeatMode == RepeatMode.ALL) queue.add(clone) else queue.addAt(0, clone)
        }
        if (queue.isEmpty) {
            if (!playFromDefault()) {
                manager.bot.nowPlayingHandler.onTrackUpdate(guildId, null, this)
                if (!manager.bot.config.stay) manager.bot.closeAudioConnection(guildId)
                // unpause, in the case when the player was paused and the track has been skipped.
                // this is to prevent the player being paused next time it's being used.
                player.isPaused = false
            }
        } else {
            val queuedTrack = queue.pull()
            player.playTrack(queuedTrack.track)
        }
    }

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        votes.clear()
        manager.bot.nowPlayingHandler.onTrackUpdate(guildId, track, this)
    }

    // Formatting
    fun getNowPlaying(jda: JDA?): Message? {
        return if (isMusicPlaying(jda)) {
            val guild = guild(jda)
            val track = player.playingTrack
            val messageBuilder = MessageBuilder()
            messageBuilder.append("${manager.bot.config.success} **Now Playing in ${guild!!.selfMember.voiceState!!.channel!!.asMention}...**".filter())
            val eb = EmbedBuilder()
            eb.setColor(guild.selfMember.color)
            val rm = requestMetadata
            if (rm.owner != 0L) {
                val u = guild.jda.getUserById(rm.user!!.id)
                if (u == null) eb.setAuthor(
                    rm.user.username + "#" + rm.user.discrim,
                    null,
                    rm.user.avatar
                ) else eb.setAuthor(u.name + "#" + u.discriminator, null, u.effectiveAvatarUrl)
            }
            try {
                eb.setTitle(track.info.title, track.info.uri)
            } catch (e: Exception) {
                eb.setTitle(track.info.title)
            }
            if (track is YoutubeAudioTrack && manager.bot.config.useNPImages()) {
                eb.setThumbnail("https://img.youtube.com/vi/" + track.identifier + "/mqdefault.jpg")
            }
            if (track.info.author != null && track.info.author.isNotEmpty()) eb.setFooter(
                "Source: " + track.info.author,
                null
            )
            val progress = player.playingTrack.position.toDouble() / track.duration
            eb.setDescription(
                statusEmoji
                        + " " + FormatUtil.progressBar(progress)
                        + " `[" + FormatUtil.formatTime(track.position) + "/" + FormatUtil.formatTime(track.duration) + "]` "
                        + FormatUtil.volumeIcon(player.volume)
            )
            messageBuilder.setEmbeds(eb.build()).build()
        } else null
    }

    fun seekTrack(position: Long): Message {
        if (player.playingTrack.isSeekable) {
            player.playingTrack.position = position
        }

        return MessageBuilder()
            .setContent("Seek to $position")
            .build()
    }

    fun getNoMusicPlaying(jda: JDA?): Message {
        val guild = guild(jda)

        return MessageBuilder()
            .setContent("${manager.bot.config.success} **Now Playing...**".filter())
            .setEmbeds(
                EmbedBuilder()
                    .setTitle("No music playing")
                    .setDescription("$STOP_EMOJI ${FormatUtil.progressBar(-1.0)} ${FormatUtil.volumeIcon(player.volume)}")
                    .setColor(guild?.selfMember?.color)
                    .build()
            ).build()
    }

    fun getTopicFormat(jda: JDA?): String {
        return if (isMusicPlaying(jda)) {
            val userid = requestMetadata.owner
            val track = player.playingTrack
            var title = track.info.title
            if (title == null || title == "Unknown Title") title = track.info.uri
            """**$title** [${if (userid == 0L) "autoplay" else "<@$userid>"}]
$statusEmoji [${FormatUtil.formatTime(track.duration)}] ${FormatUtil.volumeIcon(player.volume)}"""
        } else "No music playing " + STOP_EMOJI + " " + FormatUtil.volumeIcon(player.volume)
    }

    val statusEmoji: String
        get() = if (player.isPaused) PAUSE_EMOJI else PLAY_EMOJI

    override fun canProvide(): Boolean {
        lastFrame = player.provide()

        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer? {
        val lastFrame = lastFrame

        return if (lastFrame != null) {
            ByteBuffer.wrap(lastFrame.data)
        } else {
            null
        }
    }

    override fun isOpus() = true

    // Private methods
    private fun guild(jda: JDA?): Guild? {
        return jda?.getGuildById(guildId)
    }

    companion object {
        const val PLAY_EMOJI = "\u25B6" // ▶
        const val PAUSE_EMOJI = "\u23F8" // ⏸
        const val STOP_EMOJI = "\u23F9" // ⏹
    }
}
