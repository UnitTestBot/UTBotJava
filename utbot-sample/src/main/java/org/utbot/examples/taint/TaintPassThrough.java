package org.utbot.examples.taint;

public class TaintPassThrough {
    public String passThroughTaintInformation(String value) {
        return value + "example";
    }

    public String passSecondParameter(String fst, String snd) {
        System.out.println(fst);
        return snd + "example";
    }

    public String passFirstParameter(String fst, String snd) {
        System.out.println(snd);
        return fst + "example";
    }

    public String concatenationWithoutSavingTaintInformation(String value) {
        return value + "example";
    }
}
