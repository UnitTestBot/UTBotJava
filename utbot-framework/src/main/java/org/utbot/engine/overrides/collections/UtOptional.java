package org.utbot.engine.overrides.collections;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

/**
 * Class represents hybrid implementation (java + engine instructions) of Optional for {@link org.utbot.engine.Traverser}.
 * <p>
 * Should behave the same as {@link java.util.Optional}.
 * @see org.utbot.engine.OptionalWrapper
 */
@SuppressWarnings("unused")
public final class UtOptional<T> {
    final T value;

    public UtOptional() {
        visit(this);
        this.value = null;
    }

    public UtOptional(T value) {
        Objects.requireNonNull(value);
        visit(this);
        this.value = value;
    }

    /**
     * Auxiliary function, wrapped in OptionalWrapper class.
     *
     * @return this object with type Optional.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<T> asOptional() {
        return null;
    }

    public void eqGenericType(T value) {
    }

    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        eqGenericType(value);
        // assume that value is null only when this Optional equals Optional.empty() by reference
        assume((asOptional() == Optional.empty()) == (value == null));

        visit(this);
    }

    public T get() {
        preconditionCheck();
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        preconditionCheck();
        return value != null;
    }

    public void ifPresent(Consumer<? super T> consumer) {
        preconditionCheck();
        if (value != null) {
            consumer.accept(value);
        }
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        preconditionCheck();
        if (!isPresent())
            return asOptional();
        else
            return predicate.test(value) ? asOptional() : Optional.empty();
    }

    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        preconditionCheck();
        if (!isPresent())
            return Optional.empty();
        else {
            return Optional.ofNullable(mapper.apply(value));
        }
    }

    public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
        Objects.requireNonNull(mapper);
        preconditionCheck();
        if (!isPresent())
            return Optional.empty();
        else {
            return Objects.requireNonNull(mapper.apply(value));
        }
    }

    public T orElse(T other) {
        preconditionCheck();
        return value != null ? value : other;
    }

    public T orElseGet(Supplier<? extends T> other) {
        preconditionCheck();
        return value != null ? value : other.get();
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        preconditionCheck();
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    @Override
    public boolean equals(Object obj) {
        preconditionCheck();
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Optional)) {
            return false;
        }

        Optional<?> other = (Optional<?>) obj;
        boolean otherIsPresent = other.isPresent();
        boolean thisIsPresent = value != null;
        return (thisIsPresent && otherIsPresent)
                ? value.equals(other.get())
                : thisIsPresent == otherIsPresent;
    }


    @Override
    public int hashCode() {
        preconditionCheck();
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        preconditionCheck();
        return value != null
                ? String.format("Optional[%s]", value)
                : "Optional.empty";
    }
}
