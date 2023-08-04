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
package com.jagrosh.jmusicbot.settings

import com.jagrosh.jdautilities.command.GuildSettingsProvider
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class Settings(
    private val manager: SettingsManager,
    var textId: Long,
    var voiceId: Long,
    var roleId: Long,
    private var _volume: Int,
    private var _defaultPlaylist: String?,
    private var _repeatMode: RepeatMode,
    private var _prefix: String?,
    var _skipRatio: Double
) : GuildSettingsProvider {

    constructor(
        manager: SettingsManager,
        textId: String?,
        voiceId: String?,
        roleId: String?,
        volume: Int,
        defaultPlaylist: String?,
        repeatMode: RepeatMode,
        prefix: String?,
        skipRatio: Double
    ) : this(
        manager,
        textId?.toLongOrNull() ?: 0,
        voiceId?.toLongOrNull() ?: 0,
        roleId?.toLongOrNull() ?: 0,
        volume,
        defaultPlaylist,
        repeatMode,
        prefix,
        skipRatio
    )

    var volume = _volume
        set(value) {
            field = value
            manager.writeSettings()
        }

    var defaultPlaylist = _defaultPlaylist
        set(value) {
            field = value
            manager.writeSettings()
        }

    var repeatMode = _repeatMode
        set(value) {
            field = value
            manager.writeSettings()
        }

    var prefix = _prefix
        set(value) {
            field = value
            manager.writeSettings()
        }

    var skipRatio = _skipRatio
        set(value) {
            field = value
            manager.writeSettings()
        }

    fun getTextChannel(guild: Guild?) = guild?.getTextChannelById(textId)

    fun getVoiceChannel(guild: Guild?) = guild?.getVoiceChannelById(voiceId)

    fun getRole(guild: Guild?) = guild?.getRoleById(roleId)

    fun setTextChannel(textChannel: TextChannel?) {
        textId = textChannel?.idLong ?: 0
        manager.writeSettings()
    }

    fun setVoiceChannel(voiceChannel: VoiceChannel?) {
        voiceId = voiceChannel?.idLong ?: 0
        manager.writeSettings()
    }

    fun setDJRole(role: Role?) {
        roleId = role?.idLong ?: 0
        manager.writeSettings()
    }

    override fun getPrefixes() = setOfNotNull(prefix)
}
