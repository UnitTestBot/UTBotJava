package org.utbot.examples.samples.transformation;

public class StringMethodsCalls {
    public static boolean equalsWithEmptyString(String strToCompare) {
        if (strToCompare.equals("")) {
            return true;
        }
        return false;
    }

    public static boolean equalsWithNotEmptyString(String strToCompare) {
        if (strToCompare.equals("abc")) {
            return true;
        }
        return false;
    }

    public static boolean startsWithWithEmptyString(String strToCompare) {
        if (strToCompare.startsWith("")) {
            return true;
        }
        return false;
    }

    public static boolean startsWithWithNotEmptyString(String strToCompare) {
        if (strToCompare.startsWith("abc")) {
            return true;
        }
        return false;
    }

    public static boolean endsWithWithEmptyString(String strToCompare) {
        if (strToCompare.endsWith("")) {
            return true;
        }
        return false;
    }

    public static boolean endsWithWithNotEmptyString(String strToCompare) {
        if (strToCompare.endsWith("abc")) {
            return true;
        }
        return false;
    }
}