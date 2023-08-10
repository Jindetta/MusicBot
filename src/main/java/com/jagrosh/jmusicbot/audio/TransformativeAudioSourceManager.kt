/*
 * Copyright 2021 John Grosh <john.a.grosh@gmail.com>.
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
package com.jagrosh.jmusicbot.audio

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.typesafe.config.Config
import com.typesafe.config.ConfigValue
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.regex.PatternSyntaxException
import java.util.stream.Collectors

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class TransformativeAudioSourceManager(
    private val name: String,
    private val regex: String,
    private val replacement: String,
    private val selector: String,
    private val format: String
) : YoutubeAudioSourceManager() {
    constructor(name: String, `object`: Config) : this(
        name,
        `object`.getString("regex"),
        `object`.getString("replacement"),
        `object`.getString("selector"),
        `object`.getString("format")
    )

    override fun getSourceName(): String {
        return name
    }

    override fun loadItem(apm: AudioPlayerManager, ar: AudioReference): AudioItem? {
        if (ar.identifier == null || !ar.identifier.matches(regex.toRegex())) return null
        try {
            val url = ar.identifier.replace(regex.toRegex(), replacement)
            val doc = Jsoup.connect(url).get()
            val value = doc.selectFirst(selector)!!.ownText()
            val formattedValue = String.format(format, value)
            return super.loadItem(apm, AudioReference(formattedValue, null))
        } catch (ex: PatternSyntaxException) {
            log.info(String.format("Invalid pattern syntax '%s' in source '%s'", regex, name))
        } catch (ex: IOException) {
            log.warn(String.format("Failed to resolve URL in source '%s': ", name), ex)
        } catch (ex: Exception) {
            log.warn(String.format("Exception in source '%s'", name), ex)
        }
        return null
    }

    companion object {
        private val log = LoggerFactory.getLogger(TransformativeAudioSourceManager::class.java)

        fun createTransforms(transforms: Config): List<TransformativeAudioSourceManager> {
            return try {
                transforms.root().entries
                    .map { (key) ->
                        TransformativeAudioSourceManager(
                            key,
                            transforms.getConfig(key)
                        )
                    }
            } catch (ex: Exception) {
                log.warn("Invalid transform ", ex)
                emptyList()
            }
        }
    }
}
