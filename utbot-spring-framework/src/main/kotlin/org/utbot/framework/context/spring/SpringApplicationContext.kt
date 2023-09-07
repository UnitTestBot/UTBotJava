package org.utbot.framework.context.spring

import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings

/**
 * Data we get from Spring application context
 * to manage engine and code generator behaviour.
 */
interface SpringApplicationContext : ApplicationContext {
    val springSettings: SpringSettings

    /**
     * Describes bean definitions (bean name, type, some optional additional data)
     */
    val beanDefinitions: List<BeanDefinitionData>
    val injectedTypes: Set<ClassId>
    val allInjectedSuperTypes: Set<ClassId>

    var concreteContextLoadingResult: ConcreteContextLoadingResult?
    fun getBeansAssignableTo(classId: ClassId): List<BeanDefinitionData>
}