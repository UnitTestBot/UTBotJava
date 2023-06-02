package org.utbot.spring.repositoryWrapper

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RepositoryWrapperConfiguration {
    @Bean
    open fun utBotRepositoryWrapper(): BeanPostProcessor =
        RepositoryWrapperBeanPostProcessor
}