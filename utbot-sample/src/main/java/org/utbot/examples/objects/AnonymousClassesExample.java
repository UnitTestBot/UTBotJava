package org.utbot.examples.objects;

public class AnonymousClassesExample {
    static final AbstractAnonymousClass staticAnonymousClass = AbstractAnonymousClass.getInstance(1);
    @SuppressWarnings("FieldMayBeFinal")
    static AbstractAnonymousClass nonFinalAnonymousStatic = AbstractAnonymousClass.getInstance(1);

    public int anonymousClassAsParam(AbstractAnonymousClass abstractAnonymousClass) {
        return abstractAnonymousClass.constValue();
    }

    public int anonymousClassAsStatic() {
        return staticAnonymousClass.constValue();
    }

    public int nonFinalAnonymousStatic() {
        return nonFinalAnonymousStatic.constValue();
    }

    public AbstractAnonymousClass anonymousClassAsResult() {
        int x = 1;
        return AbstractAnonymousClass.getInstance(x);
    }
}
