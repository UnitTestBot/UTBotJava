package org.utbot.spring.context

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.repository.CrudRepository
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.context.RepositoryDescription

private val logger = getLogger<SpringContextWrapper>()

class SpringContextWrapper(override val context: ConfigurableApplicationContext) : ContextWrapper {

    private val springClassPrefix = "org.springframework"

    private val isCrudRepositoryOnClasspath = try {
        CrudRepository::class.java.name
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    override fun getBean(beanName: String): Any = context.getBean(beanName)

    override fun getDependenciesForBean(beanName: String): Set<String> {
        val analyzedBeanNames = mutableSetOf<String>()
        return getDependenciesForBeanInternal(beanName, analyzedBeanNames)
    }

    private fun getDependenciesForBeanInternal(
        beanName: String,
        analyzedBeanNames: MutableSet<String>,
        ): Set<String> {
        if (beanName in analyzedBeanNames) {
            return emptySet()
        }

        analyzedBeanNames.add(beanName)

        val dependencyBeanNames = context.beanFactory
            .getDependenciesForBean(beanName)
            .filter { it in context.beanDefinitionNames } // filters out inner beans
            .toSet()

        return setOf(beanName) + dependencyBeanNames.flatMap { getDependenciesForBean(it) }
    }

    override fun resetBean(beanName: String) {
        val beanDefinitionRegistry = context.beanFactory as BeanDefinitionRegistry

        val beanDefinition = context.beanFactory.getBeanDefinition(beanName)
        beanDefinitionRegistry.removeBeanDefinition(beanName)
        beanDefinitionRegistry.registerBeanDefinition(beanName, beanDefinition)
    }

    override fun resolveRepositories(beanNames: Set<String>): Set<RepositoryDescription> {
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
                .filterNot { it.name.startsWith(springClassPrefix) }
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

    private fun describesRepository(bean: Any): Boolean =
        try {
            bean is CrudRepository<*, *>
        } catch (e: ClassNotFoundException) {
            false
        }

    private fun getTableName(entity: Class<*>): String = entity.simpleName.decapitalize() + "s"

    data class SimpleBeanDefinition(
        val beanName: String,
        val bean: Any,
    )
}