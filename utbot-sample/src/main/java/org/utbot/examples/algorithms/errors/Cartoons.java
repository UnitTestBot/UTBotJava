package org.utbot.examples.algorithms.errors;


import org.utbot.examples.structures.Pair;
import java.util.HashSet;
import java.util.Set;

// http://codeforces.com/contest/1297/problem/B?locale=en TODO object casts required
public class Cartoons {

    boolean in(long value, long from, long to) {
        return from <= value && value <= to;
    }

    long runTest(int numberOfCartoons, Pair[] cartoonDescription) {
        if (numberOfCartoons < 1 || numberOfCartoons > 2000) {
            throw new IllegalArgumentException("Number of cartoons not in 1..2000");
        }

        if (numberOfCartoons != cartoonDescription.length) {
            throw new IllegalArgumentException("Number of cartoons doesn't match with description size");
        }

        for (int i = 0; i < numberOfCartoons; i++) {
            if (!in(cartoonDescription[i].getFirst(), 1, 1_000_000_000) ||
                    !in(cartoonDescription[i].getSecond(), 1, 1_000_000_000)) {
                throw new IllegalArgumentException("All values in description should be in 1..1_000_000_000");
            }
        }

        return process(numberOfCartoons, cartoonDescription);
    }

    private long process(int numberOfCartoons, Pair[] cartoonDescription) {
        Set<Pair> days = new HashSet<>();
        for (int j = 0; j < numberOfCartoons; j++) {
            if (j == 0) {
                days.add(cartoonDescription[j]);
                continue;
            }

            Set<Pair> setToRemove = new HashSet<>();
            long a = cartoonDescription[j].getFirst();
            long b = cartoonDescription[j].getSecond();
            for (Pair day : days) {
                if (a >= day.getFirst() && a <= day.getSecond()) {
                    setToRemove.add(day);
                    if (a > day.getFirst() && day.getFirst() != day.getSecond()) {
                        days.add(new Pair(day.getFirst(), a - 1));
                    }
                    if (b < day.getSecond() && day.getFirst() != day.getSecond()) {
                        days.add(new Pair(b + 1, day.getSecond()));
                    }
                } else if (b >= day.getFirst() && b <= day.getSecond()) {
                    setToRemove.add(day);
                    if (day.getSecond() > b && day.getFirst() != day.getSecond()) {
                        days.add(new Pair(b + 1, day.getSecond()));
                    }
                } else {
                    days.add(new Pair(a, b));
                }
            }
            days.removeAll(setToRemove);
        }
        if (!days.isEmpty()) {
            return days.iterator().next().getFirst();
        } else {
            return -1;
        }
    }
}