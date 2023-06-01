package org.utbot.spring.config

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource
import org.utbot.spring.postProcessors.UtBotBeanFactoryPostProcessor

@Configuration
@ImportResource
open class TestApplicationConfiguration {

    @Bean
    open fun utBotBeanFactoryPostProcessor(): BeanFactoryPostProcessor = UtBotBeanFactoryPostProcessor
}