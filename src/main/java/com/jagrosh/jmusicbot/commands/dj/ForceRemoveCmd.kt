/*
 * Copyright 2019 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.commands.dj

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jdautilities.commons.utils.FinderUtil
import com.jagrosh.jdautilities.menu.OrderedMenu
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.audioHandler
import com.jagrosh.jmusicbot.commands.DJCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.TimeUnit

/**
 *
 * @author Michaili K.
 */
class ForceRemoveCmd(bot: Bot) : DJCommand(bot) {
    init {
        name = "forceremove"
        help = "removes all entries by a user from the queue"
        arguments = "<user>"
        aliases = bot.config.getAliases(name)
        beListening = false
        bePlaying = true
        botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
    }

    override fun runCommand(event: CommandEvent) {
        if (event.args.isEmpty()) {
            event.replyError("You need to mention a user!")
            return
        }
        val handler = event.audioHandler()
        if (handler.queue.isEmpty) {
            event.replyError("There is nothing in the queue!")
            return
        }
        val target: User
        val found = FinderUtil.findMembers(event.args, event.guild)
        target = if (found.isEmpty()) {
            event.replyError("Unable to find the user!")
            return
        } else if (found.size > 1) {
            val builder = OrderedMenu.Builder()
            var i = 0
            while (i < found.size && i < 4) {
                val member = found[i]
                builder.addChoice("**" + member.user.name + "**#" + member.user.discriminator)
                i++
            }
            builder
                .setSelection { msg: Message?, i: Int -> removeAllEntries(found[i - 1].user, event) }
                .setText("Found multiple users:")
                .setColor(event.selfMember.color)
                .useNumbers()
                .setUsers(event.author)
                .useCancelButton(true)
                .setCancel { msg: Message? -> }
                .setEventWaiter(bot.waiter)
                .setTimeout(1, TimeUnit.MINUTES)
                .build().display(event.channel)
            return
        } else {
            found[0].user
        }
        removeAllEntries(target, event)
    }

    private fun removeAllEntries(target: User, event: CommandEvent) {
        val handler = event.audioHandler()
        val removedEntries = handler.queue.removeAll(target.idLong)

        if (removedEntries == 0) {
            event.replyWarning("**${target.name}** doesn't have any songs in the queue!")
        } else {
            event.replySuccess("Successfully removed `$removedEntries` entries from **${target.name}**#${target.discriminator}.")
        }
    }
}
