package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceOfBeansWithSameType {
    @Autowired
    private Person personOne;

    @Autowired
    private Person personTwo;

    public final List<String> baseOrders = new ArrayList<>();

    // a method for testing the case when the Engine produces one model on @Autowired variables of the same type
    public Integer ageSum(){
        return personOne.getAge() + personTwo.getAge();
    }

    // a method for testing the case when the Engine produces two models on @Autowired variables of the same type
    public Boolean checker() {
        return personOne.getName().equals("k") && personTwo.getName().length() > 5 && baseOrders.isEmpty();
    }
}