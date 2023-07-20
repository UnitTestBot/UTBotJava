package org.utbot.examples.controlflow;

import java.math.RoundingMode;

public class Switch {

    public int simpleSwitch(int x) {
        switch (x) {
            case 10:
                return 10;
            case 11: // fall-through
            case 12:
                return 12;
            case 13:
                return 13;
            default:
                return -1;
        }
    }

    public int lookupSwitch(int x) {
        switch (x) {
            case 0:
                return 0;
            case 10: // fall-through
            case 20:
                return 20;
            case 30:
                return 30;
            default:
                return -1;
        }
    }

    public int enumSwitch(RoundingMode m) {
        switch (m) {
            case HALF_DOWN: // fall-through
            case HALF_EVEN: // fall-through
            case HALF_UP: // fall-through
                return 1;
            case DOWN:
                return 2;
            case CEILING:
                return 3;
        }
        return -1;
    }

    public int charToIntSwitch(char c) {
        switch (c) {
            case 'I': return 1;
            case 'V': return 5;
            case 'X': return 10;
            case 'L': return 50;
            case 'C': return 100;
            case 'D': return 500;
            case 'M': return 1000;
            default: throw new IllegalArgumentException("Unrecognized symbol: " + c);
        }
    }

    public int throwExceptionInSwitchArgument() {
        switch (getChar()) {
            case 'I':
                return 1;
            default:
                return 100;
        }
    }

    private char getChar() throws RuntimeException {
        throw new RuntimeException("Exception message");
    }

    //TODO: String switch
//    public int stringSwitch(String s) {
//        switch (s) {
//            case "ABC":
//                return 1;
//            case "DEF": // fall-through
//            case "GHJ":
//                return 2;
//            default:
//            return -1;
//        }
//    }
}
