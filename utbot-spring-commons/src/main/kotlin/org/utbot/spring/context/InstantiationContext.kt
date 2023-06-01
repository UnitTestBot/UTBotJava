package org.utbot.spring.context

class InstantiationContext(
    val configurationClasses: Array<Class<*>>,
    val profileExpression: String?,
)
