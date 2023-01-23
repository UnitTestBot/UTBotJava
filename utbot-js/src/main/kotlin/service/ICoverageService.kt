package service

import utils.CoverageData

interface ICoverageService {

    fun getCoveredLines(): List<CoverageData>
}