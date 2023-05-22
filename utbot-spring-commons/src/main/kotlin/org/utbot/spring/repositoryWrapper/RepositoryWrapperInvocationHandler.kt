package org.utbot.spring.repositoryWrapper

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class RepositoryWrapperInvocationHandler(
    private val originalRepository: Any,
    private val beanName: String
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val nonNullArgs = args ?: emptyArray()
        val result = runCatching { method.invoke(originalRepository, *nonNullArgs) }
        RepositoryInteraction.repositoryInteractions.add(
            RepositoryInteraction(beanName, method, nonNullArgs.toList(), result)
        )
        return result.getOrThrow()
    }
}