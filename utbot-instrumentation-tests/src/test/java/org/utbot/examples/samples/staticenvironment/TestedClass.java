package org.utbot.examples.samples.staticenvironment;

public class TestedClass {
    public static int x = 10;

    public static int slomayInts() {
        x++;
        MyHiddenClass.var0++;

        return x + MyHiddenClass.var0;
    }

    public static String slomayString() {
        MyHiddenClass.var1 = "ha-ha";
        return MyHiddenClass.var1;
    }
}
