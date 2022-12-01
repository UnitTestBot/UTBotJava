package org.utbot.engine.greyboxfuzzer.quickcheck.random

import org.utbot.engine.greyboxfuzzer.quickcheck.internal.Items
import org.utbot.engine.greyboxfuzzer.quickcheck.internal.Ranges
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.Random
import java.util.concurrent.TimeUnit

/**
 * A source of randomness, fed to
 * [generators][org.utbot.quickcheck.generator.Generator]
 * so they can produce random values for property parameters.
 */
class SourceOfRandomness(delegate: Random) {
    private val delegate: Random
    private var seed: Long

    /**
     * Makes a new source of randomness.
     *
     * @param delegate a JDK source of randomness, to which the new instance
     * will delegate
     */
    init {
        seed = delegate.nextLong()
        this.delegate = delegate
        delegate.setSeed(seed)
    }

    /**
     *
     * Gives a JDK source of randomness, with the same internal state as
     * this source of randomness.
     *
     * @return a JDK "clone" of self
     */
    fun toJDKRandom(): Random {
        val bytesOut = ByteArrayOutputStream()
        try {
            ObjectOutputStream(bytesOut).use { objectOut -> objectOut.writeObject(delegate) }
        } catch (ex: IOException) {
            throw IllegalStateException(ex)
        }
        val bytesIn = ByteArrayInputStream(bytesOut.toByteArray())
        try {
            ObjectInputStream(bytesIn).use { objectIn -> return objectIn.readObject() as Random }
        } catch (ex: IOException) {
            throw IllegalStateException(ex)
        } catch (shouldNeverHappen: ClassNotFoundException) {
            throw AssertionError(shouldNeverHappen)
        }
    }

    /**
     * @return a uniformly distributed boolean value
     * @see Random.nextBoolean
     */
    fun nextBoolean(): Boolean {
        return delegate.nextBoolean()
    }

    /**
     * @param bytes a byte array to fill with random values
     * @see Random.nextBytes
     */
    fun nextBytes(bytes: ByteArray?) {
        delegate.nextBytes(bytes)
    }

    /**
     * Gives an array of a given length containing random bytes.
     *
     * @param count the desired length of the random byte array
     * @return random bytes
     * @see Random.nextBytes
     */
    fun nextBytes(count: Int): ByteArray {
        val buffer = ByteArray(count)
        delegate.nextBytes(buffer)
        return buffer
    }

    /**
     * @return a uniformly distributed random `double` value in the
     * interval `[0.0, 1.0)`
     * @see Random.nextDouble
     */
    fun nextDouble(): Double {
        return delegate.nextDouble()
    }

    /**
     * @return a uniformly distributed random `float` value in the
     * interval `[0.0, 1.0)`
     * @see Random.nextFloat
     */
    fun nextFloat(): Float {
        return delegate.nextFloat()
    }

    /**
     * @return a Gaussian-distributed random double value
     * @see Random.nextGaussian
     */
    fun nextGaussian(): Double {
        return delegate.nextGaussian()
    }

    /**
     * @return a uniformly distributed random `int` value
     * @see Random.nextInt
     */
    fun nextInt(): Int {
        return delegate.nextInt()
    }

    /**
     * @param n upper bound
     * @return a uniformly distributed random `int` value in the interval
     * `[0, n)`
     * @see Random.nextInt
     */
    fun nextInt(n: Int): Int {
        return delegate.nextInt(n)
    }

    /**
     * @return a uniformly distributed random `long` value
     * @see Random.nextLong
     */
    fun nextLong(): Long {
        return delegate.nextLong()
    }

    /**
     * @param seed value with which to seed this source of randomness
     * @see Random.setSeed
     */
    fun setSeed(seed: Long) {
        this.seed = seed
        delegate.setSeed(seed)
    }

    /**
     * @return the value used to initially seed this source of randomness
     */
    fun seed(): Long {
        return seed
    }

