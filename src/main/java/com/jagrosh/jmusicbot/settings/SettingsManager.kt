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
import java.util.function.Consumer

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class SettingsManager : GuildSettingsManager<Settings?> {
    private val settings: HashMap<Long, Settings> = HashMap()

    init {
        try {
            val loadedSettings = JSONObject(String(Files.readAllBytes(OtherUtil.getPath("serversettings.json"))))
            loadedSettings.keySet().forEach(Consumer<String> { id: String ->
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
                    if (o.has("repeat_mode")) o.getEnum<RepeatMode>(
                        RepeatMode::class.java,
                        "repeat_mode"
                    ) else RepeatMode.OFF,
                    if (o.has("prefix")) o.getString("prefix") else null,
                    if (o.has("skip_ratio")) o.getDouble("skip_ratio") else SKIP_RATIO
                )
            })
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
        return settings.computeIfAbsent(guildId) { id: Long? -> createDefaultSettings() }
    }

    private fun createDefaultSettings(): Settings {
        return Settings(this, 0, 0, 0, 100, null, RepeatMode.OFF, null, SKIP_RATIO)
    }

    fun writeSettings() {
        val obj = JSONObject()
        settings.keys.stream().forEach { key: Long ->
            val o = JSONObject()
            val s = settings[key]
            if (s!!.textId != 0L) o.put("text_channel_id", java.lang.Long.toString(s.textId))
            if (s.voiceId != 0L) o.put("voice_channel_id", java.lang.Long.toString(s.voiceId))
            if (s.roleId != 0L) o.put("dj_role_id", java.lang.Long.toString(s.roleId))
            if (s.volume != 100) o.put("volume", s.volume)
            if (s.defaultPlaylist != null) o.put("default_playlist", s.defaultPlaylist)
            if (s.repeatMode != RepeatMode.OFF) o.put("repeat_mode", s.repeatMode)
            if (s.prefix != null) o.put("prefix", s.prefix)
            if (s._skipRatio != SKIP_RATIO) o.put("skip_ratio", s._skipRatio)
            obj.put(java.lang.Long.toString(key), o)
        }
        try {
            Files.write(OtherUtil.getPath("serversettings.json"), obj.toString(4).toByteArray())
        } catch (ex: IOException) {
            LoggerFactory.getLogger("Settings").warn("Failed to write to file: $ex")
        }
    }

    companion object {
        private const val SKIP_RATIO = .55
    }
}
