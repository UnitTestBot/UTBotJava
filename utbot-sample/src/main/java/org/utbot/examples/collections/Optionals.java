package org.utbot.examples.collections;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class Optionals {
    public Optional<Integer> create(Integer i) {
        if (i == null) {
            return Optional.of(i);
        } else {
            return Optional.of(i);
        }
    }

    public OptionalInt createInt(int i) {
        return OptionalInt.of(i);
    }

    public OptionalLong createLong(long l) {
        return OptionalLong.of(l);
    }

    public OptionalDouble createDouble(double d) {
        return OptionalDouble.of(d);
    }

    public Optional<Integer> createNullable(Integer i) {
        if (i == null) {
            return Optional.ofNullable(i);
        } else {
            return Optional.ofNullable(i);
        }
    }

    public Optional<Integer> createEmpty() {
        return Optional.empty();
    }

    public OptionalInt createIntEmpty() {
        return OptionalInt.empty();
    }

    public OptionalLong createLongEmpty() {
        return OptionalLong.empty();
    }

    public OptionalDouble createDoubleEmpty() {
        return OptionalDouble.empty();
    }

    // IMPORTANT: we have two branches everywhere to make sure we covered
    // both situations with present and empty value

    public Integer getValue(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return optional.get();
        }
    }

    public int getIntValue(OptionalInt optional) {
        if (optional.isPresent()) {
            return optional.getAsInt();
        } else {
            return optional.getAsInt();
        }
    }

    public long getLongValue(OptionalLong optional) {
        if (optional.isPresent()) {
            return optional.getAsLong();
        } else {
            return optional.getAsLong();
        }
    }

    public double getDoubleValue(OptionalDouble optional) {
        if (optional.isPresent()) {
            return optional.getAsDouble();
        } else {
            return optional.getAsDouble();
        }
    }

    public Integer getWithIsPresent(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }

    public Integer countIfPresent(Optional<Integer> optional) {
        int[] count = {0};

        if (!optional.isPresent()) {
            return 0;
        }

        optional.ifPresent((value) -> count[0] += value);
        return count[0];
    }

    public int countIntIfPresent(OptionalInt optional) {
        int[] count = {0};

        if (!optional.isPresent()) {
            return 0;
        }

        optional.ifPresent((value) -> count[0] += value);
        return count[0];
    }

    public long countLongIfPresent(OptionalLong optional) {
        long[] count = {0L};

        if (!optional.isPresent()) {
            return 0;
        }

        optional.ifPresent((value) -> count[0] += value);
        return count[0];
    }

    public double countDoubleIfPresent(OptionalDouble optional) {
        double[] count = {0.0};

        if (!optional.isPresent()) {
            return 0.0;
        }

        optional.ifPresent((value) -> count[0] += value);
        return count[0];
    }


    public Optional<Integer> filterLessThanZero(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.filter((value) -> value >= 0);
        } else {
            return optional.filter((value) -> value >= 0);
        }
    }

    public Optional<Integer> absNotNull(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.map((value) -> value < 0 ? -value : value);
        } else {
            return optional.map((value) -> value < 0 ? -value : value);
        }
    }

    public Optional<Integer> mapLessThanZeroToNull(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.map((value) -> value < 0 ? null : value);
        } else {
            return optional.map((value) -> value < 0 ? null : value);
        }
    }

    public Optional<Integer> flatAbsNotNull(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.flatMap((value) -> Optional.of(value < 0 ? -value : value));
        } else {
            return optional.flatMap((value) -> Optional.of(value < 0 ? -value : value));
        }
    }

    public Optional<Integer> flatMapWithNull(Optional<Integer> optional) {
        if (optional.isPresent()) {
            return optional.flatMap((value) -> {
                if (value < 0) {
                    return Optional.empty();
                } else if (value > 0) {
                    return Optional.of(value);
                } else {
                    return null;
                }
            });
        } else {
            return optional.flatMap((value) -> {
                if (value < 0) {
                    return Optional.empty();
                } else if (value > 0) {
                    return Optional.of(value);
                } else {
                    return null;
                }
            });
        }

    }

    public Integer leftOrElseRight(Optional<Integer> left, Integer right) {
        Integer result = left.orElse(right);
        if (left.isPresent()) {
            return result;
        } else {
            return result;
        }
    }

    public int leftIntOrElseRight(OptionalInt left, int right) {
        int result = left.orElse(right);
        if (left.isPresent()) {
            return result;
        } else {
            return result;
        }
    }

    public long leftLongOrElseRight(OptionalLong left, long right) {
        long result = left.orElse(right);
        if (left.isPresent()) {
            return result;
        } else {
            return result;
        }
    }

    public double leftDoubleOrElseRight(OptionalDouble left, double right) {
        double result = left.orElse(right);
        if (left.isPresent()) {
            return result;
        } else {
            return result;
        }
    }

    public Integer leftOrElseGetOne(Optional<Integer> left) {
        return left.orElseGet(() -> 1);
    }

    public int leftIntOrElseGetOne(OptionalInt left) {
        return left.orElseGet(() -> 1);
    }

    public long leftLongOrElseGetOne(OptionalLong left) {
        return left.orElseGet(() -> 1L);
    }

    public double leftDoubleOrElseGetOne(OptionalDouble left) {
        return left.orElseGet(() -> 1.0);
    }

    public Integer leftOrElseThrow(Optional<Integer> left) {
        if (left.isPresent()) {
            return left.orElseThrow(() -> new IllegalArgumentException());
        } else {
            return left.orElseThrow(() -> new IllegalArgumentException());
        }
    }

    public int leftIntOrElseThrow(OptionalInt left) {
        return left.orElseThrow(() -> new IllegalArgumentException());
    }

    public long leftLongOrElseThrow(OptionalLong left) {
        return left.orElseThrow(() -> new IllegalArgumentException());
    }

    public double leftDoubleOrElseThrow(OptionalDouble left) {
        return left.orElseThrow(() -> new IllegalArgumentException());
    }

    public boolean equalOptionals(Optional<Integer> left, Optional<Integer> right) {
        if (left.equals(right)) {
            if (left.isPresent() && right.isPresent()) {
                return true;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean equalOptionalsInt(OptionalInt left, OptionalInt right) {
        if (left.equals(right)) {
            if (left.isPresent() && right.isPresent()) {
                return true;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean equalOptionalsLong(OptionalLong left, OptionalLong right) {
        if (left.equals(right)) {
            if (left.isPresent() && right.isPresent()) {
                return true;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean equalOptionalsDouble(OptionalDouble left, OptionalDouble right) {
        if (left.equals(right)) {
            if (left.isPresent() && right.isPresent()) {
                return true;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public Optional<Integer> optionalOfPositive(int value) {
        return value > 0 ? Optional.of(value) : Optional.empty();
    }
}
