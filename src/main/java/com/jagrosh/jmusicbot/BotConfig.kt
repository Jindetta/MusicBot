/*
 * Copyright 2018 John Grosh (jagrosh)
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


import com.jagrosh.jmusicbot.entities.Prompt
import com.jagrosh.jmusicbot.utils.FormatUtil
import com.jagrosh.jmusicbot.utils.OtherUtil
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigException.Missing
import com.typesafe.config.ConfigFactory
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToLong

/**
 *
 *
 * @author John Grosh (jagrosh)
 */
class BotConfig(private val prompt: Prompt) {
    private var path: Path? = null
    var token: String? = null
        private set
    var prefix: String? = null
        private set
    private var altprefix: String? = null
    var help: String? = null
        private set
    var playlistsFolder: String? = null
        private set
    var success: String? = null
        private set
    var warning: String? = null
        private set
    var error: String? = null
        private set
    var loading: String? = null
        private set
    var searching: String? = null
        private set
    var stay = false
        private set
    var songInStatus = false
        private set
    private var npImages = false
    private var updatealerts = false
    private var useEval = false
    var dBots = false
        private set
    var ownerId: Long = 0
        private set
    var maxSeconds: Long = 0
        private set
    var aloneTimeUntilStop: Long = 0
        private set
    var status: OnlineStatus? = null
        private set
    var game: Activity? = null
        private set
    private var aliases: Config? = null
    var transforms: Config? = null
        private set
    var isValid = false
        private set

    fun load() {
        isValid = false

        // read config from file
        try {
            // get the path to the config, default config.txt
            path = configPath

            // load in the config file, plus the default values
            //Config config = ConfigFactory.parseFile(path.toFile()).withFallback(ConfigFactory.load());
            val config = ConfigFactory.load()

            // set values
            token = config.getString("token")
            prefix = config.getString("prefix")
            altprefix = config.getString("altprefix")
            help = config.getString("help")
            ownerId = config.getLong("owner")
            success = config.getString("success")
            warning = config.getString("warning")
            error = config.getString("error")
            loading = config.getString("loading")
            searching = config.getString("searching")
            game = OtherUtil.parseGame(config.getString("game"))
            status = OtherUtil.parseStatus(config.getString("status"))
            stay = config.getBoolean("stayinchannel")
            songInStatus = config.getBoolean("songinstatus")
            npImages = config.getBoolean("npimages")
            updatealerts = config.getBoolean("updatealerts")
            useEval = config.getBoolean("eval")
            maxSeconds = config.getLong("maxtime")
            aloneTimeUntilStop = config.getLong("alonetimeuntilstop")
            playlistsFolder = config.getString("playlistsfolder")
            aliases = config.getConfig("aliases")
            transforms = config.getConfig("transforms")
            dBots = ownerId == 113156185389092864L

            // we may need to write a new config file
            var write = false

            // validate bot token
            if (token == null || token!!.isEmpty() || token.equals("BOT_TOKEN_HERE", ignoreCase = true)) {
                token = prompt.prompt(
                    """
    Please provide a bot token.
    Instructions for obtaining a token can be found here:
    https://github.com/jagrosh/MusicBot/wiki/Getting-a-Bot-Token.
    Bot Token: 
    """.trimIndent()
                )
                write = if (token == null) {
                    prompt.alert(
                        Prompt.Level.WARNING, CONTEXT, """
     No token provided! Exiting.
     
     Config Location: ${path!!.toAbsolutePath()}
     """.trimIndent()
                    )
                    return
                } else {
                    true
                }
            }

            // validate bot owner
            if (ownerId <= 0) {
                ownerId = try {
                    prompt.prompt(
                        """
                Owner ID was missing, or the provided owner ID is not valid.
                Please provide the User ID of the bot's owner.
                Instructions for obtaining your User ID can be found here:
                https://github.com/jagrosh/MusicBot/wiki/Finding-Your-User-ID
                Owner User ID: 
                """.trimIndent()
                    )?.toLong() ?: 0
                } catch (ex: NumberFormatException) {
                    0
                } catch (ex: NullPointerException) {
                    0
                }
                write = if (ownerId <= 0) {
                    prompt.alert(
                        Prompt.Level.ERROR, CONTEXT, """
     Invalid User ID! Exiting.
     
     Config Location: ${path!!.toAbsolutePath()}
     """.trimIndent()
                    )
                    return
                } else {
                    true
                }
            }
            if (write) writeToFile()

            // if we get through the whole config, it's good to go
            isValid = true
        } catch (ex: ConfigException) {
            prompt.alert(
                Prompt.Level.ERROR, CONTEXT, """
     $ex: ${ex.message}
     
     Config Location: ${path!!.toAbsolutePath()}
     """.trimIndent()
            )
        }
    }

