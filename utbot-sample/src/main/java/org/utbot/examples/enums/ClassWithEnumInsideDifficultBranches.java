package org.utbot.examples.enums;

public class ClassWithEnumInsideDifficultBranches {
    public int useEnumInDifficultIf(String s) {
        if ("TRYIF".equalsIgnoreCase(s)) {
            return foo(ManyConstantsEnum.A);
        } else {
            return foo(ManyConstantsEnum.B);
        }
    }

    private int foo(ManyConstantsEnum e) {
        if (e.equals(ManyConstantsEnum.A)) {
            return 1;
        } else {
            return 2;
        }
    }

    @SuppressWarnings("unused")
    enum ManyConstantsEnum {
        A, B, C, D, E, F, G, H, I, J, K
    }
}
