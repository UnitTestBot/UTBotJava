package org.utbot.instrumentation.util

import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.signature
import org.utbot.instrumentation.Executor
import org.utbot.instrumentation.execute
import java.lang.reflect.Method
import kotlin.reflect.KCallable
import kotlin.reflect.jvm.kotlinFunction

/**
 * Class, which creates isolated function from [executableId]. Delegates the function call to the [executor].
 */
class Isolated<TIResult>(
    private val kCallable: KCallable<*>,
    private val executor: Executor<TIResult>
) {
    constructor(
        method: Method,
        executor: Executor<TIResult>
    ) : this(method.kotlinFunction!!, executor)

    operator fun invoke(vararg args: Any?): TIResult {
        return executor.execute(kCallable, arrayOf(*args), null)
    }

    val name: String by lazy { kCallable.name }
    val signature: String by lazy { kCallable.signature }
}