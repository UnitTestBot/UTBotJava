package org.utbot.framework.context.utils

import org.utbot.framework.context.JavaFuzzingContext
import org.utbot.fuzzing.JavaValueProvider

fun JavaFuzzingContext.transformValueProvider(
    transformer: (JavaValueProvider) -> JavaValueProvider
) = object : JavaFuzzingContext by this {
    override val valueProvider: JavaValueProvider =
        transformer(this@transformValueProvider.valueProvider)
}

fun JavaFuzzingContext.withValueProvider(
    valueProvider: JavaValueProvider
) = transformValueProvider { it.with(valueProvider) }