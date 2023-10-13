package org.utbot.spring.api.provider

class InstantiationSettings(
    val configurationClasses: Array<Class<*>>,
    val profiles: Array<String>,
) {
    override fun toString(): String =
        "InstantiationSettings(configurationClasses=${configurationClasses.contentToString()}, profiles=${profiles.contentToString()})"
}