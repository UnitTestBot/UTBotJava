package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PersonService {
    @Autowired
    private Person personOne;

    @Autowired
    private Person personTwo;

    public final List<String> baseOrders = new ArrayList<>();

    // a method for testing the case when the Engine reproduces one model on @Autowired variables of the same type
    public Integer ageSum(){
        return personOne.getAge() + personTwo.getAge();
    }

    // a method for testing the case when the Engine reproduces two models on @Autowired variables of the same type
    public Integer joinInfo(){
        return personOne.getWeight() + personTwo.getAge() + baseOrders.size();
    }
}