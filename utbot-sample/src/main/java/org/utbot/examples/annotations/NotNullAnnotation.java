package org.utbot.examples.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnnecessaryUnboxing")
public class NotNullAnnotation {
    @SuppressWarnings("UnnecessaryUnboxing")
    public int doesNotThrowNPE(@NotNull Integer value) {
        return value.intValue();
    }

    @SuppressWarnings({"UnnecessaryUnboxing", "ConstantConditions"})
    public int throwsNPE(@Nullable Integer value) {
        return value.intValue();
    }

    @NotNull
    private Integer notNullableReturn(Integer value) {
        return value;
    }

    @SuppressWarnings("UnnecessaryUnboxing")
    public int severalParameters(@NotNull Integer first, Integer second, @NotNull Integer third) {
        return first.intValue() + second.intValue() + third.intValue();
    }

    @SuppressWarnings("UnnecessaryUnboxing")
    public int useNotNullableValue(Integer value) {
        return notNullableReturn(value).intValue();
    }

    private Integer methodToAvoidSootOptimization(Integer value) {
        return value;
    }

    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    public Integer notNullableVariable(Integer first, Integer second, Integer third) {
        @NotNull Integer a = methodToAvoidSootOptimization(first);
        Integer b = methodToAvoidSootOptimization(second);
        @NotNull Integer c = methodToAvoidSootOptimization(third);
        if (a == null || b == null || c == null) {
            throw new NullPointerException();
        } else {
            return a + b + c;
        }
    }

    @SuppressWarnings("UnnecessaryUnboxing")
    public int notNullField(@NotNull ClassWithRefField value) {
        return value.getBoxedInt().intValue();
    }

    @SuppressWarnings("UnnecessaryUnboxing")
    public int notNullStaticField() {
        return ClassWithRefField.getStaticBoxedInt().intValue();
    }

    // Next tests are used to check correctness of the proceeding different NotNull annotations
    public int javaxValidationNotNull(@javax.validation.constraints.NotNull Integer value) {
        return value.intValue();
    }

    public int findBugsNotNull(@edu.umd.cs.findbugs.annotations.NonNull Integer value) {
        return value.intValue();
    }

    public int javaxNotNull(@javax.annotation.Nonnull Integer value) {
        return value.intValue();
    }
}
