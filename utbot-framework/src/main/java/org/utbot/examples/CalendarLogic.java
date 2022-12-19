package org.utbot.examples;

import java.util.GregorianCalendar;

import static java.util.Calendar.*;

/* Application logic */
public class CalendarLogic {
    // Returns true iff cal is in a leap year
    public static boolean isLeapYear(GregorianCalendar cal) {
        int year = cal.get(YEAR);
        if (year % 4 == 0) {
            return year % 100 != 0;
        }
        return false;
    }

    // Returns either of -1, 0, 1 depending on whether c1 is <, =, > than c2
    public static int compare(GregorianCalendar c1, GregorianCalendar c2) {
        int cmp;
        cmp = Integer.compare(c1.get(YEAR), c2.get(YEAR));
        if (cmp == 0) {
            cmp = Integer.compare(c1.get(MONTH), c2.get(MONTH));
            if (cmp == 0) {
                cmp = Integer.compare(c1.get(DAY_OF_MONTH), c2.get(DAY_OF_MONTH));
                if (cmp == 0) {
                    cmp = Integer.compare(c1.get(HOUR), c2.get(HOUR));
                    if (cmp == 0) {
                        cmp = Integer.compare(c1.get(MINUTE), c2.get(MINUTE));
                        if (cmp == 0) {
                            cmp = Integer.compare(c1.get(SECOND), c2.get(SECOND));
                            if (cmp == 0) {
                                cmp = Integer.compare(c1.get(MILLISECOND), c2.get(MILLISECOND));
                            }
                        }
                    }
                }
            }
        }
        return cmp;
    }
}