package org.utbot.engine.overrides.collections;

import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

/**
 * Class represents hybrid implementation (java + engine instructions) of OptionalInt for {@link org.utbot.engine.Traverser}.
 * <p>
 * Should behave the same as {@link java.util.OptionalInt}.
 * @see org.utbot.engine.OptionalWrapper
 */
@SuppressWarnings("unused")
public class UtOptionalInt {
    private final boolean isPresent;
    private final int value;

    public UtOptionalInt() {
        visit(this);
        this.isPresent = false;
        this.value = 0;
    }

    public UtOptionalInt(int value) {
        visit(this);
        this.isPresent = true;
        this.value = value;
    }

    /**
     * Auxiliary function, wrapped in OptionalWrapper class.
     *
     * @return this object with type OptionalInt.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public OptionalInt asOptional() {
        return null;
    }


    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        // assume that value is present only when this OptionalInt equals OptionalInt.empty() by reference
        assume((asOptional() == OptionalInt.empty()) == (!isPresent));

        visit(this);
    }

    public int getAsInt() {
        preconditionCheck();
        if (!isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        preconditionCheck();
        return isPresent;
    }

    public void ifPresent(IntConsumer consumer) {
        preconditionCheck();
        if (isPresent) {
            consumer.accept(value);
        }
    }

    public int orElse(int other) {
        preconditionCheck();
        return isPresent ? value : other;
    }

    public int orElseGet(IntSupplier other) {
        preconditionCheck();
        return isPresent ? value : other.getAsInt();
    }

    public <X extends Throwable> int orElseThrow(Supplier<X> exceptionSupplier) throws X {
        preconditionCheck();
        if (isPresent) {
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

        if (!(obj instanceof OptionalInt)) {
            return false;
        }

        OptionalInt other = (OptionalInt) obj;
        return (isPresent && other.isPresent())
                ? value == other.getAsInt()
                : isPresent == other.isPresent();
    }

    @Override
    public int hashCode() {
        preconditionCheck();
        return isPresent ? Integer.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        preconditionCheck();
        return isPresent
                ? String.format("OptionalInt[%s]", value)
                : "OptionalInt.empty";
    }
}
