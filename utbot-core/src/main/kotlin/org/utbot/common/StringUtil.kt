package org.utbot.common

import java.math.BigInteger
import java.security.MessageDigest
import java.util.IdentityHashMap

private val reentrancyMap = ThreadLocal<IdentityHashMap<Any, Int>>()
private val reentrancyLevel = ThreadLocal<Int>()
private val refCount = ThreadLocal<Int>()

/**
 *
 */
fun Any.withToStringThreadLocalReentrancyGuard(block: Int.() -> String): String {

    val oldLevel = reentrancyLevel.get() ?: 0
    if (oldLevel == 0)  {
        require(reentrancyMap.get() == null)

        reentrancyLevel.set(0)
        reentrancyMap.set(IdentityHashMap())
        refCount.set(0)
    }

    try {
        reentrancyLevel.set(oldLevel + 1)
        val map = reentrancyMap.get()?: error("Mustn't be null")

        val existing = map[this]
        if (existing != null) return "<back-ref: $existing>"

        val ref = refCount.get() + 1
        refCount.set(ref)
        map[this] = refCount.get()

        return ref.block()
    } finally {
        if (oldLevel == 0) {
            reentrancyLevel.remove()
            reentrancyMap.remove()
            refCount.remove()
        } else {
            reentrancyLevel.set(oldLevel)
        }
    }
}

val md5: MessageDigest by lazy { MessageDigest.getInstance("MD5")}
fun String.md5() :String = BigInteger(1, md5.digest(toByteArray())).toString(16).padStart(32, '0')

const val HTML_LINE_SEPARATOR = "<br>"

fun StringBuilder.appendHtmlLine(): StringBuilder = append(HTML_LINE_SEPARATOR)
fun StringBuilder.appendHtmlLine(line: String): StringBuilder = append(line).append(HTML_LINE_SEPARATOR)