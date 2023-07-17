package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    @Autowired
    private Person personOne;

    @Autowired
    private Person personTwo;


    public String join(){
        return personOne.name() + personTwo.name();
    }

    public Integer getSum() {
        return personOne.name().length() + personTwo.name().length();
    }
}
