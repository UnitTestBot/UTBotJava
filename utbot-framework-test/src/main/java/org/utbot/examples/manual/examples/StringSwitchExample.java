package org.utbot.examples.manual.examples;

public class StringSwitchExample {

    public int validate(String value, int number, int defaultValue) {
        switch (value) {
            case "one":
                return number > 1 ? number : -1;
            case "two":
                return number > 2 ? number : -2;
            case "three":
                return number > 3 ? number : -3;
            default:
                return defaultValue;
        }
    }

}
