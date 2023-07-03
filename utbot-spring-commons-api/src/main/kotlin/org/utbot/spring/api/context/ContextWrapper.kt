package org.utbot.spring.api.context

import java.net.URLClassLoader

//TODO: `userSourcesClassLoader` must not be passed as a method argument, requires refactoring
interface ContextWrapper {
    val context: Any

    fun getBean(beanName: String): Any

    fun getDependenciesForBean(beanName: String, userSourcesClassLoader: URLClassLoader): Set<String>

    fun resetBean(beanName: String): Any

    fun resolveRepositories(beanNames: Set<String>, userSourcesClassLoader: URLClassLoader): Set<RepositoryDescription>

    /**
     * Should be called once before any invocations of [beforeTestMethod] and [afterTestMethod]
     */
    fun beforeTestClass()

    /**
     * Should be called on one thread with method under test and value constructor,
     * because transactions are bound to threads
     */
    fun beforeTestMethod()

    /**
     * Should be called on one thread with method under test and value constructor,
     * because transactions are bound to threads
     */
    fun afterTestMethod()
}

data class RepositoryDescription(
    val beanName: String,
    val repositoryName: String,
    val entityName: String,
    val tableName: String,
)