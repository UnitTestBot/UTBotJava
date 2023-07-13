package org.utbot.framework.context.spring

import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringCodeGenerationContext
import org.utbot.framework.plugin.api.SpringContextLoadingResult

/**
 * Data we get from Spring application context
 * to manage engine and code generator behaviour.
 */
// TODO #2358
interface SpringApplicationContext : ApplicationContext, SpringCodeGenerationContext {
    /**
     * Describes bean definitions (bean name, type, some optional additional data)
     */
    val beanDefinitions: List<BeanDefinitionData>
    val springInjectedClasses: Set<ClassId>
    val allInjectedTypes: Set<ClassId>

    override var springContextLoadingResult: SpringContextLoadingResult?
    fun getBeansAssignableTo(classId: ClassId): List<BeanDefinitionData>
}