    /**
     * Gives a random `byte` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextByte(min: Byte, max: Byte): Byte {
        return nextLong(min.toLong(), max.toLong()).toByte()
    }

    /**
     * Gives a random `char` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextChar(min: Char, max: Char): Char {
        Ranges.checkRange(Ranges.Type.CHARACTER, min, max)
        return Char(nextLong(min.code.toLong(), max.code.toLong()).toUShort())
    }

    /**
     *
     * Gives a random `double` value in the interval
     * `[min, max)`.
     *
     *
     * This naive implementation takes a random `double` value from
     * [Random.nextDouble] and scales/shifts the value into the desired
     * interval. This may give surprising results for large ranges.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextDouble(min: Double, max: Double): Double {
        val comparison = Ranges.checkRange(Ranges.Type.FLOAT, min, max)
        return if (comparison == 0) min else min + (max - min) * nextDouble()
    }

    /**
     *
     * Gives a random `float` value in the interval
     * `[min, max)`.
     *
     *
     * This naive implementation takes a random `float` value from
     * [Random.nextFloat] and scales/shifts the value into the desired
     * interval. This may give surprising results for large ranges.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextFloat(min: Float, max: Float): Float {
        val comparison = Ranges.checkRange(Ranges.Type.FLOAT, min, max)
        return if (comparison == 0) min else min + (max - min) * nextFloat()
    }

    /**
     * Gives a random `int` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextInt(min: Int, max: Int): Int {
        return nextLong(min.toLong(), max.toLong()).toInt()
    }

    /**
     * Gives a random `long` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextLong(min: Long, max: Long): Long {
        val comparison = Ranges.checkRange(Ranges.Type.INTEGRAL, min, max)
        return if (comparison == 0) min else Ranges.choose(this, min, max)
    }

    /**
     * Gives a random `short` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextShort(min: Short, max: Short): Short {
        return nextLong(min.toLong(), max.toLong()).toShort()
    }

    /**
     * Gives a random `BigInteger` representable by the given number
     * of bits.
     *
     * @param numberOfBits the desired number of bits
     * @return a random `BigInteger`
     * @see BigInteger.BigInteger
     */
    fun nextBigInteger(numberOfBits: Int): BigInteger {
        return BigInteger(numberOfBits, delegate)
    }

    /**
     * Gives a random `Instant` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextInstant(min: Instant, max: Instant): Instant {
        val comparison = Ranges.checkRange(Ranges.Type.STRING, min, max)
        if (comparison == 0) return min
        val next = nextSecondsAndNanos(
            min.epochSecond,
            min.nano.toLong(),
            max.epochSecond,
            max.nano.toLong()
        )
        return Instant.ofEpochSecond(next[0], next[1])
    }

    /**
     * Gives a random `Duration` value, uniformly distributed across the
     * interval `[min, max]`.
     *
     * @param min lower bound of the desired interval
     * @param max upper bound of the desired interval
     * @return a random value
     */
    fun nextDuration(min: Duration, max: Duration): Duration {
        val comparison = Ranges.checkRange(Ranges.Type.STRING, min, max)
        if (comparison == 0) return min
        val next = nextSecondsAndNanos(
            min.seconds,
            min.nano.toLong(),
            max.seconds,
            max.nano.toLong()
        )
        return Duration.ofSeconds(next[0], next[1])
    }

    /**
     * Gives a random element of the given collection.
     *
     * @param <T> type of items in the collection
     * @param items a collection
     * @return a randomly chosen element from the collection
    </T> */
    fun <T> choose(items: Collection<T>): T {
        return Items.choose(items, this)
    }

    /**
     * Gives a random element of the given array.
     *
     * @param <T> type of items in the array
     * @param items an array
     * @return a randomly chosen element from the array
    </T> */
    fun <T> choose(items: Array<T>): T {
        return items[nextInt(items.size)]
    }

    /**
     * Gives a reference to the JDK-random delegate of this instance.
     * Intended for subclasses.
     *
     * @return the JDK-random delegate
     */
    protected fun delegate(): Random {
        return delegate
    }

    private fun nextSecondsAndNanos(
        minSeconds: Long,
        minNanos: Long,
        maxSeconds: Long,
        maxNanos: Long
    ): LongArray {
        val nanoMin = BigInteger.valueOf(minSeconds)
            .multiply(NANOS_PER_SECOND)
            .add(BigInteger.valueOf(minNanos))
        val nanoMax = BigInteger.valueOf(maxSeconds)
            .multiply(NANOS_PER_SECOND)
            .add(BigInteger.valueOf(maxNanos))
        val generated = Ranges.choose(this, nanoMin, nanoMax)
            .divideAndRemainder(NANOS_PER_SECOND)
        return longArrayOf(generated[0].toLong(), generated[1].toLong())
    }

    companion object {
        private val NANOS_PER_SECOND = BigInteger.valueOf(TimeUnit.SECONDS.toNanos(1))
    }
}