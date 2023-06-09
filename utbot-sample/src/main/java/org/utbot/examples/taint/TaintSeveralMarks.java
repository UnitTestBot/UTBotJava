package org.utbot.examples.taint;

/**
 * Config: ./utbot-sample/src/main/resources/taint/TaintSeveralMarksConfig.yaml
 */
public class TaintSeveralMarks {

    public String source1() {
        return "1";
    }

    public String source2() {
        return "2";
    }

    public String source12() {
        return "12";
    }

    public String pass1(String s) {
        return s + "1";
    }

    public String pass2(String s) {
        return s + "2";
    }


    public void cleaner1(String s) {
        //
    }

    public void cleaner2(String s) {
        //
    }

    public void sink1(String s) {
        //
    }

    public void sink2(String s) {
        //
    }

    public void sink13(String s) {
        //
    }

    public void sink123(String s) {
        //
    }

    public void sinkAll(String s) {
        //
    }

    public void bad1() {
        String s = source1();
        String sp = pass1(s);
        sink1(sp);
    }

    public void bad2() {
        String s = source2();
        String sp = pass2(s);
        sink2(sp);
    }

    public void bad13() {
        String s = source1();
        sink13(s);
    }

    public void bad123() {
        String s12 = source12();
        sink123(s12);
    }

    public void badSourceAll() {
        String s = source12();
        sink1(s);
        sink2(s);
        sinkAll(s);
    }

    public void badSinkAll() {
        String s1 = source1();
        String s2 = source2();
        sinkAll(s1);
        sinkAll(s2);
    }

    public void badWrongCleaner() {
        String s = source1();
        cleaner2(s);
        sink1(s);
    }

    public void good1() {
        String s = source1();
        cleaner1(s);
        sink1(s);
    }

    public void good2() {
        String s = source2();
        cleaner2(s);
        sink2(s);
    }

    public void good13() {
        String s = source2();
        sink13(s);
    }

    public void goodWrongSource() {
        String s = source1();
        sink2(s);
    }

    public void goodWrongSink() {
        String s = source2();
        sink1(s);
    }

    public void goodWrongPass() {
        String s = source1();
        String sp = pass2(s);
        sink1(sp);
    }
}
