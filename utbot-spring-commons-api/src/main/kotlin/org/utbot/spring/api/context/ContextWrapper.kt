package org.utbot.spring.api.context

import java.net.URLClassLoader

//TODO: `userSourcesClassLoader` must not be passed as a method argument, requires refactoring
interface ContextWrapper {
    val context: Any

    fun getBean(beanName: String): Any

    fun getDependenciesForBean(beanName: String, userSourcesClassLoader: URLClassLoader): Set<String>

    fun resetBean(beanName: String): Any

    fun resolveRepositories(beanNames: Set<String>, userSourcesClassLoader: URLClassLoader): Set<RepositoryDescription>
}

data class RepositoryDescription(
    val beanName: String,
    val repositoryName: String,
    val entityName: String,
    val tableName: String,
)