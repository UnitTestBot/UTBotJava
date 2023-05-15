package org.utbot.spring.analyzers

import org.springframework.core.env.ConfigurableEnvironment
import org.utbot.spring.generated.BeanDefinitionData

interface SpringApplicationAnalyzer {

    fun canAnalyze(): Boolean

    fun analyze(sources: Array<Class<*>>, environment: ConfigurableEnvironment): List<BeanDefinitionData>
}