package org.utbot.spring.repositoryWrapper

import org.utbot.spring.api.repositoryWrapper.RepositoryInteraction
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class RepositoryWrapperInvocationHandler(
    private val originalRepository: Any,
    private val beanName: String
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val nonNullArgs = args ?: emptyArray()
        val result = try {
            Result.success(method.invoke(originalRepository, *nonNullArgs))
        } catch (e: InvocationTargetException) {
            Result.failure(e.targetException)
        }
        RepositoryInteraction.recordedInteractions.add(
            RepositoryInteraction(beanName, method, nonNullArgs.toList(), result)
        )
        return result.getOrThrow()
    }
}