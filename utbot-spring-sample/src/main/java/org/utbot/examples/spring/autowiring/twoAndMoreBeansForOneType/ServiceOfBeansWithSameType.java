package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceOfBeansWithSameType {
    @Autowired
    private Person personOne;

    @Autowired
    private Person personTwo;

    // A method for testing both cases when the Engine produces
    //  - two models for two @Autowired fields of the same type
    //  - one model for two @Autowired fields of the same type
    public Boolean checker() {
        String name1 = personOne.getName();// shouldn't produce NPE because `personOne` is `@Autowired`
        int length = name1.length(); // can produce NPE because `Person.name` isn't `@Autowired`
        Integer age2 = personTwo.getAge(); // shouldn't produce NPE because `personTwo` is `@Autowired`
        return personOne == personTwo;
    }

    public Person getPersonOne() {
        return personOne;
    }

    public Person getPersonTwo() {
        return personTwo;
    }
}