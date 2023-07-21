package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

public class Person {
    private String firstName;
    private String lastName;

    private Integer age;

    public Person(String firstName, String secondName, Integer age) {
        this.firstName = firstName;
        this.lastName = secondName;
        this.age = age;
    }

    public Integer getAge(){
        return age;
    }

    public String getName() {
        return firstName + " " + lastName;
    }
}