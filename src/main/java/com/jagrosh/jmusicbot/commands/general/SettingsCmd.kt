/*
 * Copyright 2017 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.general

import com.jagrosh.jdautilities.command.Command
import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.filter
import com.jagrosh.jmusicbot.settings.RepeatMode
import com.jagrosh.jmusicbot.settings.Settings
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SettingsCmd(bot: Bot) : Command() {
    init {
        name = "settings"
        help = "shows the bots settings"
        aliases = bot.config.getAliases(name)
        guildOnly = true
    }

    override fun execute(event: CommandEvent) {
        val settings = event.client.getSettingsFor<Settings>(event.guild)
        val builder = MessageBuilder()
            .append("$EMOJI **")
            .append(event.selfUser.name.filter())
            .append("** settings:")
        val textChannel = settings.getTextChannel(event.guild)
        val voiceChannel = settings.getVoiceChannel(event.guild)
        val role = settings.getRole(event.guild)
        val ebuilder = EmbedBuilder()
            .setColor(event.selfMember.color)
            .setDescription(
                """
    Text Channel: ${if (textChannel == null) "Any" else "**#" + textChannel.name + "**"}
    Voice Channel: ${voiceChannel?.asMention ?: "Any"}
    DJ Role: ${if (role == null) "None" else "**" + role.name + "**"}
    Custom Prefix: ${if (settings.prefix == null) "None" else "`" + settings.prefix + "`"}
    Repeat Mode: ${if (settings.repeatMode == RepeatMode.OFF) settings.repeatMode.userFriendlyName else "**" + settings.repeatMode.userFriendlyName + "**"}
    Default Playlist: ${if (settings.defaultPlaylist == null) "None" else "**" + settings.defaultPlaylist + "**"}
    """.trimIndent()
            )
            .setFooter(
                event.jda.guilds.size.toString() + " servers | "
                        + event.jda.guilds.stream().filter { g: Guild -> g.selfMember.voiceState!!.inVoiceChannel() }
                    .count()
                        + " audio connections", null
            )
        event.channel.sendMessage(builder.setEmbeds(ebuilder.build()).build()).queue()
    }

    companion object {
        private const val EMOJI = "\uD83C\uDFA7" // 🎧

        fun getTextChannel(channelName: String?): String {
            return if (channelName == null) "Any" else "**#${channelName}**"
        }

        fun getVoiceChannel(channelName: String?): String {
            return channelName ?: "Any"
        }

        fun getRole(role: String?): String {
            return if (role == null) "None" else "**@${role}**"
        }
    }
}
