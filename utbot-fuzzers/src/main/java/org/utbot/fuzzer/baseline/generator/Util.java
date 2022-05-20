package org.utbot.fuzzer.baseline.generator;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 *
 */
public class Util {
    private static final Random rnd = new Random();

    public static List<String> addTabs(List<String> src, int count) {
        List<String> res = new LinkedList<String>();
        for (String line : src) {
            String tabs = "";
            for (int i = 0; i < count; i++) {
                tabs += "\t";
            }
            res.add(tabs + line);
        }
        return res;
    }

    public static int findType(TypeVariable[] typeVariables, TypeVariable findThis) {
        String typeName = findThis.getName();
        for (int i = 0; i < typeVariables.length; i++) {
            if (typeVariables[i].getName().equals(typeName)) {
                return i;
            }
        }
        return -1;
    }

    public static String getTypeName(Type t, TypeVariable[] typeVariables, Type[] typeValues) {
        String res = t.getTypeName();
        if (typeVariables != null && typeValues != null
                && typeVariables.length > 0 && typeValues.length == typeVariables.length) {
            for (int i = 0; i < typeVariables.length; i++) {
                String pattern = "\\b" + Pattern.quote(typeVariables[i].getName()) + "\\b";
                String value = typeValues[i].getTypeName();
                res = res.replaceAll(pattern, value);
            }
        }
        return res;
    }

    public static int rndRange(int min, int max) {
        return rnd.nextInt(max - min + 1) + min;
    }

    public static int getArrayDepth(Type t) {
        int count = 0;
        while ((t instanceof GenericArrayType)
                || (t instanceof Class<?> && ((Class<?>) t).isArray())) {
            count++;
            if (t instanceof GenericArrayType) {
                t = ((GenericArrayType) t).getGenericComponentType();
            } else {
                t = ((Class<?>) t).getComponentType();
            }
        }
        return count;
    }

    public static Type getArrayChildType(Type t) {
        while ((t instanceof GenericArrayType)
                || (t instanceof Class<?> && ((Class<?>) t).isArray())) {
            if (t instanceof GenericArrayType) {
                t = ((GenericArrayType) t).getGenericComponentType();
            } else {
                t = ((Class<?>) t).getComponentType();
            }
        }
        return t;
    }

    public static String repeat(String str, int count) {
        String res = "";
        for (int i = 0; i < count; i++) {
            res += str;
        }
        return res;
    }
}
