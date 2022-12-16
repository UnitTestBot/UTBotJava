package org.utbot.language.ts.service

interface ICoverageService {

    fun getCoveredLines(): List<Set<Int>>
}