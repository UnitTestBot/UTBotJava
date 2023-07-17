package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

public class Person {
    private String firstName;
    private String lastName;

    public Person(String firstName, String secondName) {
        this.firstName = firstName;
        this.lastName = secondName;
    }

    public String name() {
        return firstName + " " + lastName;
    }
}