package org.utbot.spring.beans;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@SpringBootApplication
public class SpringBeansAnalyzer {

    public static void main(String[] args) throws ClassNotFoundException, MalformedURLException {
        ClassLoader classLoader = new URLClassLoader(new URL[] { Path.of( args[0]).toUri().toURL() });
        Class userConfigurationClass = classLoader.loadClass(args[1]);

        SpringApplication app = new SpringApplicationBuilder(SpringBeansAnalyzer.class)
                .sources(TestApplicationConfiguration.class, userConfigurationClass)
                .listeners(new UtBotContextInitializedListener())
                .build();


       app.run();
    }

    @Configuration
    static class TestApplicationConfiguration {

        @Bean
        public static BeanFactoryPostProcessor utBotBeanFactoryPostProcessor() {
            return new UtBotBeanFactoryPostProcessor();
        }
    }
}
