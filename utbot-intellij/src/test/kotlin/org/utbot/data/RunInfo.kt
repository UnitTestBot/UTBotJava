package org.utbot.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

val TEST_RUN_NUMBER = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
val DEFAULT_TEST_GENERATION_TIMEOUT = 60L
val NEW_PROJECT_NAME_START = "Aut_${TEST_RUN_NUMBER}_"
val DEFAULT_PROJECT_DIRECTORY = "~\\IdeaProjects"
var random: Random = Random()
