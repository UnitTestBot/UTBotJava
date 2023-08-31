package org.utbot.spring.api

import java.net.URLClassLoader

//TODO: `userSourcesClassLoader` must not be passed as a method argument, requires refactoring
interface SpringApi {
    /**
     * NOTE! [Any] return type is used here because Spring itself may not be on the classpath of the API user class loader
     *
     * @throws [UTSpringContextLoadingException] as a wrapper for all runtime exceptions
     */
    fun getOrLoadSpringApplicationContext(): Any

    /**
     * NOTE! [Any] return type is used here because Spring itself may not be on the classpath of the API user class loader
     *
     * Besides that `entityManager` depending on libraries used by user may be either `javax.persistence.EntityManager`
     * or `jakarta.persistence.EntityManager` and they don't have common super type other than `Object`.
     */
    fun getEntityManager(): Any

    fun getBean(beanName: String): Any

    fun getDependenciesForBean(beanName: String, userSourcesClassLoader: URLClassLoader): Set<String>

    fun resetBean(beanName: String)

    fun resolveRepositories(beanNames: Set<String>, userSourcesClassLoader: URLClassLoader): Set<RepositoryDescription>

    /**
     * NOTE! Should be called on one thread with method under test and value constructor,
     * because transactions are bound to threads
     */
    fun beforeTestMethod()

    /**
     * NOTE! Should be called on one thread with method under test and value constructor,
     * because transactions are bound to threads
     */
    fun afterTestMethod()

    /**
     * Time-consuming operation that fully resets Spring context
     */
    fun resetContext()
}

data class RepositoryDescription(
    val beanName: String,
    val repositoryName: String,
    val entityName: String,
)