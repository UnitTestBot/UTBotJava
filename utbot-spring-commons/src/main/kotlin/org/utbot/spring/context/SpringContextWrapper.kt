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
                .map { it.name }
                .firstOrNull()

            val entity = RepositoryUtils.getEntityClass(repositoryClass)

            if (!repositoryClassName.isNullOrEmpty() && !entity?.name.isNullOrEmpty()) {
                entity?.let {
                    descriptions += RepositoryDescription(
                        beanName = repositoryBean.beanName,
                        repositoryName = repositoryClassName,
                        entityName = entity.name,
                        tableName = getTableName(entity),
                    )
                }
            } else {
                logger.warn {
                    "Bean named ${repositoryBean.beanName} as recognized as repository bean, but " +
                            "repository class name is $repositoryClassName and entity name is ${entity?.name}"
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