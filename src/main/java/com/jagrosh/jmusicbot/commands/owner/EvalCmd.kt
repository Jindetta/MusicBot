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
package com.jagrosh.jmusicbot.commands.owner

import com.jagrosh.jdautilities.command.CommandEvent
import com.jagrosh.jmusicbot.Bot
import com.jagrosh.jmusicbot.commands.OwnerCommand
import javax.script.ScriptEngineManager

/**
 *
 * @author John Grosh (jagrosh)
 */
class EvalCmd(private val bot: Bot) : OwnerCommand() {
    init {
        name = "eval"
        help = "evaluates nashorn code"
        aliases = bot.config.getAliases(name)
        guildOnly = false
    }

    override fun execute(event: CommandEvent) {
        val se = ScriptEngineManager().getEngineByName("Nashorn")
        se.put("bot", bot)
        se.put("event", event)
        se.put("jda", event.jda)
        se.put("guild", event.guild)
        se.put("channel", event.channel)
        try {
            event.reply(
                """${event.client.success} Evaluated Successfully:
```
${se.eval(event.args)} ```"""
            )
        } catch (e: Exception) {
            event.reply(
                """${event.client.error} An exception was thrown:
```
$e ```"""
            )
        }
    }
}
