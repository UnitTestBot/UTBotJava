package config

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import post_processors.UtBotBeanFactoryPostProcessor

@Configuration
open class TestApplicationConfiguration {

    @Bean
    open fun utBotBeanFactoryPostProcessor(): BeanFactoryPostProcessor {
        return UtBotBeanFactoryPostProcessor()
    }
}