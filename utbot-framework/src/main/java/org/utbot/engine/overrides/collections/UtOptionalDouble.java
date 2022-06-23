package org.utbot.engine.overrides.collections;

import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import static org.utbot.api.mock.UtMock.assume;
import static org.utbot.engine.overrides.UtOverrideMock.alreadyVisited;
import static org.utbot.engine.overrides.UtOverrideMock.visit;

/**
 * Class represents hybrid implementation (java + engine instructions) of OptionalDouble for {@link org.utbot.engine.Traverser}.
 * <p>
 * Should behave the same as {@link java.util.OptionalDouble}.
 * @see org.utbot.engine.OptionalWrapper
 */
@SuppressWarnings("unused")
public class UtOptionalDouble {
    private final boolean isPresent;
    private final double value;

    public UtOptionalDouble() {
        visit(this);
        this.isPresent = false;
        this.value = Double.NaN;
    }

    public UtOptionalDouble(double value) {
        visit(this);
        this.isPresent = true;
        this.value = value;
    }

    /**
     * Auxiliary function, wrapped in OptionalWrapper class.
     *
     * @return this object with type OptionalDouble
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public OptionalDouble asOptional() {
        return null;
    }


    public void preconditionCheck() {
        if (alreadyVisited(this)) {
            return;
        }
        // assume that value is present only when this OptionalDouble equals OptionalDouble.empty() by reference
        assume((asOptional() == OptionalDouble.empty()) == (!isPresent));

        visit(this);
    }

    public double getAsDouble() {
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

    public void ifPresent(DoubleConsumer consumer) {
        preconditionCheck();
        if (isPresent) {
            consumer.accept(value);
        }
    }

    public double orElse(double other) {
        preconditionCheck();
        return isPresent ? value : other;
    }

    public double orElseGet(DoubleSupplier other) {
        preconditionCheck();
        return isPresent ? value : other.getAsDouble();
    }

    public <X extends Throwable> double orElseThrow(Supplier<X> exceptionSupplier) throws X {
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

        if (!(obj instanceof OptionalDouble)) {
            return false;
        }

        OptionalDouble other = (OptionalDouble) obj;
        return (isPresent && other.isPresent())
                ? value == other.getAsDouble()
                : isPresent == other.isPresent();
    }

    @Override
    public int hashCode() {
        preconditionCheck();
        return isPresent ? Double.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        preconditionCheck();
        return isPresent
                ? String.format("OptionalDouble[%s]", value)
                : "OptionalDouble.empty";
    }
}
