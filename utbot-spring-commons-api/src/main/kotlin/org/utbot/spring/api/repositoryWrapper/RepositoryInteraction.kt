package org.utbot.spring.api.repositoryWrapper

import java.lang.reflect.Method

data class RepositoryInteraction(
    val beanName: String,
    val method: Method,
    val args: List<Any?>,
    val result: Result<Any?>
) {
    companion object {
        val recordedInteractions = mutableListOf<RepositoryInteraction>()
    }
}