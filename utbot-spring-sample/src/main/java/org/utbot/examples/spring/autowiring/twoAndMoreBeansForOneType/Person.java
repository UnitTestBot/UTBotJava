package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

public class Person {
    private String firstName;
    private String lastName;

    private Integer age;

    private Integer weight;

    public Person(String firstName, String secondName, Integer age, Integer weight) {
        this.firstName = firstName;
        this.lastName = secondName;
        this.age = age;
        this.weight = weight;
    }

    public Integer getAge(){
        return age;
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public Integer getWeight() {
        return weight;
    }
}