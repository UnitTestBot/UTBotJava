package org.utbot.spring.analyzers

interface SpringApplicationAnalyzer {
    fun analyze(analysisContext: SpringApplicationAnalysisContext): List<String>
}