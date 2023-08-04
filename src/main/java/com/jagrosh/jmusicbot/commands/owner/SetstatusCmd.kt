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
import net.dv8tion.jda.api.OnlineStatus
import java.util.*

/**
 *
 * @author John Grosh <john.a.grosh></john.a.grosh>@gmail.com>
 */
class SetstatusCmd(bot: Bot) : OwnerCommand() {
    init {
        name = "setstatus"
        help = "sets the status the bot displays"
        arguments = "<status>"
        aliases = bot.config.getAliases(name)
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        try {
            val status = OnlineStatus.fromKey(event.args)
            if (status == OnlineStatus.UNKNOWN) {
                event.replyError("Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`")
            } else {
                event.jda.presence.setStatus(status)
                event.replySuccess("Set the status to `" + status.key.uppercase(Locale.getDefault()) + "`")
            }
        } catch (e: Exception) {
            event.reply(event.client.error + " The status could not be set!")
        }
    }
}
