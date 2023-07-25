package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {
    @Autowired
    private Person personOne;

    @Autowired
    private Person personTwo;

    public Integer ageSum(){
        return personOne.getAge() + personTwo.getAge();
    }
}