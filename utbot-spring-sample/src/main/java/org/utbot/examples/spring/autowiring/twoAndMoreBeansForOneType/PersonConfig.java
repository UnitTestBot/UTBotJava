package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonConfig {
    @Bean
    public Person personOne() {
        return new Person("Eg", "or", 7, 5);
    }

    @Bean
    public Person personTwo() {
        return new Person("Kir", "ill", 6, 5);
    }
}
