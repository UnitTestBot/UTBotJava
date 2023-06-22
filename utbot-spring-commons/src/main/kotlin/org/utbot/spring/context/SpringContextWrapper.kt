package org.utbot.spring.context

import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.repository.CrudRepository
import org.utbot.spring.api.context.ContextWrapper
import org.utbot.spring.api.context.RepositoryDescription
import org.utbot.spring.api.context.SimpleBeanDefinition

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

        val namedBeans = mutableSetOf<String>()
        namedBeans += beanName
        analyzedBeanNames.add(beanName)


        val dependencyBeanNames = context.beanFactory
            .getDependenciesForBean(beanName)
            .filter { it in  context.beanDefinitionNames }
            .toSet()

        namedBeans += dependencyBeanNames
        dependencyBeanNames.flatMap { dependencyBeanName -> getDependenciesForBean(dependencyBeanName) }

        return namedBeans
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
            val repositoryName = repositoryClass
                .interfaces
                .filterNot { it.name.startsWith(springClassPrefix) }
                .map { it.name }
                .firstOrNull()

            val entity = RepositoryUtils.getEntity(repositoryClass)

            if (!repositoryName.isNullOrEmpty() && !entity?.name.isNullOrEmpty()) {
                entity?.let {
                    val beanName = repositoryBean.beanName
                    val entityName = entity.name
                    val tableName = getTableName(entity)
                    descriptions += RepositoryDescription(beanName, repositoryName, entityName, tableName)
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
}