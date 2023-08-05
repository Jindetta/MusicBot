/*
 * Copyright 2018 John Grosh (jagrosh).
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
package com.jagrosh.jmusicbot.playlist

import com.jagrosh.jmusicbot.BotConfig
import com.jagrosh.jmusicbot.utils.OtherUtil
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.*
import java.io.*
import java.nio.file.Files
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class PlaylistLoader(private val config: BotConfig) {
    val playlistNames: List<String>
        get() = if (folderExists()) {
            val folder = File(OtherUtil.getPath(config.playlistsFolder!!).toString())
            folder.listFiles { pathname: File -> pathname.name.endsWith(".txt") }?.asList()
                ?.stream()?.map { f: File -> f.name.substring(0, f.name.length - 4) }?.collect(Collectors.toList())
                ?: emptyList()
        } else {
            createFolder()
            emptyList()
        }

    fun createFolder() {
        try {
            Files.createDirectory(OtherUtil.getPath(config.playlistsFolder!!))
        } catch (ignore: IOException) {
        }
    }

    fun folderExists(): Boolean {
        return Files.exists(OtherUtil.getPath(config.playlistsFolder!!))
    }

    @Throws(IOException::class)
    fun createPlaylist(name: String) {
        Files.createFile(OtherUtil.getPath(config.playlistsFolder + File.separator + name + ".txt"))
    }

    @Throws(IOException::class)
    fun deletePlaylist(name: String) {
        Files.delete(OtherUtil.getPath(config.playlistsFolder + File.separator + name + ".txt"))
    }

    @Throws(IOException::class)
    fun writePlaylist(name: String, text: String) {
        Files.write(
            OtherUtil.getPath(config.playlistsFolder + File.separator + name + ".txt"),
            text.trim { it <= ' ' }.toByteArray()
        )
    }

    fun getPlaylist(name: String): Playlist? {
        return if (!playlistNames.contains(name)) null else try {
            if (folderExists()) {
                val shuffle = booleanArrayOf(false)
                val list: MutableList<String> = ArrayList()
                Files.readAllLines(OtherUtil.getPath(config.playlistsFolder + File.separator + name + ".txt"))
                    .forEach { str: String ->
                        var s = str.trim { it <= ' ' }
                        if (s.isEmpty()) return@forEach
                        if (s.startsWith("#") || s.startsWith("//")) {
                            s = s.replace("\\s+".toRegex(), "")
                            if (s.equals("#shuffle", ignoreCase = true) || s.equals(
                                    "//shuffle",
                                    ignoreCase = true
                                )
                            ) shuffle[0] = true
                        } else list.add(s)
                    }
                if (shuffle[0]) shuffle(list)
                Playlist(name, list, shuffle[0])
            } else {
                createFolder()
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    inner class Playlist(val name: String, val items: List<String>, private val shuffle: Boolean) {
        val tracks: MutableList<AudioTrack> = LinkedList()
        val errors: MutableList<PlaylistLoadError> = LinkedList()
        private var loaded = false
        fun loadTracks(manager: AudioPlayerManager, consumer: Consumer<AudioTrack?>, callback: Runnable?) {
            if (loaded) return
            loaded = true
            for (i in items.indices) {
                val last = i + 1 == items.size
                manager.loadItemOrdered(name, items[i], object : AudioLoadResultHandler {
                    private fun done() {
                        if (last) {
                            if (shuffle) shuffleTracks()
                            callback?.run()
                        }
                    }

                    override fun trackLoaded(at: AudioTrack) {
                        if (config.isTooLong(at)) errors.add(
                            PlaylistLoadError(
                                i,
                                items[i],
                                "This track is longer than the allowed maximum"
                            )
                        ) else {
                            at.userData = 0L
                            tracks.add(at)
                            consumer.accept(at)
                        }
                        done()
                    }

                    override fun playlistLoaded(ap: AudioPlaylist) {
                        if (ap.isSearchResult) {
                            trackLoaded(ap.tracks[0])
                        } else if (ap.selectedTrack != null) {
                            trackLoaded(ap.selectedTrack)
                        } else {
                            val loaded: MutableList<AudioTrack> = ArrayList(ap.tracks)
                            if (shuffle) for (first in loaded.indices) {
                                val second = (Math.random() * loaded.size).toInt()
                                val tmp = loaded[first]
                                loaded[first] = loaded[second]
                                loaded[second] = tmp
                            }
                            loaded.removeIf { track: AudioTrack -> config.isTooLong(track) }
                            loaded.forEach(Consumer { at: AudioTrack -> at.userData = 0L })
                            tracks.addAll(loaded)
                            loaded.forEach(Consumer { at: AudioTrack? -> consumer.accept(at) })
                        }
                        done()
                    }

                    override fun noMatches() {
                        errors.add(PlaylistLoadError(i, items[i], "No matches found."))
                        done()
                    }

                    override fun loadFailed(fe: FriendlyException) {
                        errors.add(PlaylistLoadError(i, items[i], "Failed to load track: " + fe.localizedMessage))
                        done()
                    }
                })
            }
        }

        fun shuffleTracks() {
            shuffle(tracks)
        }
    }

    class PlaylistLoadError(val index: Int, val item: String, val reason: String)
    companion object {
        private fun <T> shuffle(list: MutableList<T>) {
            for (first in list.indices) {
                val second = (Math.random() * list.size).toInt()
                val tmp = list[first]
                list[first] = list[second]
                list[second] = tmp
            }
        }
    }
}
