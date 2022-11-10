package org.utbot.examples.taint;

public class BadSink {
    private static String value;

    public static void writeIntoBd(String param) {
        value = param;
    }

    public static void onlySecondParamIsImportant(String fst, String snd) {
        System.out.println(fst);
        value = snd;
    }
}
