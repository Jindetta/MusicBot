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
package com.jagrosh.jmusicbot.settings

import com.jagrosh.jdautilities.command.GuildSettingsManager
import com.jagrosh.jmusicbot.utils.OtherUtil
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class SettingsManager : GuildSettingsManager<Settings?> {
    private val settings: HashMap<Long, Settings> = HashMap()

    init {
        try {
            val loadedSettings = JSONObject(String(Files.readAllBytes(OtherUtil.getPath("serversettings.json"))))
            loadedSettings.keySet().forEach { id ->
                val o = loadedSettings.getJSONObject(id)

                // Legacy version support: On versions 0.3.3 and older, the repeat mode was represented as a boolean.
                if (!o.has("repeat_mode") && o.has("repeat") && o.getBoolean("repeat")) o.put(
                    "repeat_mode",
                    RepeatMode.ALL
                )
                settings[id.toLong()] = Settings(
                    this,
                    if (o.has("text_channel_id")) o.getString("text_channel_id") else null,
                    if (o.has("voice_channel_id")) o.getString("voice_channel_id") else null,
                    if (o.has("dj_role_id")) o.getString("dj_role_id") else null,
                    if (o.has("volume")) o.getInt("volume") else 100,
                    if (o.has("default_playlist")) o.getString("default_playlist") else null,
                    if (o.has("repeat_mode")) o.getEnum(
                        RepeatMode::class.java,
                        "repeat_mode"
                    ) else RepeatMode.OFF,
                    if (o.has("prefix")) o.getString("prefix") else null,
                    if (o.has("skip_ratio")) o.getDouble("skip_ratio") else SKIP_RATIO
                )
            }
        } catch (e: IOException) {
            LoggerFactory.getLogger("Settings")
                .warn("Failed to load server settings (this is normal if no settings have been set yet): $e")
        } catch (e: JSONException) {
            LoggerFactory.getLogger("Settings")
                .warn("Failed to load server settings (this is normal if no settings have been set yet): $e")
        }
    }

    /**
     * Gets non-null settings for a Guild
     *
     * @param guild the guild to get settings for
     * @return the existing settings, or new settings for that guild
     */
    override fun getSettings(guild: Guild): Settings {
        return getSettings(guild.idLong)
    }

    fun getSettings(guildId: Long): Settings {
        return settings.computeIfAbsent(guildId) {
            _ -> createDefaultSettings()
        }
    }

    private fun createDefaultSettings(): Settings {
        return Settings(this, 0, 0, 0, 100, null, RepeatMode.OFF, null, SKIP_RATIO)
    }

    fun writeSettings() {
        val settingsData = getSettingsJSON().toString(4).toByteArray()

        try {
            Files.write(OtherUtil.getPath("serversettings.json"), settingsData)
        } catch (ex: IOException) {
            LoggerFactory.getLogger("Settings").warn("Failed to write to file: $ex")
        }
    }

    private fun getSettingsJSON(): JSONObject {
        val rootObject = JSONObject()

        for ((key, settings) in settings) {
            val childObject = JSONObject()

            with(childObject) {
                if (settings.textId != 0L) {
                    put("text_channel_id", settings.textId.toString())
                }

                if (settings.voiceId != 0L) {
                    put("voice_channel_id", settings.voiceId.toString())
                }

                if (settings.roleId != 0L) {
                    put("dj_role_id", settings.roleId.toString())
                }

                if (settings.volume != 100) {
                    put("volume", settings.volume)
                }

                if (settings.defaultPlaylist != null) {
                    put("default_playlist", settings.defaultPlaylist)
                }

                if (settings.repeatMode != RepeatMode.OFF) {
                    put("repeat_mode", settings.repeatMode)
                }

                if (settings.prefix != null) {
                    put("prefix", settings.prefix)
                }

                if (settings.skipRatio != SKIP_RATIO) {
                    put("skip_ratio", settings.skipRatio)
                }
            }

            rootObject.put(key.toString(), childObject)
        }

        return rootObject
    }

    companion object {
        private const val SKIP_RATIO = .55
    }
}
