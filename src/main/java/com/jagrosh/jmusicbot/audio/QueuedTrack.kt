/*
 * Copyright 2021 John Grosh <john.a.grosh@gmail.com>.
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

import com.jagrosh.jmusicbot.queue.Queueable
import com.jagrosh.jmusicbot.utils.FormatUtil
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.User
import java.text.Normalizer.Form

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class QueuedTrack(val track: AudioTrack, requestMetaData: RequestMetadata?) : Queueable {

    constructor(track: AudioTrack, owner: User?) : this(track, RequestMetadata(owner))

    init {
        track.userData = requestMetaData
    }

    override val identifier: Long
        get() = track.getUserData(RequestMetadata::class.java).owner

    override fun toString(): String {
        val stringBuilder = StringBuilder("`[${FormatUtil.formatTime(track.duration)}] ")
        val trackInfo = track.info

        if (trackInfo.uri.startsWith("http")) {
            stringBuilder.append("[**${trackInfo.title}**](${trackInfo.uri})")
        } else {
            stringBuilder.append("**${trackInfo.title}**")
        }

        return stringBuilder.append(" - <@${track.getUserData(RequestMetadata::class.java).owner}>").toString()
    }
}
