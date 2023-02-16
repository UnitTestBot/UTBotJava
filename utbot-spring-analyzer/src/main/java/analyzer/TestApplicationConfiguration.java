package analyzer;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestApplicationConfiguration {

    @Bean
    public static BeanFactoryPostProcessor utBotBeanFactoryPostProcessor() {
        return new UtBotBeanFactoryPostProcessor();
    }
}