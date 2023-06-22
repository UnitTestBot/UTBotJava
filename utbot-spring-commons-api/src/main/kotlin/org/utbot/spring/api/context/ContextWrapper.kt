package org.utbot.spring.api.context

interface ContextWrapper {
    val context: Any

    fun getBean(beanName: String): Any

    fun getDependenciesForBean(beanName: String): Set<String>

    fun resetBean(beanName: String): Any

    fun resolveRepositories(beanNames: Set<String>): Set<RepositoryDescription>
}

data class RepositoryDescription(
    val beanName: String,
    val repositoryName: String,
    val entityName: String,
    val tableName: String,
)