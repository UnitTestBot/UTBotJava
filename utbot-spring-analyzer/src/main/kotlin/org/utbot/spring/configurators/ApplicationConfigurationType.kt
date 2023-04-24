package org.utbot.spring.configurators

enum class ApplicationConfigurationType {

    XmlConfiguration,

    /**
     * Any Java-based configuration, including both simple @Configuration and @SpringBootApplication
     */
    JavaConfiguration,
}