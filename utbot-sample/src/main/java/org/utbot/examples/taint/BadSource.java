package org.utbot.examples.taint;

public class BadSource {
    public static String getEnvironment(String param) {
        return "unsafe " + param;
    }
}
