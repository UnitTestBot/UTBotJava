package org.utbot.fuzzing.samples;

@SuppressWarnings("All")
public class FieldSetterClass {

    public static int pubStaticField;
    public final int pubFinalField = 0;
    public int pubField;
    public int pubFieldWithSetter;
    private int prvField;
    private int prvFieldWithSetter;

    public int getPubFieldWithSetter() {
        return pubFieldWithSetter;
    }

    public void setPubFieldWithSetter(int pubFieldWithSetter) {
        this.pubFieldWithSetter = pubFieldWithSetter;
    }

    public int getPrvFieldWithSetter() {
        return prvFieldWithSetter;
    }

    public void setPrvFieldWithSetter(int prvFieldWithSetter) {
        this.prvFieldWithSetter = prvFieldWithSetter;
    }
}
