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
package com.jagrosh.jmusicbot.queue

/**
 *
 * @author John Grosh (jagrosh)
 * @param <T>
</T> */
class FairQueue<T : Queueable> {
    private val list: MutableList<T> = ArrayList()
    private val set: MutableSet<Long> = HashSet()
    fun add(item: T): Int {
        var lastIndex: Int = list.lastIndex
        while (lastIndex > -1) {
            if (list[lastIndex].identifier == item.identifier) break
            lastIndex--
        }
        lastIndex++
        set.clear()
        while (lastIndex < list.size) {
            if (set.contains(list[lastIndex].identifier)) break
            set.add(list[lastIndex].identifier)
            lastIndex++
        }
        list.add(lastIndex, item)
        return lastIndex
    }

    fun addAt(index: Int, item: T) {
        if (index >= list.size) list.add(item) else list.add(index, item)
    }

    fun size(): Int {
        return list.size
    }

    fun pull(): T {
        return list.removeFirst()
    }

    val isEmpty: Boolean
        get() = list.isEmpty()

    fun getList(): List<T> {
        return list
    }

    operator fun get(index: Int): T {
        return list[index]
    }

    fun remove(index: Int): T {
        return list.removeAt(index)
    }

    fun removeAll(identifier: Long): Int {
        var count = 0
        for (i in list.indices.reversed()) {
            if (list[i].identifier == identifier) {
                list.removeAt(i)
                count++
            }
        }
        return count
    }

    fun clear() {
        list.clear()
    }

    fun shuffle(identifier: Long): Int {
        val iset: MutableList<Int> = ArrayList()
        for (i in list.indices) {
            if (list[i].identifier == identifier) iset.add(i)
        }
        for (j in iset.indices) {
            val first = iset[j]
            val second = iset[(Math.random() * iset.size).toInt()]
            val temp = list[first]
            list[first] = list[second]
            list[second] = temp
        }
        return iset.size
    }

    fun skip(number: Int) {
        for (i in 0 until number) list.removeFirst()
    }

    /**
     * Move an item to a different position in the list
     * @param from The position of the item
     * @param to The new position of the item
     * @return the moved item
     */
    fun moveItem(from: Int, to: Int): T {
        val item = list.removeAt(from)
        list.add(to, item)
        return item
    }
}
