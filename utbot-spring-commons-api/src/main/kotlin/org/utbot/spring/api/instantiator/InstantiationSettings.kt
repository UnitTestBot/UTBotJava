package org.utbot.spring.api.instantiator

class InstantiationSettings(
    val configurationClasses: Array<Class<*>>,
    val profileExpression: String?,
)
