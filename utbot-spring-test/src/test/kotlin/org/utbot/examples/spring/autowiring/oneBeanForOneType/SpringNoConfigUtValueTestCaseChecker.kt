package org.utbot.examples.spring.autowiring.oneBeanForOneType

import org.utbot.examples.spring.utils.standardSpringTestingConfigurations
import org.utbot.testing.UtValueTestCaseChecker
import org.utbot.testing.springNoConfigApplicationContext
import kotlin.reflect.KClass

abstract class SpringNoConfigUtValueTestCaseChecker(
    testClass: KClass<*>
) : UtValueTestCaseChecker(
    testClass,
    configurations = standardSpringTestingConfigurations,
    applicationContext = springNoConfigApplicationContext
)