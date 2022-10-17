package org.utbot.examples.taint;

public class TaintPassThrough {
    public static String passThroughTaintInformation(String value) {
        return value + "example";
    }

    public static String concatenationWithoutSavingTaintInformation(String value) {
        return value + "example";
    }
}
