package org.utbot.spring

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
import org.utbot.spring.api.SpringApi
import org.utbot.spring.api.RepositoryDescription
import org.utbot.spring.api.UTSpringContextLoadingException
import org.utbot.spring.api.provider.InstantiationSettings
import org.utbot.spring.dummy.DummySpringIntegrationTestClass
import org.utbot.spring.utils.DependencyUtils.isSpringDataOnClasspath
import org.utbot.spring.utils.RepositoryUtils
import java.lang.reflect.Method
import java.net.URLClassLoader
import kotlin.reflect.jvm.javaMethod

private val logger = getLogger<SpringApiImpl>()

class SpringApiImpl(
    instantiationSettings: InstantiationSettings,
    dummyTestClass: Class<out DummySpringIntegrationTestClass>,
) : SpringApi {
    private lateinit var dummyTestClassInstance: DummySpringIntegrationTestClass
    private val dummyTestClass = dummyTestClass.also {
        patchAnnotation(
            annotation = it.getAnnotation(ActiveProfiles::class.java),
            property = "value",
            newValue = instantiationSettings.profiles
        )
        patchAnnotation(
            annotation = it.getAnnotation(ContextConfiguration::class.java),
            property = "classes",
            newValue = instantiationSettings.configurationClasses
        )
    }
    private val dummyTestMethod: Method = DummySpringIntegrationTestClass::dummyTestMethod.javaMethod!!
    private val testContextManager: TestContextManager = TestContextManager(this.dummyTestClass)

    private val context get() = getOrLoadSpringApplicationContext()

    override fun getOrLoadSpringApplicationContext() = try {
        testContextManager.testContext.applicationContext as ConfigurableApplicationContext
    } catch (e: Throwable) {
        throw UTSpringContextLoadingException(e)
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
        if (!isSpringDataOnClasspath) return emptySet()
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

    private var beforeTestClassCalled = false
    private var isInsideTestMethod = false

    private fun beforeTestClass() {
        beforeTestClassCalled = true
        testContextManager.beforeTestClass()
        dummyTestClassInstance = dummyTestClass.getConstructor().newInstance()
        testContextManager.prepareTestInstance(dummyTestClassInstance)
    }

    override fun beforeTestMethod() {
        if (!beforeTestClassCalled)
            beforeTestClass()
        if (isInsideTestMethod) {
            logger.warn { "afterTestMethod() wasn't called for previous test method, calling it from beforeTestMethod()" }
            afterTestMethod()
        }
        testContextManager.beforeTestMethod(dummyTestClassInstance, dummyTestMethod)
        testContextManager.beforeTestExecution(dummyTestClassInstance, dummyTestMethod)
        isInsideTestMethod = true
    }

    override fun afterTestMethod() {
        testContextManager.afterTestExecution(dummyTestClassInstance, dummyTestMethod, null)
        testContextManager.afterTestMethod(dummyTestClassInstance, dummyTestMethod, null)
        isInsideTestMethod = false
    }

    private fun describesRepository(bean: Any): Boolean =
        try {
            bean is CrudRepository<*, *>
        } catch (e: ClassNotFoundException) {
            false
        }

    data class SimpleBeanDefinition(
        val beanName: String,
        val bean: Any,
    )
}