    private fun writeToFile() {
        val bytes: ByteArray = loadDefaultConfig().replace("BOT_TOKEN_HERE", token!!)
            .replace("0 // OWNER ID", ownerId.toString())
            .trim { it <= ' ' }.toByteArray()
        try {
            path?.let { Files.write(it, bytes) }
        } catch (ex: IOException) {
            prompt.alert(
                Prompt.Level.WARNING, CONTEXT, """
     Failed to write new config options to config.txt: $ex
     Please make sure that the files are not on your desktop or some other restricted area.
     
     Config Location: ${path!!.toAbsolutePath()}
     """.trimIndent()
            )
        }
    }

    val configLocation: String
        get() = path!!.toFile().absolutePath
    val altPrefix: String?
        get() = if ("NONE".equals(altprefix, ignoreCase = true)) null else altprefix

    fun useUpdateAlerts(): Boolean {
        return updatealerts
    }

    fun useEval(): Boolean {
        return useEval
    }

    fun useNPImages(): Boolean {
        return npImages
    }

    val maxTime: String
        get() = FormatUtil.formatTime(maxSeconds * 1000)

    fun isTooLong(track: AudioTrack): Boolean {
        return maxSeconds > 0 && (track.duration / 1000.0).roundToLong() > maxSeconds
    }

    fun getAliases(command: String?): Array<String?> {
        return try {
            aliases!!.getStringList(command).toTypedArray<String?>()
        } catch (e: NullPointerException) {
            arrayOfNulls(0)
        } catch (e: Missing) {
            arrayOfNulls(0)
        }
    }

    companion object {
        private const val CONTEXT = "Config"
        private const val START_TOKEN = "/// START OF JMUSICBOT CONFIG ///"
        private const val END_TOKEN = "/// END OF JMUSICBOT CONFIG ///"
        private fun loadDefaultConfig(): String {
            val original = OtherUtil.loadResource("/reference.conf")
            return original?.substring(original.indexOf(START_TOKEN) + START_TOKEN.length, original.indexOf(END_TOKEN))
                ?.trim()
                ?: "token = BOT_TOKEN_HERE\r\nowner = 0 // OWNER ID"
        }

        private val configPath: Path
            get() {
                val path =
                    OtherUtil.getPath(System.getProperty("config.file", System.getProperty("config", "config.txt")))
                if (path.toFile().exists()) {
                    if (System.getProperty("config.file") == null) System.setProperty(
                        "config.file",
                        System.getProperty("config", path.toAbsolutePath().toString())
                    )
                    ConfigFactory.invalidateCaches()
                }
                return path
            }

        fun writeDefaultConfig() {
            val prompt = Prompt("", null, isNoGUI = true, noprompt = true)
            prompt.alert(Prompt.Level.INFO, "JMusicBot Config", "Generating default config file")
            val path: Path = configPath
            val bytes: ByteArray = loadDefaultConfig().toByteArray()
            try {
                prompt.alert(
                    Prompt.Level.INFO,
                    "JMusicBot Config",
                    "Writing default config file to " + path.toAbsolutePath().toString()
                )
                Files.write(path, bytes)
            } catch (ex: Exception) {
                prompt.alert(
                    Prompt.Level.ERROR,
                    "JMusicBot Config",
                    "An error occurred writing the default config file: " + ex.message
                )
            }
        }
    }
}
