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
package com.jagrosh.jmusicbot.commands.owner

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.OwnerCommand
import com.jagrosh.jmusicbot.utils.OtherUtil
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class DebugCmd(private val bot: Bot) : OwnerCommand() {
    init {
        name = "debug"
        help = "shows debug info"
        aliases = bot.config.getAliases(name)
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val sb = StringBuilder()
        sb.append("```\nSystem Properties:")
        for (key in PROPERTIES) sb.append("\n  ").append(key).append(" = ").append(System.getProperty(key))
        sb.append("\n\nJMusicBot Information:")
            .append("\n  Version = ").append(OtherUtil.currentVersion)
            .append("\n  Owner = ").append(bot.config.ownerId)
            .append("\n  Prefix = ").append(bot.config.prefix)
            .append("\n  AltPrefix = ").append(bot.config.altPrefix)
            .append("\n  MaxSeconds = ").append(bot.config.maxSeconds)
            .append("\n  NPImages = ").append(bot.config.useNPImages())
            .append("\n  SongInStatus = ").append(bot.config.songInStatus)
            .append("\n  StayInChannel = ").append(bot.config.stay)
            .append("\n  UseEval = ").append(bot.config.useEval())
            .append("\n  UpdateAlerts = ").append(bot.config.useUpdateAlerts())
        sb.append("\n\nDependency Information:")
            .append("\n  JDA Version = ").append(JDAInfo.VERSION)
            .append("\n  JDA-Utilities Version = ").append(JDAUtilitiesInfo.VERSION)
            .append("\n  Lavaplayer Version = ").append(PlayerLibrary.VERSION)
        val total = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val used = total - Runtime.getRuntime().freeMemory() / 1024 / 1024
        sb.append("\n\nRuntime Information:")
            .append("\n  Total Memory = ").append(total)
            .append("\n  Used Memory = ").append(used)
        sb.append("\n\nDiscord Information:")
            .append("\n  ID = ").append(event.jda.selfUser.id)
            .append("\n  Guilds = ").append(event.jda.guildCache.size())
            .append("\n  Users = ").append(event.jda.userCache.size())
        sb.append("\n```")
        if (event.isFromType(ChannelType.PRIVATE)
            || event.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_ATTACH_FILES)
        ) event.channel.sendFile(sb.toString().toByteArray(), "debug_information.txt")
            .queue() else event.reply("Debug Information: $sb")
    }

    companion object {
        private val PROPERTIES = arrayOf(
            "java.version", "java.vm.name", "java.vm.specification.version",
            "java.runtime.name", "java.runtime.version", "java.specification.version", "os.arch", "os.name"
        )
    }
}
