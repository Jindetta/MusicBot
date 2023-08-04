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
package com.jagrosh.jmusicbot.gui

import java.awt.EventQueue
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.*
import javax.swing.JTextArea

/**
 *
 * @author Lawrence Dol
 */
class TextAreaOutputStream @JvmOverloads constructor(txtara: JTextArea, maxlin: Int = 1000) : OutputStream() {
    // *************************************************************************************************
    // INSTANCE MEMBERS
    // *************************************************************************************************
    private val oneByte // array for write(int val);
            : ByteArray
    private var appender // most recent action
            : Appender?

    init {
        require(maxlin >= 1) { "TextAreaOutputStream maximum lines must be positive (value=$maxlin)" }
        oneByte = ByteArray(1)
        appender = Appender(txtara, maxlin)
    }

    /** Clear the current console text area.  */
    @Synchronized
    fun clear() {
        appender?.clear()
    }

    @Synchronized
    override fun close() {
        appender = null
    }

    @Synchronized
    override fun flush() {
        /* empty */
    }

    @Synchronized
    override fun write(`val`: Int) {
        oneByte[0] = `val`.toByte()
        write(oneByte, 0, 1)
    }

    @Synchronized
    override fun write(ba: ByteArray) {
        write(ba, 0, ba.size)
    }

    @Synchronized
    override fun write(ba: ByteArray, str: Int, len: Int) {
        if (appender != null) {
            appender!!.append(bytesToString(ba, str, len))
        }
    }

    // *************************************************************************************************
    // STATIC MEMBERS
    // *************************************************************************************************
    internal class Appender(
        private val textArea: JTextArea, // maximum lines allowed in text area
        private val maxLines: Int
    ) : Runnable {
        private val lengths // length of lines within text area
                : LinkedList<Int>
        private val values // values waiting to be appended
                : MutableList<String>
        private var curLength // length of current line
                : Int
        private var clear: Boolean
        private var queue: Boolean

        init {
            lengths = LinkedList()
            values = ArrayList()
            curLength = 0
            clear = false
            queue = true
        }

        @Synchronized
        fun append(`val`: String) {
            values.add(`val`)
            if (queue) {
                queue = false
                EventQueue.invokeLater(this)
            }
        }

        @Synchronized
        fun clear() {
            clear = true
            curLength = 0
            lengths.clear()
            values.clear()
            if (queue) {
                queue = false
                EventQueue.invokeLater(this)
            }
        }

        // MUST BE THE ONLY METHOD THAT TOUCHES textArea!
        @Synchronized
        override fun run() {
            if (clear) {
                textArea.text = ""
            }
            values.stream().map<String> { `val`: String ->
                curLength += `val`.length
                `val`
            }.map<String> { `val`: String ->
                if (`val`.endsWith(EOL1) || `val`.endsWith(EOL2)) {
                    if (lengths.size >= maxLines) {
                        textArea.replaceRange("", 0, lengths.removeFirst())
                    }
                    lengths.addLast(curLength)
                    curLength = 0
                }
                `val`
            }.forEach { `val`: String? -> textArea.append(`val`) }
            values.clear()
            clear = false
            queue = true
        }

        companion object {
            private const val EOL1 = "\n"
            private val EOL2 = System.getProperty("line.separator", EOL1)
        }
    }

    companion object {
        //@edu.umd.cs.findbugs.annotations.SuppressWarnings("DM_DEFAULT_ENCODING")
        private fun bytesToString(ba: ByteArray, str: Int, len: Int): String {
            return try {
                String(ba, str, len, charset("UTF-8"))
            } catch (thr: UnsupportedEncodingException) {
                String(ba, str, len)
            } // all JVMs are required to support UTF-8
        }
    }
} /* END PUBLIC CLASS */