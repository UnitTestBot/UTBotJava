package org.utbot.cli.go.util

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun now(): LocalDateTime = LocalDateTime.now()

fun durationInMillis(started: LocalDateTime): Long = ChronoUnit.MILLIS.between(started, now())