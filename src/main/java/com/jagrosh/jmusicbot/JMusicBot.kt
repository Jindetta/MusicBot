/*
 * Copyright 2016 John Grosh (jagrosh).
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

import com.jagrosh.jdautilities.command.CommandClientBuilder
import com.jagrosh.jdautilities.commons.waiter.EventWaiter
import com.jagrosh.jdautilities.examples.command.AboutCommand
import com.jagrosh.jdautilities.examples.command.PingCommand
import com.jagrosh.jmusicbot.commands.admin.*
import com.jagrosh.jmusicbot.commands.dj.*
import com.jagrosh.jmusicbot.commands.general.SettingsCmd
import com.jagrosh.jmusicbot.commands.music.*
import com.jagrosh.jmusicbot.commands.owner.*
import com.jagrosh.jmusicbot.entities.Prompt
import com.jagrosh.jmusicbot.gui.GUI
import com.jagrosh.jmusicbot.settings.SettingsManager
import com.jagrosh.jmusicbot.utils.OtherUtil
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.*
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

/**
 *
 * @author John Grosh (jagrosh)
 */
object JMusicBot {
    val RECOMMENDED_PERMS = arrayOf(
        Permission.MESSAGE_READ,
        Permission.MESSAGE_WRITE,
        Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_ADD_REACTION,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_ATTACH_FILES,
        Permission.MESSAGE_MANAGE,
        Permission.MESSAGE_EXT_EMOJI,
        Permission.MANAGE_CHANNEL,
        Permission.VOICE_CONNECT,
        Permission.VOICE_SPEAK,
        Permission.NICKNAME_CHANGE
    )

    private val INTENTS = arrayOf(
        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_VOICE_STATES
    )

    private val LOG = LoggerFactory.getLogger(JMusicBot::class.java)

    /**
     * @param args the command line arguments
     */
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isNotEmpty()) {
            if (args[0].lowercase(Locale.getDefault()) == "generate-config") {
                BotConfig.writeDefaultConfig()
                return
            }
        }

        startBot()
    }

    private fun startBot() {
        // create prompt to handle startup
        val prompt = Prompt("JMusicBot")

        // startup checks
        //OtherUtil.checkVersion(prompt)
        OtherUtil.checkJavaVersion(prompt)

        // load config
        val config = BotConfig(prompt)
        config.load()
        if (!config.isValid) return
        LOG.info("Loaded config from ${config.configLocation}")

        // set up the listener
        val waiter = EventWaiter()
        val settings = SettingsManager()
        val bot = Bot(waiter, config, settings)
        val aboutCommand = AboutCommand(
            Color.BLUE.brighter(),
            "a music bot that is [easy to host yourself!](https://github.com/jagrosh/MusicBot) (v${OtherUtil.currentVersion})",
            arrayOf("High-quality music playback", "FairQueue™ Technology", "Easy to host yourself"),
            *RECOMMENDED_PERMS
        )
        aboutCommand.setIsAuthor(false)
        aboutCommand.setReplacementCharacter("\uD83C\uDFB6") // 🎶

        // set up the command client
        val commandBuilder = CommandClientBuilder()
            .setPrefix(config.prefix)
            .setAlternativePrefix(config.altPrefix)
            .setOwnerId(config.ownerId.toString())
            .setEmojis(config.success, config.warning, config.error)
            .setHelpWord(config.help)
            .setLinkedCacheSize(200)
            .setGuildSettingsManager(settings)
            .addCommands(
                aboutCommand,
                PingCommand(),
                SettingsCmd(bot),
                LyricsCmd(bot),
                NowPlayingCmd(bot),
                PlayCmd(bot),
                PlaylistsCmd(bot),
                QueueCmd(bot),
                RemoveCmd(bot),
                SearchCmd(bot),
                SCSearchCmd(bot),
                ShuffleCmd(bot),
                SeekToCmd(bot),
                SkipCmd(bot),
                ForceRemoveCmd(bot),
                ForceSkipCmd(bot),
                MoveTrackCmd(bot),
                PauseCmd(bot),
                PlayNextCmd(bot),
                RepeatCmd(bot),
                SkipToCmd(bot),
                StopCmd(bot),
                VolumeCmd(bot),
                PrefixCmd(bot),
                SetDJCmd(bot),
                SkipRatioCmd(bot),
                SetTextChannelCmd(bot),
                SetVoiceChannelCmd(bot),
                AutoPlaylistCmd(bot),
                DebugCmd(bot),
                PlaylistCmd(bot),
                SetAvatarCmd(bot),
                SetGameCmd(bot),
                SetNameCmd(bot),
                SetStatusCmd(bot),
                ShutdownCmd(bot)
            )

        if (config.useEval()) commandBuilder.addCommand(EvalCmd(bot))
        var nogame = false
        if (config.status != OnlineStatus.UNKNOWN) commandBuilder.setStatus(config.status)
        if (config.game == null) commandBuilder.useDefaultGame() else if (config.game?.name.equals(
                "none",
                ignoreCase = true
            )
        ) {
            commandBuilder.setActivity(null)
            nogame = true
        } else commandBuilder.setActivity(config.game)
        if (!prompt.isNoGUI) {
            try {
                val gui = GUI(bot)
                bot.setGUI(gui)
                gui.init()
            } catch (e: Exception) {
                LOG.error(
                    "Could not start GUI. If you are "
                            + "running on a server or in a location where you cannot display a "
                            + "window, please run in nogui mode using the -Dnogui=true flag."
                )
            }
        }

        // attempt to log in and start
        try {
            val jda = JDABuilder.create(config.token, listOf(*INTENTS))
                .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE, CacheFlag.ONLINE_STATUS)
                .setActivity(if (nogame) null else Activity.playing("loading..."))
                .setStatus(if (config.status == OnlineStatus.INVISIBLE || config.status == OnlineStatus.OFFLINE) OnlineStatus.INVISIBLE else OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners(commandBuilder.build(), waiter, Listener(bot))
                .setBulkDeleteSplittingEnabled(true)
                .build()

            bot.jda = jda
        } catch (ex: LoginException) {
            prompt.alert(Prompt.Level.ERROR, "JMusicBot", """
     $ex
     Please make sure you are editing the correct config.txt file, and that you have used the correct token (not the 'secret'!)
     Config Location: ${config.configLocation}
     """.trimIndent()
            )
            exitProcess(1)
        } catch (ex: IllegalArgumentException) {
            prompt.alert(
                Prompt.Level.ERROR, "JMusicBot", """
     Some aspect of the configuration is invalid: $ex
     Config Location: ${config.configLocation}
     """.trimIndent()
            )
            exitProcess(1)
        } catch (ex: ErrorResponseException) {
            prompt.alert(
                Prompt.Level.ERROR, "JMusicBot", """
     $ex
     Invalid reponse returned when attempting to connect, please make sure you're connected to the internet
     """.trimIndent()
            )
            exitProcess(1)
        }
    }

    private fun getStatus(status: OnlineStatus): OnlineStatus {
        if (status == OnlineStatus.INVISIBLE || status == OnlineStatus.OFFLINE) {
            return OnlineStatus.INVISIBLE
        }

        return OnlineStatus.DO_NOT_DISTURB
    }
}
