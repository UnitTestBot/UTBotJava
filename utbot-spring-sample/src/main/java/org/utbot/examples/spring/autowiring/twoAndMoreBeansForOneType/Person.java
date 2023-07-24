package org.utbot.examples.spring.autowiring.twoAndMoreBeansForOneType;

public class Person {
    private Integer age;

    private Integer weight;

    public Person(Integer age, Integer weight) {
        this.age = age;
        this.weight = weight;
    }

    public Integer getAge(){
        return age;
    }

    public Integer getWeight() {
        return weight;
    }
}