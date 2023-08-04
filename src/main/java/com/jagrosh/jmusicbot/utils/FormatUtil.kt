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
package com.jagrosh.jmusicbot.utils

import com.jagrosh.jmusicbot.filter
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import kotlin.math.roundToLong

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
object FormatUtil {
    fun formatTime(duration: Long): String {
        if (duration == Long.MAX_VALUE) return "LIVE"
        var seconds = (duration / 1000.0).roundToLong()
        val hours = seconds / (60 * 60)
        seconds %= (60 * 60).toLong()
        val minutes = seconds / 60
        seconds %= 60
        return (if (hours > 0) "$hours:" else "") + (if (minutes < 10) "0$minutes" else minutes) + ":" + if (seconds < 10) "0$seconds" else seconds
    }

    fun progressBar(percent: Double): String {
        var str = ""
        for (i in 0..11) str += if (i == (percent * 12).toInt()) "\uD83D\uDD18" // 🔘
        else "▬"
        return str
    }

    fun volumeIcon(volume: Int): String {
        return when {
            volume == 0 -> "\uD83D\uDD07" // 🔇
            volume < 30 -> "\uD83D\uDD08" // 🔈
            volume < 70 -> "\uD83D\uDD09" // 🔉
            else -> "\uD83D\uDD0A" // 🔊
        }
    }

    fun listTextChannels(list: List<TextChannel>, query: String, limit: Int = 6): String {
        val stringBuilder = StringBuilder( " Multiple text channels found matching \"$query\":\n")

        for (channel in list.take(limit)) {
            stringBuilder.append(" - ${channel.name} (<#${channel.id}>)\n")
        }

        if (list.size > limit) {
            stringBuilder.append("**And ${list.size - limit} more...**\n")
        }

        return stringBuilder.toString()
    }

    fun listVoiceChannels(list: List<VoiceChannel>, query: String, limit: Int = 6): String {
        val stringBuilder = StringBuilder(" Multiple voice channels found matching \"$query\":\n")

        for (channel in list.take(limit)) {
            stringBuilder.append(" - ${channel.name} (<ID:${channel.id}>)\n")
        }

        if (list.size > limit) {
            stringBuilder.append("**And ${list.size - limit} more...**\n")
        }

        return stringBuilder.toString()
    }

    fun listOfRoles(list: List<Role>, query: String, limit: Int = 6): String {
        val stringBuilder = StringBuilder(" Multiple text channels found matching \"$query\":")

        for (role in list.take(limit)) {
            stringBuilder.append(" - ${role.name} (<ID:${role.id}>)\n")
        }

        if (list.size > limit) {
            stringBuilder.append("**And ${list.size - limit} more...**\n")
        }

        return stringBuilder.toString()
    }

    fun filter(str: String): String {
        return str.filter()
    }
}
