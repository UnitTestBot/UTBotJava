package org.utbot.spring.repositoryWrapper

import java.lang.reflect.Method

data class RepositoryInteraction(
    val beanName: String,
    val method: Method,
    val args: List<Any?>,
    val result: Result<Any?>
) {
    companion object {
        @JvmStatic
        val repositoryInteractions = mutableListOf<RepositoryInteraction>()
    }

    // TODO once utbot-spring-commons-api is extracted remove these properties and just use `result` directly
    //  can't use `result` via reflection because it's an inline class and hash is appended to getter name
    val resultSuccess: Any?
        get() = result.getOrNull()

    val resultFailure: Throwable?
        get() = result.exceptionOrNull()
}