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
package com.jagrosh.jmusicbot.audio

import com.jagrosh.jmusicbot.Bot
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import net.dv8tion.jda.api.entities.Guild

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class PlayerManager(val bot: Bot) : DefaultAudioPlayerManager() {

    fun init() {
        TransformativeAudioSourceManager.createTransforms(bot.config.transforms!!)
            .forEach { t: TransformativeAudioSourceManager -> registerSourceManager(t) }
        AudioSourceManagers.registerRemoteSources(this)
        AudioSourceManagers.registerLocalSource(this)
        source(YoutubeAudioSourceManager::class.java).setPlaylistPageCount(10)
    }

    fun hasHandler(guild: Guild): Boolean {
        return guild.audioManager.sendingHandler != null
    }

    fun setUpHandler(guild: Guild): AudioHandler? {
        val handler: AudioHandler?
        if (guild.audioManager.sendingHandler == null) {
            val player = createPlayer()
            player.volume = bot.settingsManager.getSettings(guild).volume
            handler = AudioHandler(this, guild, player)
            player.addListener(handler)
            guild.audioManager.sendingHandler = handler
        } else handler = guild.audioManager.sendingHandler as AudioHandler?
        return handler
    }
}
