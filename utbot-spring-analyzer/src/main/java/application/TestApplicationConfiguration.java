package application;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import post_processors.UtBotBeanFactoryPostProcessor;

@Configuration
public class TestApplicationConfiguration {

    @Bean
    public static BeanFactoryPostProcessor utBotBeanFactoryPostProcessor() {
        return new UtBotBeanFactoryPostProcessor();
    }
}