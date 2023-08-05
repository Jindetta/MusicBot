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
package com.jagrosh.jmusicbot.utils

import com.jagrosh.jmusicbot.JMusicBot
import com.jagrosh.jmusicbot.entities.Prompt
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
object OtherUtil {
    const val NEW_VERSION_AVAILABLE = ("There is a new version of JMusicBot available!\n"
            + "Current version: %s\n"
            + "New Version: %s\n\n"
            + "Please visit https://github.com/jagrosh/MusicBot/releases/latest to get the latest release.")
    private const val WINDOWS_INVALID_PATH = "c:\\windows\\system32\\"

    /**
     * gets a Path from a String
     * also fixes the windows tendency to try to start in system32
     * any time the bot tries to access this path, it will instead start in the location of the jar file
     *
     * @param path the string path
     * @return the Path object
     */
    fun getPath(path: String): Path {
        var result = Paths.get(path)
        // special logic to prevent trying to access system32
        if (result.toAbsolutePath().toString().lowercase(Locale.getDefault()).startsWith(WINDOWS_INVALID_PATH)) {
            runCatching {
                result =
                    Paths.get(
                        File(JMusicBot::class.java.protectionDomain.codeSource.location.toURI()).parentFile.path,
                        path
                    )
            }
        }

        return result
    }

    /**
     * Loads a resource from the jar as a string
     *
     * @param name name of resource
     * @return string containing the contents of the resource
     */
    fun loadResource(name: String): String? {
        runCatching {
            val inputStream = JMusicBot.javaClass.getResourceAsStream(name)

            inputStream?.bufferedReader()?.use { reader ->
                val stringBuilder = StringBuilder()

                for (line in reader.readLines()) {
                    stringBuilder.append("\r\n$line")
                }

                return stringBuilder.toString().trim()
            }
        }

        return null
    }

    /**
     * Loads image data from a URL
     *
     * @param url url of image
     * @return inputstream of url
     */
    fun imageFromUrl(url: String?): InputStream? {
        url?.let {
            runCatching {
                val connection = URL(url).openConnection()

                connection.setRequestProperty(
                    "user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36"
                )

                return connection.getInputStream()
            }
        }

        return null
    }

    /**
     * Parses an activity from a string
     *
     * @param game the game, including the action such as 'playing' or 'watching'
     * @return the parsed activity
     */
    fun parseGame(game: String?): Activity? {
        if (game == null || game.trim { it <= ' ' }.isEmpty() || game.trim { it <= ' ' }
                .equals("default", ignoreCase = true)) return null
        val lower = game.lowercase(Locale.getDefault())
        if (lower.startsWith("playing")) return Activity.playing(makeNonEmpty(game.substring(7).trim { it <= ' ' }))
        if (lower.startsWith("listening to")) return Activity.listening(
            makeNonEmpty(
                game.substring(12).trim { it <= ' ' })
        )
        if (lower.startsWith("listening")) return Activity.listening(makeNonEmpty(game.substring(9).trim { it <= ' ' }))
        if (lower.startsWith("watching")) return Activity.watching(makeNonEmpty(game.substring(8).trim { it <= ' ' }))
        if (lower.startsWith("streaming")) {
            val parts = game.substring(9).trim { it <= ' ' }.split("\\s+".toRegex(), limit = 2).toTypedArray()
            if (parts.size == 2) {
                return Activity.streaming(makeNonEmpty(parts[1]), "https://twitch.tv/" + parts[0])
            }
        }

        return Activity.playing(game)
    }

    private fun makeNonEmpty(str: String?): String {
        return if (str.isNullOrEmpty()) "\u200B" else str
    }

    fun parseStatus(status: String?): OnlineStatus {
        if (status.isNullOrBlank()) {
            return OnlineStatus.ONLINE
        }

        return OnlineStatus.fromKey(status) ?: OnlineStatus.ONLINE
    }

    fun checkJavaVersion(prompt: Prompt) {
        if (!System.getProperty("java.vm.name").contains("64")) prompt.alert(
            Prompt.Level.WARNING, "Java Version",
            "It appears that you may not be using a supported Java version. Please use 64-bit java."
        )
    }

    fun checkVersion(prompt: Prompt) {
        // Get current version number
        val version = currentVersion

        // Check for new version
        val latestVersion = latestVersion
        if (latestVersion != null && latestVersion != version) {
            prompt.alert(
                Prompt.Level.WARNING,
                "JMusicBot Version",
                String.format(NEW_VERSION_AVAILABLE, version, latestVersion)
            )
        }
    }

    val currentVersion: String
        get() = if (JMusicBot::class.java.getPackage() != null && JMusicBot::class.java.getPackage().implementationVersion != null) JMusicBot::class.java.getPackage().implementationVersion else "UNKNOWN"
    val latestVersion: String?
        get() {
            runCatching {
                val response = OkHttpClient.Builder().build()
                    .newCall(
                        Request.Builder().get().url("https://api.github.com/repos/jagrosh/MusicBot/releases/latest")
                            .build()
                    )
                    .execute()
                val body = response.body()
                if (body != null) {
                    response.use { _ ->
                        body.charStream().use { reader ->
                            val obj = JSONObject(JSONTokener(reader))
                            return obj.getString("tag_name")
                        }
                    }
                } else return null
            }

            return null
        }
}
