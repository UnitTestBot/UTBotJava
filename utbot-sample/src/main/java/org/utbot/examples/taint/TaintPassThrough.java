package org.utbot.examples.taint;

public class TaintPassThrough {
    public String passThroughTaintInformation(String value) {
        return value + "example";
    }

    public String concatenationWithoutSavingTaintInformation(String value) {
        return value + "example";
    }
}
