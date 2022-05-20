package org.utbot.examples.samples.jacoco;

public class MonitorUsage {
    public int simpleMonitor(int x) {
        int y;
        if (x > 0) {
            synchronized (this) {
                y = x + 2;
            }
        } else {
            y = -1;
        }
        return y > 0 ? 1 : 0;
    }
}