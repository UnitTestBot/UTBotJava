package org.utbot.examples;

import java.util.ArrayList;

public class H<T> implements I<T> {


    @Override
    public int lol2() {
        return 123;
    }

    @Override
    public int lol3() {
        return I.super.lol3();
    }

    @Override
    public void lol4() {
        System.out.println("LOL");
    }

    @Override
    public ArrayList<T> lol5(Object r) {
        return new ArrayList<T>(15);
    }

    @Override
    public int lol() {
        return 777;
    }
}
