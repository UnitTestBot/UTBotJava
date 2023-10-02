package org.utbot.data

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

val TEST_RUN_NUMBER: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))!!
val NEW_PROJECT_NAME_START = "Aut_${TEST_RUN_NUMBER}_"

val tempDirectoryPath: String = System.getProperty("java.io.tmpdir")
val DEFAULT_PROJECT_DIRECTORY = "${tempDirectoryPath + File.separator}Autotests"

const val SPRING_EXISTING_PROJECT_NAME = "spring-petclinic"
val random: Random = Random()
const val DEFAULT_TEST_GENERATION_TIMEOUT = 60L
