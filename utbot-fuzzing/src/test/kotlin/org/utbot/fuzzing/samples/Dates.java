package org.utbot.fuzzing.samples;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings({"unused", "RedundantIfStatement"})
public class Dates {
    public boolean getTime(Date date) {
        return date.getTime() == 100;
    }

    public boolean getTimeFormatted(Date date) throws ParseException {
        boolean after = new SimpleDateFormat("dd-MM-yyyy").parse("10-06-2012").after(date);
        if (after) {
            return true;
        } else {
            return false;
        }
    }
}
