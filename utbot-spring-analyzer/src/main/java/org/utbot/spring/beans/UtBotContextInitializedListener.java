package org.utbot.spring.beans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

@Component
public class UtBotContextInitializedListener implements ApplicationListener<ApplicationEvent>, PriorityOrdered  {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("Event happened:" + event.toString());
        if (event instanceof ApplicationContextInitializedEvent) {
            //ConfigurableApplicationContext context = ((ApplicationContextInitializedEvent) event).getApplicationContext();
            //context.close();
        }
        if (event instanceof ServletWebServerInitializedEvent) {
            ConfigurableApplicationContext context = ((ServletWebServerInitializedEvent) event).getApplicationContext();
            //context.close();
            SpringApplication.exit(context);
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
