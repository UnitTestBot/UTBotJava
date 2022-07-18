package org.utbot.engine.overrides.collections;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
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
public class UtOptionalLong {
    private final boolean isPresent;
    private final long value;

    public UtOptionalLong() {
        visit(this);
        this.isPresent = false;
        this.value = 0L;
    }

    public UtOptionalLong(long value) {
        visit(this);
        this.isPresent = true;
        this.value = value;
    }

    /**
     * Auxiliary function, wrapped in OptionalWrapper class.
     *
     * @return this object with type OptionalLong.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public OptionalLong asOptional() {
        return null;
    }


    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        // assume that value is present only when this OptionalLong equals OptionalLong.empty() by reference
        assume((asOptional() == OptionalLong.empty()) == (!isPresent));

        visit(this);
    }

    public long getAsLong() {
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

    public void ifPresent(LongConsumer consumer) {
        preconditionCheck();
        if (isPresent) {
            consumer.accept(value);
        }
    }

    public long orElse(long other) {
        preconditionCheck();
        return isPresent ? value : other;
    }

    public long orElseGet(LongSupplier other) {
        preconditionCheck();
        return isPresent ? value : other.getAsLong();
    }

    public<X extends Throwable> long orElseThrow(Supplier<X> exceptionSupplier) throws X {
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

        if (!(obj instanceof OptionalLong)) {
            return false;
        }

        OptionalLong other = (OptionalLong) obj;
        return (isPresent && other.isPresent())
                ? value == other.getAsLong()
                : isPresent == other.isPresent();
    }

    @Override
    public int hashCode() {
        preconditionCheck();
        return isPresent ? Long.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        preconditionCheck();
        return isPresent
                ? String.format("OptionalLong[%s]", value)
                : "OptionalLong.empty";
    }
}
