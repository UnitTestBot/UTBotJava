package org.utbot.examples.annotations;

import org.jetbrains.annotations.NotNull;

public class ClassWithRefField {
    @NotNull private final Integer boxedInt;
    @NotNull private static Integer staticBoxedInt = 1;

    @SuppressWarnings("NullableProblems")
    public ClassWithRefField(Integer boxedInt) {
        this.boxedInt = boxedInt;
    }

    @SuppressWarnings("NullableProblems")
    public static Integer getStaticBoxedInt() {
        return staticBoxedInt;
    }

    public static void setStaticBoxedInt(@NotNull Integer staticBoxedInt) {
        ClassWithRefField.staticBoxedInt = staticBoxedInt;
    }

    @SuppressWarnings("NullableProblems")
    public Integer getBoxedInt() {
        return boxedInt;
    }
}
