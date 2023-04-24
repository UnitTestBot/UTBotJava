package org.utbot.spring.analyzers

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.utbot.spring.exception.UtBotSpringShutdownException

private val logger = getLogger<PureSpringApplicationAnalyzer>()

class PureSpringApplicationAnalyzer : SpringApplicationAnalyzer {
    override fun analyze(analysisContext: SpringApplicationAnalysisContext): List<String> {
        logger.info { "Analyzing pure Spring application" }
        val applicationContext = AnnotationConfigApplicationContext()
        applicationContext.register(*analysisContext.sources)
        applicationContext.environment = analysisContext.createEnvironment()
        return UtBotSpringShutdownException.catch { applicationContext.refresh() }.beanQualifiedNames
    }
}