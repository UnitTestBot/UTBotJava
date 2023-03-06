package org.utbot.fuzzing.samples;

public class OuterClassWithEnums {
    private SampleEnum value;
    private final InnerClassWithEnums left;
    private final InnerClassWithEnums right;

    public OuterClassWithEnums(SampleEnum value, InnerClassWithEnums left, InnerClassWithEnums right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }

    public void setValue(SampleEnum value) {
        this.value = value;
    }

    public SampleEnum getA() {
        if (value == SampleEnum.LEFT && left != null) {
            return left.getA();
        } else if (value == SampleEnum.RIGHT && right != null) {
            return right.getA();
        } else {
            return null;
        }
    }

    public SampleEnum getB() {
        if (value == SampleEnum.LEFT && left != null) {
            return left.getB();
        } else if (value == SampleEnum.RIGHT && right != null) {
            return right.getB();
        } else {
            return null;
        }
    }
}
