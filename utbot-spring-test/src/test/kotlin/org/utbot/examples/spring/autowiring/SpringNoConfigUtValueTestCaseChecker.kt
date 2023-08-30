package org.utbot.examples.spring.autowiring

import org.utbot.examples.spring.utils.standardSpringTestingConfigurations
import org.utbot.framework.context.spring.SpringApplicationContextImpl
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.SpringTestType
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.defaultApplicationContext
import kotlin.reflect.KClass

val springNoConfigApplicationContext = SpringApplicationContextImpl(
    delegateContext = defaultApplicationContext,
    springTestType = SpringTestType.UNIT_TEST,
    springSettings = SpringSettings.AbsentSpringSettings,
    beanDefinitions = emptyList()
)

abstract class SpringNoConfigUtValueTestCaseChecker(
    testClass: KClass<*>
) : UtValueTestCaseChecker(
    testClass,
    configurations = standardSpringTestingConfigurations,
    applicationContext = springNoConfigApplicationContext
)