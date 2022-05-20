package org.utbot.examples.modificators.hierarchy;

/**
 * An inherited class with modifications of base and own fields,
 * overridden and own modifying methods.
 */
public class InheritedModifications extends BaseModifications {
    protected int x, y, t;

    public InheritedModifications(int y) {
        super(y);
        this.y = y;
    }

    public void setBaseField() {
        baseField = 4;
        y = 5;
    }

    public void setWithModifyingBaseCall() {
        x = 8;
        setBaseField();
    }

    public void setWithOverrideCall() {
        setBaseFieldInChild();
        x = 12;
    }

    public void setInClassAndBaseClassMethods() {
        setFieldHereAndInChild();
        super.setFieldHereAndInChild();
    }

    public void setInInterfaceMethod(InterfaceModifications object) {
        x = 13;
        object.write();
        y = 14;
    }

    public void setInStaticInterfaceMethodCall(InheritedModifications obj) {
        obj.x = 15;
        InterfaceModifications.writeAndModifyStatic(obj);
    }

    @Override
    protected int setBaseFieldInChild() {
        y = 71;
        baseField = super.setBaseFieldInChild();
        return 0;
    }

    @Override
    protected void setFieldHereAndInChild() {
        y = 72;
    }

    @Override
    public void setInChildAbstract() {
        baseField = 72;
    }
}

