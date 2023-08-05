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

import net.dv8tion.jda.api.entities.User

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
class RequestMetadata(user: User?) {
    val user: RequestMetadata.UserInfo?

    init {
        this.user =
            if (user == null) null else UserInfo(user.idLong, user.name, user.discriminator, user.effectiveAvatarUrl)
    }

    val owner: Long
        get() = user?.id ?: 0

    inner class RequestInfo(val query: String, val url: String)
    inner class UserInfo(val id: Long, val username: String, val discrim: String, val avatar: String)
    companion object {
        val EMPTY = RequestMetadata(null)
    }
}
