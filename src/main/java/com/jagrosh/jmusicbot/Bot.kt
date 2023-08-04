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
package com.jagrosh.jmusicbot

import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jmusicbot.audio.AloneInVoiceHandler
import com.jagrosh.jmusicbot.audio.AudioHandler
import com.jagrosh.jmusicbot.audio.NowPlayingHandler
import com.jagrosh.jmusicbot.audio.PlayerManager
import com.jagrosh.jmusicbot.gui.GUI
import com.jagrosh.jmusicbot.playlist.PlaylistLoader
import com.jagrosh.jmusicbot.settings.SettingsManager
import net.dv8tion.jda.api.JDA
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.system.exitProcess

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class Bot(val waiter: EventWaiter, val config: BotConfig, val settingsManager: SettingsManager) {
    val threadPool: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    val aloneInVoiceHandler: AloneInVoiceHandler = AloneInVoiceHandler(this)
    val nowPlayingHandler: NowPlayingHandler = NowPlayingHandler(this)
    val playlistLoader: PlaylistLoader = PlaylistLoader(config)
    val playerManager: PlayerManager = PlayerManager(this)

    var jda: JDA? = null

    private var shuttingDown = false
    private var gui: GUI? = null

    init {
        playerManager.init()
        nowPlayingHandler.init()
        aloneInVoiceHandler.init()
    }

    fun closeAudioConnection(guildId: Long) {
        jda?.let { jda ->
            val guild = jda.getGuildById(guildId)

            guild?.let {
                threadPool.submit {
                    guild.audioManager.closeAudioConnection()
                }
            }
        }
    }

    fun resetGame() {
        jda?.let { jda ->
            val game = if (config.game?.name.equals("none", ignoreCase = true)) null else config.game

            if (jda.presence.activity != game) {
                jda.presence.activity = game
            }
        }
    }

    fun shutdown() {
        if (!shuttingDown) {
            shuttingDown = true
            threadPool.shutdownNow()

            jda?.let { jda ->
                if (jda.status != JDA.Status.SHUTTING_DOWN) {
                    jda.guilds.forEach { guild ->
                        guild.audioManager.closeAudioConnection()
                        val audioHandler = guild.audioManager.sendingHandler as? AudioHandler

                        audioHandler?.let {
                            audioHandler.stopAndClear()
                            audioHandler.player.destroy()
                            nowPlayingHandler.updateTopic(guild.idLong, audioHandler, true)
                        }
                    }

                    jda.shutdown()
                }
            }

            gui?.dispose()
            exitProcess(0)
        }
    }

    fun setGUI(gui: GUI?) {
        this.gui = gui
    }
}
