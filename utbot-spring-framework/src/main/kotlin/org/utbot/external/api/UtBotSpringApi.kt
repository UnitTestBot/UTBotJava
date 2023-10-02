package org.utbot.external.api

import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.simple.SimpleApplicationContext
import org.utbot.framework.context.spring.SpringApplicationContext
import org.utbot.framework.context.spring.SpringApplicationContextImpl
import org.utbot.framework.plugin.api.SpringConfiguration
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.SpringTestType
import org.utbot.framework.process.SpringAnalyzerTask
import java.io.File

object UtBotSpringApi {
    private val springBootConfigAnnotations = setOf(
        "org.springframework.boot.autoconfigure.SpringBootApplication",
        "org.springframework.boot.SpringBootConfiguration"
    )

    /**
     * NOTE: [classpath] should include project under test classpath (with all dependencies) as well as
     * `spring-test`, `spring-boot-test`, and `spring-security-test` if respectively `spring-beans`,
     * `spring-boot`, and `spring-security-core` are dependencies of project under test.
     *
     * UtBot doesn't add Spring test modules to classpath automatically to let API users control their versions.
     */
    @JvmOverloads
    @JvmStatic
    fun createSpringApplicationContext(
        springSettings: SpringSettings,
        springTestType: SpringTestType,
        classpath: List<String>,
        delegateContext: ApplicationContext = SimpleApplicationContext()
    ): SpringApplicationContext {
        if (springTestType == SpringTestType.INTEGRATION_TEST) {
            val configuration = (springSettings as? SpringSettings.PresentSpringSettings)?.configuration
            require(configuration !is SpringConfiguration.XMLConfiguration) {
                "Integration tests aren't supported for XML configurations, consider using Java " +
                        "configuration that imports your XML configuration with @ImportResource"
            }
        }
        return SpringApplicationContextImpl.internalCreate(
            delegateContext = delegateContext,
            beanDefinitions = when (springSettings) {
                SpringSettings.AbsentSpringSettings -> listOf()
                is SpringSettings.PresentSpringSettings -> SpringAnalyzerTask(classpath, springSettings).perform()
            },
            springTestType = springTestType,
            springSettings = springSettings,
        )
    }

    @JvmStatic
    fun createXmlSpringConfiguration(xmlConfig: File): SpringConfiguration.XMLConfiguration =
        SpringConfiguration.XMLConfiguration(xmlConfig.absolutePath)

    @JvmStatic
    fun createJavaSpringConfiguration(javaConfig: Class<*>): SpringConfiguration.JavaBasedConfiguration =
        if (javaConfig.annotations.any { it.annotationClass.java.name in springBootConfigAnnotations }) {
            SpringConfiguration.SpringBootConfiguration(javaConfig.name, isDefinitelyUnique = false)
        } else {
            SpringConfiguration.JavaConfiguration(javaConfig.name)
        }
}