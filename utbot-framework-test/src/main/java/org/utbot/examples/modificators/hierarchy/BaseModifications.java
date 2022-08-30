package org.utbot.examples.modificators.hierarchy;

/**
 * A base class to check modifications recognition in inherited class.
 */
public abstract class BaseModifications implements InterfaceModifications {

    protected int baseField;

    public BaseModifications() {
    }

    public BaseModifications(int baseField) {
        this.baseField = baseField;
    }

    protected void setBaseField() {
        baseField = 1;
    }

    protected int setBaseFieldInChild() {
        return 0;
    }

    protected void setFieldHereAndInChild() {
        baseField = 1;
    }

    public abstract void setInChildAbstract();

    @Override
    public void write() {
        baseField = 1;
    }

    @Override
    public void writeAndModify(InheritedModifications obj) {
        InterfaceModifications.super.writeAndModify(obj);
        baseField = 1;
    }
}

