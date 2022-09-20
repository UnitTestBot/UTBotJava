package org.utbot.common

import java.util.*

private val os = System.getProperty("os.name").lowercase(Locale.getDefault())
val isWindows = os.startsWith("windows")
val isUnix = !isWindows
val isMac = os.startsWith("mac")