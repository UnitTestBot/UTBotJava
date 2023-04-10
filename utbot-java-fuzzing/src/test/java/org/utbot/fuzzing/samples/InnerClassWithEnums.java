package org.utbot.fuzzing.samples;

public class InnerClassWithEnums {
    private SampleEnum a;
    private SampleEnum b;

    public InnerClassWithEnums(SampleEnum a, SampleEnum b) {
        this.a = a;
        this.b = b;
    }

    public SampleEnum getA() {
        return a;
    }

    public void setA(SampleEnum a) {
        this.a = a;
    }

    public SampleEnum getB() {
        return b;
    }

    public void setB(SampleEnum b) {
        this.b = b;
    }
}
