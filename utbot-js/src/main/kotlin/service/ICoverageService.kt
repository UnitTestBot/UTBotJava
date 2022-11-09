package service

interface ICoverageService {

    fun getCoveredLines(): List<Set<Int>>
}