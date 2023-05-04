package org.utbot.intellij.plugin.language.python.table;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {
    public static Boolean haveCommonPrefix(List<String> strings) {
        Set<String> prefixes = new HashSet<>();
        for (String str: strings) {
            prefixes.add(getPrefix(str));
        }
        return prefixes.size() <= 1;
    }

    public static String getPrefix(String str) {
        String suffix = getSuffix(str);
        int len = str.length();
        return str.substring(0, len-suffix.length()-1);
    }

    public static String getSuffix(String str) {
        String[] parts = str.split("\\.");
        int len = parts.length;
        return parts[len-1];
    }
}
