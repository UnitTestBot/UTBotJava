package org.utbot.fuzzer.baseline.generator;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 *
 */
public class TypeChooser {
    private static Type chooseType(TypeVariable typeVariable) {
        Type[] bounds = typeVariable.getBounds();
        if (bounds.length <= 0) {
            return null;
        }
        Type bound = bounds[0];
        return bound;
    }

    public static Type[] chooseTypeParameterValues(TypeVariable[] typeParameters) {
        Type[] typeValues = new Type[typeParameters.length];
        for (int i = 0; i < typeParameters.length; i++) {
            typeValues[i] = chooseType(typeParameters[i]);
        }
        return typeValues;
    }
}
