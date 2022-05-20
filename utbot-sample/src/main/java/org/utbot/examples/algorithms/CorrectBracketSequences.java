package org.utbot.examples.algorithms;

import java.util.ArrayList;
import java.util.List;

public class CorrectBracketSequences {
    public static boolean isOpen(char a) {
        return a == '(' || a == '{' || a == '[';
    }

    public static boolean isTheSameType(char a, char b) {
        return a == '(' && b == ')' || a == '{' && b == '}' || a == '[' && b == ']';
    }

    public static boolean isBracket(char a) {
        return isOpen(a) || a == ')' || a == '}' || a == ']';
    }

    public static boolean isCbs(List<Character> chars) {
        int index = 0;
        List<Character> queue = new ArrayList<>();

        for (char c : chars) {
            if (!isBracket(c)) {
                return false;
            }

            if (index == 0 && !isOpen(c)) {
                return false;
            }

            if (isOpen(c)) {
                queue.add(c);
                index++;
                continue;
            }

            if (isTheSameType(queue.get(index - 1), c)) {
                index--;
                continue;
            }

            return false;
        }

        return index == 0;
    }
}
