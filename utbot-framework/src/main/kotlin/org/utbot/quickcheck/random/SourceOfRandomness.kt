

package org.utbot.quickcheck.random;

import org.utbot.quickcheck.internal.Items;
import org.utbot.quickcheck.internal.Ranges;

import java.io.*;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Random;

import static org.utbot.quickcheck.internal.Ranges.checkRange;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A source of randomness, fed to
 * {@linkplain org.utbot.quickcheck.generator.Generator generators}
 * so they can produce random values for property parameters.
 */
public class SourceOfRandomness {
    private static final BigInteger NANOS_PER_SECOND =
        BigInteger.valueOf(SECONDS.toNanos(1));

    private final Random delegate;

    private long seed;

    /**
     * Makes a new source of randomness.
     *
     * @param delegate a JDK source of randomness, to which the new instance
     * will delegate
     */
    
    public SourceOfRandomness(Random delegate) {
        seed = delegate.nextLong();
        this.delegate = delegate;
        delegate.setSeed(seed);
    }

    /**
     * <p>Gives a JDK source of randomness, with the same internal state as
     * this source of randomness.</p>
     *
     * @return a JDK "clone" of self
     */
    public Random toJDKRandom() {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();

        try (ObjectOutputStream objectOut = new ObjectOutputStream(bytesOut)) {
            objectOut.writeObject(delegate);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        ByteArrayInputStream bytesIn =
            new ByteArrayInputStream(bytesOut.toByteArray());

        try (ObjectInputStream objectIn = new ObjectInputStream(bytesIn)) {
            return (Random) objectIn.readObject();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (ClassNotFoundException shouldNeverHappen) {
            throw new AssertionError(shouldNeverHappen);
        }
    }

    /**
     * @return a uniformly distributed boolean value
     * @see Random#nextBoolean()
     */
    public boolean nextBoolean() {
        return delegate.nextBoolean();
    }

    /**
     * @param bytes a byte array to fill with random values
     * @see Random#nextBytes(byte[])
     */
    public void nextBytes(byte[] bytes) {
        delegate.nextBytes(bytes);
    }

    /**
     * Gives an array of a given length containing random bytes.
     *
     * @param count the desired length of the random byte array
     * @return random bytes
     * @see Random#nextBytes(byte[])
     */
    public byte[] nextBytes(int count) {
        byte[] buffer = new byte[count];
        delegate.nextBytes(buffer);
        return buffer;
    }

    /**
     * @return a uniformly distributed random {@code double} value in the
     * interval {@code [0.0, 1.0)}
     * @see Random#nextDouble()
     */
    public double nextDouble() {
        return delegate.nextDouble();
    }

    /**
     * @return a uniformly distributed random {@code float} value in the
     * interval {@code [0.0, 1.0)}
     * @see Random#nextFloat()
     */
    public float nextFloat() {
        return delegate.nextFloat();
    }

    /**
     * @return a Gaussian-distributed random double value
     * @see Random#nextGaussian()
     */
    public double nextGaussian() {
        return delegate.nextGaussian();
    }

    /**
     * @return a uniformly distributed random {@code int} value
     * @see Random#nextInt()
     */
    public int nextInt() {
        return delegate.nextInt();
    }

    /**
     * @param n upper bound
     * @return a uniformly distributed random {@code int} value in the interval
     * {@code [0, n)}
     * @see Random#nextInt(int)
     */
    public int nextInt(int n) {
        return delegate.nextInt(n);
    }

    /**
     * @return a uniformly distributed random {@code long} value
     * @see Random#nextLong()
     */
    public long nextLong() {
        return delegate.nextLong();
    }

    /**
     * @param seed value with which to seed this source of randomness
     * @see Random#setSeed(long)
     */
    public void setSeed(long seed) {
        this.seed = seed;
        delegate.setSeed(seed);
    }

    /**
     * @return the value used to initially seed this source of randomness
     */
    public long seed() {
        return seed;
    }

    /**
     * Gives a random {@code byte} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public byte nextByte(byte min, byte max) {
        return (byte) nextLong(min, max);
    }

    /**
     * Gives a random {@code char} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public char nextChar(char min, char max) {
        checkRange(Ranges.Type.CHARACTER, min, max);

        return (char) nextLong(min, max);
    }

    /**
     * <p>Gives a random {@code double} value in the interval
     * {@code [min, max)}.</p>
     *
     * <p>This naive implementation takes a random {@code double} value from
     * {@link Random#nextDouble()} and scales/shifts the value into the desired
     * interval. This may give surprising results for large ranges.</p>
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public double nextDouble(double min, double max) {
        int comparison = checkRange(Ranges.Type.FLOAT, min, max);
        return comparison == 0 ? min : min + (max - min) * nextDouble();
    }

    /**
     * <p>Gives a random {@code float} value in the interval
     * {@code [min, max)}.</p>
     *
     * <p>This naive implementation takes a random {@code float} value from
     * {@link Random#nextFloat()} and scales/shifts the value into the desired
     * interval. This may give surprising results for large ranges.</p>
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public float nextFloat(float min, float max) {
        int comparison = checkRange(Ranges.Type.FLOAT, min, max);
        return comparison == 0 ? min : min + (max - min) * nextFloat();
    }

    /**
     * Gives a random {@code int} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public int nextInt(int min, int max) {
        return (int) nextLong(min, max);
    }

    /**
     * Gives a random {@code long} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public long nextLong(long min, long max) {
        int comparison = checkRange(Ranges.Type.INTEGRAL, min, max);
        if (comparison == 0)
            return min;

        return Ranges.choose(this, min, max);
    }

    /**
     * Gives a random {@code short} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public short nextShort(short min, short max) {
        return (short) nextLong(min, max);
    }

    /**
     * Gives a random {@code BigInteger} representable by the given number
     * of bits.
     *
     * @param numberOfBits the desired number of bits
     * @return a random {@code BigInteger}
     * @see BigInteger#BigInteger(int, Random)
     */
    public BigInteger nextBigInteger(int numberOfBits) {
        return new BigInteger(numberOfBits, delegate);
    }

    /**
     * Gives a random {@code Instant} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public Instant nextInstant(Instant min, Instant max) {
        int comparison = checkRange(Ranges.Type.STRING, min, max);
        if (comparison == 0)
            return min;

        long[] next =
            nextSecondsAndNanos(
                min.getEpochSecond(),
                min.getNano(),
                max.getEpochSecond(),
                max.getNano());

        return Instant.ofEpochSecond(next[0], next[1]);
    }

    /**
     * Gives a random {@code Duration} value, uniformly distributed across the
     * interval {@code [min, max]}.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    public Duration nextDuration(Duration min, Duration max) {
        int comparison = checkRange(Ranges.Type.STRING, min, max);
        if (comparison == 0)
            return min;

        long[] next =
            nextSecondsAndNanos(
                min.getSeconds(),
                min.getNano(),
                max.getSeconds(),
                max.getNano());

        return Duration.ofSeconds(next[0], next[1]);
    }

    /**
     * Gives a random element of the given collection.
     *
     * @param <T> type of items in the collection
     * @param items a collection
     * @return a randomly chosen element from the collection
     */
    public <T> T choose(Collection<T> items) {
        return Items.choose(items, this);
    }

    /**
     * Gives a random element of the given array.
     *
     * @param <T> type of items in the array
     * @param items an array
     * @return a randomly chosen element from the array
     */
    public <T> T choose(T[] items) {
        return items[nextInt(items.length)];
    }

    /**
     * Gives a reference to the JDK-random delegate of this instance.
     * Intended for subclasses.
     *
     * @return the JDK-random delegate
     */
    protected final Random delegate() {
        return delegate;
    }

    private long[] nextSecondsAndNanos(
        long minSeconds,
        long minNanos,
        long maxSeconds,
        long maxNanos) {

        BigInteger nanoMin =
            BigInteger.valueOf(minSeconds)
                .multiply(NANOS_PER_SECOND)
                .add(BigInteger.valueOf(minNanos));
        BigInteger nanoMax =
            BigInteger.valueOf(maxSeconds)
                .multiply(NANOS_PER_SECOND)
                .add(BigInteger.valueOf(maxNanos));

        BigInteger[] generated =
            Ranges.choose(this, nanoMin, nanoMax)
                .divideAndRemainder(NANOS_PER_SECOND);

        return new long[] { generated[0].longValue(), generated[1].longValue() };
    }
}
