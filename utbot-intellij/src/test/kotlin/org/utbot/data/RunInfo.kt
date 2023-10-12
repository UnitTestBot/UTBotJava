package org.utbot.data

import org.utbot.common.utBotTempDirectory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Random

val TEST_RUN_NUMBER: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))!!

val tempDirectoryPath: String = utBotTempDirectory.toAbsolutePath().toString()
const val DEFAULT_DIRECTORY_NAME = "Autotests"
val DEFAULT_DIRECTORY_FULL_PATH = tempDirectoryPath + File.separator + DEFAULT_DIRECTORY_NAME
val CURRENT_RUN_DIRECTORY_FULL_PATH = DEFAULT_DIRECTORY_FULL_PATH + File.separator + TEST_RUN_NUMBER
val CURRENT_RUN_DIRECTORY_END = DEFAULT_DIRECTORY_NAME + File.separator + TEST_RUN_NUMBER

const val SPRING_PROJECT_NAME = "spring-petclinic"
const val SPRING_PROJECT_URL = "https://github.com/spring-projects/spring-petclinic.git"

val random: Random = Random()
