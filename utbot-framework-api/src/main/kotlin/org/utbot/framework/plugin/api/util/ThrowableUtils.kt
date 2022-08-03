package org.utbot.framework.plugin.api.util

import kotlinx.coroutines.runBlocking
import org.utbot.jcdb.api.ClassId

val Throwable.description
    get() = message?.replace('\n', '\t') ?: "<Throwable with empty message>"

val Throwable.isCheckedException
    get() = !(this is RuntimeException || this is Error)

suspend fun ClassId.allParents(): List<ClassId> {
    var clazz = this
    val list = arrayListOf<ClassId>()
    while (true) {
        val parent = clazz.superclass()
        if (parent != null) {
            list.add(parent)
        } else {
            break
        }
        clazz = parent
    }
    return list
}

private val runtimeException = RuntimeException::class.java.name
private val error = Error::class.java.name

suspend fun ClassId.isCheckedException(): Boolean {
    return allParents().all { it.name != runtimeException && it.name != error }
}

val ClassId.isCheckedException: Boolean
    get() = runBlocking {
        isCheckedException()
    }