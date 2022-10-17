package org.utbot.examples.taint;

public class BadSink {
    private static String value;

    public static void writeIntoBd(String param) {
        value = param;
    }
}
