package org.utbot.engine.overrides.collections;

import org.utbot.api.annotation.UtClassMock;

import java.util.Collection;

@UtClassMock(target = java.util.List.class, internalUsage = true)
public interface List<E> extends java.util.List<E> {
    static <E> java.util.List<E> of() {
        return new UtArrayList<>();
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1) {
        return new UtArrayList<>((E[]) new Object[]{e1});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4, E e5) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4, e5});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4, e5, e6});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4, e5, e6, e7});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4, e5, e6, e7, e8});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4, e5, e6, e7, e8, e9});
    }

    @SuppressWarnings("unchecked")
    static <E> java.util.List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return new UtArrayList<>((E[]) new Object[]{e1, e2, e3, e4, e5, e6, e7, e8, e9, e10});
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    static <E> java.util.List<E> of(E... elements) {
        return new UtArrayList<>(elements);
    }

    static <E> java.util.List<E> copyOf(Collection<? extends E> collection) {
        return new UtArrayList<>(collection);
    }
}
