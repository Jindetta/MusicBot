package com.jagrosh.jmusicbot

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.audio.AudioHandler
import java.lang.IllegalArgumentException
import java.lang.NumberFormatException

fun CommandEvent.audioHandler(): AudioHandler {
    val audioHandler = guild.audioManager.sendingHandler as? AudioHandler

    return audioHandler ?: throw IllegalStateException("AudioHandler is not registered")
}

fun Long.formatTime(): String {
    if (this == Long.MAX_VALUE) return "LIVE"
    var seconds = Math.round(this / 1000.0)
    val hours = seconds / (60 * 60)
    seconds %= (60 * 60).toLong()
    val minutes = seconds / 60
    seconds %= 60
    return (if (hours > 0) "$hours:" else "") + (if (minutes < 10) "0$minutes" else minutes) + ":" + if (seconds < 10) "0$seconds" else seconds
}

fun String.timeToLong(): Long {
    try {
        val parts = split(":", limit = 3)

        return when (parts.size) {
            1 -> {
                val (seconds) = parts
                seconds.toSeconds().toMillis()
            }
            2 -> {
                val (minutes, seconds) = parts
                (minutes.toMinutes() + seconds.toSeconds()).toMillis()
            }
            3 -> {
                val (hours, minutes, seconds) = parts
                (hours.toHours() + minutes.toMinutes() + seconds.toSeconds()).toMillis()
            }
            else -> throw IllegalArgumentException("Illegal time format [[hh]:mm]:ss")
        }
    } catch (_: NumberFormatException) {
        throw IllegalArgumentException("Time format cannot be converted ")
    }
}

fun String.filter(): String {
    return replace("\u202E", "")
        .replace("@everyone", "@\u0435veryone") // cyrillic letter e
        .replace("@here", "@h\u0435re") // cyrillic letter e
        .trim()
}

fun String.toHours() = toLong() * 60 * 60
fun String.toMinutes() = toLong() * 60
fun String.toSeconds() = toLong()
fun Long.toMillis() = this * 1000