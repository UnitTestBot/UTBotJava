package org.utbot.spring.analyzers

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.springframework.boot.builder.SpringApplicationBuilder
import org.utbot.spring.exception.UtBotSpringShutdownException

private val logger = getLogger<SpringBootApplicationAnalyzer>()

class SpringBootApplicationAnalyzer : SpringApplicationAnalyzer {
    override fun analyze(analysisContext: SpringApplicationAnalysisContext): List<String> {
        logger.info { "Analyzing Spring Boot application" }
        return try {
            val app = SpringApplicationBuilder(*analysisContext.sources)
                .environment(analysisContext.createEnvironment())
                .build()
            UtBotSpringShutdownException.catch { app.run() }.beanQualifiedNames
        } catch (e: Throwable) {
            logger.error("Failed to analyze Spring Boot application, falling back to using pure Spring", e)
            PureSpringApplicationAnalyzer().analyze(analysisContext)
        }
    }
}