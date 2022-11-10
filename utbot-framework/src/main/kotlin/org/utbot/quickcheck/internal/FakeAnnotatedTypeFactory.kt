

package org.utbot.quickcheck.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

final class FakeAnnotatedTypeFactory {
    private FakeAnnotatedTypeFactory() {
        throw new UnsupportedOperationException();
    }

    static AnnotatedType makeFrom(Class<?> clazz) {
        return clazz.isArray() ? makeArrayType(clazz) : makePlainType(clazz);
    }

    private static AnnotatedArrayType makeArrayType(Class<?> type) {
        return new FakeAnnotatedArrayType(type);
    }

    private static AnnotatedType makePlainType(Class<?> type) {
        return new FakeAnnotatedType(type);
    }

    private static final class FakeAnnotatedArrayType
        implements AnnotatedArrayType {

        private final Class<?> type;

        FakeAnnotatedArrayType(Class<?> type) {
            this.type = type;
        }

        @Override public AnnotatedType getAnnotatedGenericComponentType() {
            return makeFrom(type.getComponentType());
        }

        // Not introduced until JDK 9 -- not marking as...
        // @Override
        public AnnotatedType getAnnotatedOwnerType() {
            return null;
        }

        @Override public Type getType() {
            return type;
        }

        @Override public <T extends Annotation> T getAnnotation(
            Class<T> annotationClass) {

            return null;
        }

        @Override public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }
    }

    private static final class FakeAnnotatedType implements AnnotatedType {
        private final Class<?> type;

        FakeAnnotatedType(Class<?> type) {
            this.type = type;
        }

        @Override public Type getType() {
            return type;
        }

        @Override public <T extends Annotation> T getAnnotation(
            Class<T> annotationClass) {

            return null;
        }

        @Override public Annotation[] getAnnotations() {
            return new Annotation[0];
        }

        @Override public Annotation[] getDeclaredAnnotations() {
            return new Annotation[0];
        }
    }
}
