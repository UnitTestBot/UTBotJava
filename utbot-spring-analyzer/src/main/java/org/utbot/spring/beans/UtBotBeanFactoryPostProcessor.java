package org.utbot.spring.beans;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.type.MethodMetadata;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class UtBotBeanFactoryPostProcessor implements BeanFactoryPostProcessor, PriorityOrdered {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("Started post-processing bean factory in UtBot");

        ArrayList<String> beanClassNames = new ArrayList<>();
        for (String beanDefinitionName : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanDefinitionName);

            if (beanDefinition instanceof AnnotatedBeanDefinition) {
                MethodMetadata factoryMethodMetadata = ((AnnotatedBeanDefinition) beanDefinition).getFactoryMethodMetadata();
                if (factoryMethodMetadata != null) {
                    beanClassNames.add(factoryMethodMetadata.getReturnTypeName());
                }
            } else {
                beanClassNames.add(beanFactory.getBeanDefinition(beanDefinitionName).getBeanClassName());
            }
        }

        try {
            File springBeansFile = new File("SpringBeans.txt");
            FileWriter fileWriter = new FileWriter(springBeansFile);

            Object[] distinctClassNames = beanClassNames.stream()
                    .filter(s -> s != null && !s.startsWith("org.springframework"))
                    .distinct()
                    .toArray();
            Arrays.sort(distinctClassNames);

            for (Object beanClassName : distinctClassNames) {
                fileWriter.append(beanClassName.toString());
                fileWriter.append("\n");
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (Throwable e){
            System.out.println("Storing bean information failed");
        } finally {
            System.out.println("Finished post-processing bean factory in UtBot");
            ConfigurableApplicationContext c = (ConfigurableApplicationContext)beanFactory;
            SpringApplication.exit(c);
        }
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }
}
