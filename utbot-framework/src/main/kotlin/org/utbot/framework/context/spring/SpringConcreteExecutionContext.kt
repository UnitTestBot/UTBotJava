package org.utbot.framework.context.spring

import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.plugin.api.UtError

class SpringConcreteExecutionContext(
    private val delegateContext: ConcreteExecutionContext,
    private val springApplicationContext: SpringApplicationContext,
) : ConcreteExecutionContext {
    override fun preventsFurtherTestGeneration(): Boolean =
        delegateContext.preventsFurtherTestGeneration() ||
                springApplicationContext.springContextLoadingResult?.contextLoaded == false

    override fun getErrors(): List<UtError> =
        springApplicationContext.springContextLoadingResult?.exceptions?.map { exception ->
            UtError(
                "Failed to load Spring application context",
                exception
            )
        }.orEmpty() + delegateContext.getErrors()
}