package org.utbot.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val PROJECT_PATH = "D:\\JavaProjects" // with "\\" for Windows, "\/" for Linux/mac
val EXISTING_PROJECT_NAME = "Oct24Maven11"
val TEST_RUN_NUMBER = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
val DEFAULT_TEST_GENERATION_TIMEOUT = 60L
val NEW_PROJECT_NAME_START = "Aut_${TEST_RUN_NUMBER}_"
