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
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.OwnerCommand
import com.jagrosh.jmusicbot.utils.OtherUtil
import net.dv8tion.jda.api.entities.Icon
import java.io.IOException

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SetavatarCmd(bot: Bot) : OwnerCommand() {
    init {
        name = "setavatar"
        help = "sets the avatar of the bot"
        arguments = "<url>"
        aliases = bot.config.getAliases(name)
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val url: String?
        url =
            if (event.args.isEmpty()) if (!event.message.attachments.isEmpty() && event.message.attachments[0].isImage) event.message.attachments[0].url else null else event.args
        val s = OtherUtil.imageFromUrl(url)
        if (s == null) {
            event.reply(event.client.error + " Invalid or missing URL")
        } else {
            try {
                event.selfUser.manager.setAvatar(Icon.from(s)).queue(
                    { v: Void? -> event.reply(event.client.success + " Successfully changed avatar.") }
                ) { t: Throwable? -> event.reply(event.client.error + " Failed to set avatar.") }
            } catch (e: IOException) {
                event.reply(event.client.error + " Could not load from provided URL.")
            }
        }
    }
}
