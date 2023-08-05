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
package com.jagrosh.jmusicbot.commands

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.settings.Settings
import net.dv8tion.jda.api.exceptions.PermissionException

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
abstract class MusicCommand(protected val bot: Bot) : Command() {
    protected var bePlaying = false
    protected var beListening = false

    init {
        category = Category("Music")
        guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val settings = event.client.getSettingsFor<Settings>(event.guild)
        val textChannel = settings.getTextChannel(event.guild)

        if (textChannel != null && event.textChannel != textChannel) {
            runCatching {
                event.message.delete().queue()
            }

            event.replyInDm("${event.client.error} You can only use that command in ${textChannel.asMention}!")
            return
        }

        bot.playerManager.setUpHandler(event.guild) // no point constantly checking for this later

        if (bePlaying && !event.audioHandler().isMusicPlaying(event.jda)) {
            event.reply("${event.client.error} There must be music playing to use that!")
            return
        }

        if (beListening) {
            val currentChannel = event.guild.selfMember.voiceState?.channel ?: settings.getVoiceChannel(event.guild)

            val userState = event.member.voiceState
            if (userState?.inVoiceChannel() == false || userState?.isDeafened == true || currentChannel != null && userState?.channel != currentChannel) {
                event.replyError(
                    "You must be listening in " + (currentChannel?.asMention
                        ?: "a voice channel") + " to use that!"
                )
                return
            }

            val afkChannel = userState?.guild?.afkChannel
            if (afkChannel != null && afkChannel == userState.channel) {
                event.replyError("You cannot use that command in an AFK channel!")
                return
            }

            if (event.guild.selfMember.voiceState?.inVoiceChannel() != true) {
                try {
                    event.guild.audioManager.openAudioConnection(userState?.channel)
                } catch (ex: PermissionException) {
                    event.reply(event.client.error + " I am unable to connect to " + userState?.channel?.asMention + "!")
                    return
                }
            }
        }

        runCommand(event)
    }

    abstract fun runCommand(event: CommandEvent)
}
