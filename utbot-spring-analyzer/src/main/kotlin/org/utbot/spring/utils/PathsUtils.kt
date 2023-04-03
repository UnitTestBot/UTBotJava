package org.utbot.spring.utils

import kotlin.io.path.Path

object PathsUtils {
    const val EMPTY_PATH = ""

    fun createFakeFilePath(fileName: String): String = "fake_${Path(fileName).fileName}"
}
