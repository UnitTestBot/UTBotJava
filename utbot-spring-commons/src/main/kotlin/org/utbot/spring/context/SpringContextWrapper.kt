package org.utbot.spring.context

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.repository.CrudRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestContextManager
import org.utbot.common.hasOnClasspath
import org.utbot.common.patchAnnotation
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.context.RepositoryDescription
import org.utbot.spring.api.instantiator.InstantiationSettings
import java.lang.reflect.Method
import java.net.URLClassLoader
import kotlin.reflect.jvm.javaMethod

private val logger = getLogger<SpringContextWrapper>()

class SpringContextWrapper(
    instantiationSettings: InstantiationSettings,
    dummyTestClass: Class<out BaseDummyTestClass>,
) : ContextWrapper {
    private lateinit var dummyTestInstance: BaseDummyTestClass
    private val dummyTestClass = dummyTestClass.also {
        patchAnnotation(it, ActiveProfiles::class.java, "value", parseProfileExpression(instantiationSettings.profileExpression))
        patchAnnotation(it, ContextConfiguration::class.java, "classes", instantiationSettings.configurationClasses)
    }
    private val dummyTestMethod: Method = BaseDummyTestClass::dummyTestMethod.javaMethod!!
    private val testContextManager: TestContextManager = TestContextManager(this.dummyTestClass)

    override val context: ConfigurableApplicationContext
        get() = testContextManager.testContext.applicationContext as ConfigurableApplicationContext

    private val isCrudRepositoryOnClasspath = try {
        CrudRepository::class.java.name
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    override fun getBean(beanName: String): Any = context.getBean(beanName)

    override fun getDependenciesForBean(beanName: String, userSourcesClassLoader: URLClassLoader): Set<String> {
        val analyzedBeanNames = mutableSetOf<String>()
        return getDependenciesForBeanInternal(beanName, analyzedBeanNames, userSourcesClassLoader)
    }

    private fun getDependenciesForBeanInternal(
        beanName: String,
        analyzedBeanNames: MutableSet<String>,
        userSourcesClassLoader: URLClassLoader,
        ): Set<String> {
        if (beanName in analyzedBeanNames) {
            return emptySet()
        }

        analyzedBeanNames.add(beanName)

        val dependencyBeanNames = context.beanFactory
            .getDependenciesForBean(beanName)
            // this filtering is applied to avoid inner beans
            .filter { it in context.beanDefinitionNames }
            .filter { name ->
                val clazz = getBean(name)::class.java
                // here immediate hierarchy is enough because proxies are inherited directly
                val immediateClazzHierarchy = clazz.interfaces + clazz.superclass + clazz
                immediateClazzHierarchy.any { clazz -> userSourcesClassLoader.hasOnClasspath(clazz.name) }
            }
            .toSet()

        return setOf(beanName) + dependencyBeanNames.flatMap { getDependenciesForBean(it, userSourcesClassLoader) }
    }

    override fun resetBean(beanName: String) {
        val beanDefinitionRegistry = context.beanFactory as BeanDefinitionRegistry

        val beanDefinition = context.beanFactory.getBeanDefinition(beanName)
        beanDefinitionRegistry.removeBeanDefinition(beanName)
        beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition)
    }

    override fun resolveRepositories(beanNames: Set<String>, userSourcesClassLoader: URLClassLoader): Set<RepositoryDescription> {
        if (!isCrudRepositoryOnClasspath) return emptySet()
        val repositoryBeans = beanNames
            .map { beanName -> SimpleBeanDefinition(beanName, getBean(beanName)) }
            .filter { beanDef -> describesRepository(beanDef.bean) }
            .toSet()

        val descriptions = mutableSetOf<RepositoryDescription>()

        for (repositoryBean in repositoryBeans) {
            val repositoryClass = repositoryBean.bean::class.java
            val repositoryClassName = repositoryClass
                .interfaces
                .filter { clazz -> userSourcesClassLoader.hasOnClasspath(clazz.name) }
                .filter { CrudRepository::class.java.isAssignableFrom(it) }
                .map { it.name }
                .firstOrNull() ?: CrudRepository::class.java.name

            val entity = RepositoryUtils.getEntityClass(repositoryClass)

            if (entity != null) {
                descriptions += RepositoryDescription(
                    beanName = repositoryBean.beanName,
                    repositoryName = repositoryClassName,
                    entityName = entity.name,
                    tableName = getTableName(entity),
                )
            } else {
                logger.warn {
                    "Failed to get entity class for bean ${repositoryBean.beanName} " +
                            "that was recognised as a repository of type $repositoryClassName"
                }
            }
        }

        return descriptions
    }

    override fun beforeTestClass() {
        testContextManager.beforeTestClass()
        dummyTestInstance = dummyTestClass.getConstructor().newInstance()
        testContextManager.prepareTestInstance(dummyTestInstance)
    }

    override fun beforeTestMethod() {
        testContextManager.beforeTestMethod(dummyTestInstance, dummyTestMethod)
        testContextManager.beforeTestExecution(dummyTestInstance, dummyTestMethod)
    }

    override fun afterTestMethod() {
        testContextManager.afterTestExecution(dummyTestInstance, dummyTestMethod, null)
        testContextManager.afterTestMethod(dummyTestInstance, dummyTestMethod, null)
    }

    private fun describesRepository(bean: Any): Boolean =
        try {
            bean is CrudRepository<*, *>
        } catch (e: ClassNotFoundException) {
            false
        }

    private fun getTableName(entity: Class<*>): String = entity.simpleName.decapitalize() + "s"

    companion object {
        private const val DEFAULT_PROFILE_NAME = "default"

        /**
         * Transforms active profile information
         * from the form of user input to a list of active profiles.
         *
         * Current user input form is comma-separated values, but it may be changed later.
         */
        private fun parseProfileExpression(profileExpression: String?): Array<String> =
            if (profileExpression.isNullOrEmpty()) arrayOf(DEFAULT_PROFILE_NAME)
            else profileExpression
                .filter { !it.isWhitespace() }
                .split(',')
                .toTypedArray()
    }

    data class SimpleBeanDefinition(
        val beanName: String,
        val bean: Any,
    )
}