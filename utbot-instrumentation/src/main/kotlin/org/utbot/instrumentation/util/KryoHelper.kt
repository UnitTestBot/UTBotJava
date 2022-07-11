package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.JavaSerializer
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.jetbrains.rd.framework.impl.RdSignal
import com.jetbrains.rd.util.lifetime.Lifetime
import de.javakaffee.kryoserializers.GregorianCalendarSerializer
import de.javakaffee.kryoserializers.JdkProxySerializer
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer
import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.StdInstantiatorStrategy
import org.utbot.framework.plugin.api.TimeoutException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * Helpful class for working with the kryo.
 */
class KryoHelper internal constructor(
    private val lifetime: Lifetime,
    inputSignal: RdSignal<ByteArray>,
    private val outputSignal: RdSignal<ByteArray>,
    private val shouldContinue: () -> Boolean,
    private val doLog: (() -> String) -> Unit = { _ -> }
) : Closeable {
    private val outputBuffer = ByteArrayOutputStream()
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var lastInputStream = ByteArrayInputStream(ByteArray(0))
    private val kryoInput = object : Input(1024 * 1024) {
        override fun fill(buffer: ByteArray, offset: Int, count: Int): Int {
            val result = fillInternal(buffer, offset, count)

            if (result > 0)
                return result

            val should = shouldContinue()

            doLog { "returning fill request: already - $result, shouldContinue - $should" }

            return if (should) 0 else -1
        }

        private fun fillInternal(buffer: ByteArray, offset: Int, count: Int): Int {
            var already = 0

            doLog {"fill request: offset - $offset, count - $count" }

            while (already < count) {
                val readed = lastInputStream.read(buffer, offset + already, count - already)

                doLog { "in fill readed is: $readed" }

                if (readed == -1) {
                    val lastReceived = queue.poll()
                        ?: return already

                    lastInputStream = lastReceived.inputStream()

                    continue
                } else {
                    already += readed
                }
            }

            return already
        }
    }

    init {
        inputSignal.advise(lifetime) { byteArray ->
            doLog { "received chunk: size - ${byteArray.size}, hash - ${byteArray.contentHashCode()}" }
            queue.put(byteArray)
        }
    }

    private val kryoOutput = Output(outputBuffer)

    private val sendKryo: Kryo = TunedKryo()
    private val receiveKryo: Kryo = TunedKryo()

    fun setKryoClassLoader(classLoader: ClassLoader) {
        sendKryo.classLoader = classLoader
        receiveKryo.classLoader = classLoader
    }

    fun readLong(): Long {
        return receiveKryo.readObject(kryoInput, Long::class.java)
    }

    /**
     * Kryo tries to write the [cmd] to the [temporaryBuffer].
     * If no exception occurs, the output is flushed to the [outputStream].
     *
     * If an exception occurs, rethrows it wrapped in [WritingToKryoException].
     */
    fun <T : Protocol.Command> writeCommand(id: Long, cmd: T) {
        try {
            sendKryo.writeObject(kryoOutput, id)
            sendKryo.writeClassAndObject(kryoOutput, cmd)
            kryoOutput.flush()

            lifetime.executeIfAlive { // todo shall i if not alive?
                val toSend = outputBuffer.toByteArray()

                doLog { "sending chunk: size - ${toSend.size}, hash - ${toSend.contentHashCode()}" }
                outputSignal.fire(toSend)
            }
        } catch (e: Exception) {
            throw WritingToKryoException(e)
        } finally {
            kryoOutput.reset()
            outputBuffer.reset()
        }
    }

    /**
     * Kryo tries to read a command.
     *
     * If an exception occurs, rethrows it wrapped in [ReadingFromKryoException].
     *
     * @return successfully read command.
     */
    fun readCommand(): Protocol.Command =
        try {
            receiveKryo.readClassAndObject(kryoInput) as Protocol.Command
        } catch (e: Exception) {
            throw ReadingFromKryoException(e)
        }

    override fun close() {
        kryoInput.close()
        kryoOutput.close()
    }
}

// This kryo is used to initialize collections properly.
internal class TunedKryo : Kryo() {
    init {
        this.references = true
        this.isRegistrationRequired = false

        this.instantiatorStrategy = object : StdInstantiatorStrategy() {
            // workaround for Collections as they cannot be correctly deserialized without calling constructor
            val default = DefaultInstantiatorStrategy()
            val classesBadlyDeserialized = listOf(
                java.util.Queue::class.java,
                java.util.HashSet::class.java
            )

            override fun <T : Any> newInstantiatorOf(type: Class<T>): ObjectInstantiator<T> {
                return if (classesBadlyDeserialized.any { it.isAssignableFrom(type) }) {
                    @Suppress("UNCHECKED_CAST")
                    default.newInstantiatorOf(type) as ObjectInstantiator<T>
                } else {
                    super.newInstantiatorOf(type)
                }
            }
        }

        register(GregorianCalendar::class.java, GregorianCalendarSerializer())
        register(InvocationHandler::class.java, JdkProxySerializer())
        register(TimeoutException::class.java, TimeoutExceptionSerializer())
        UnmodifiableCollectionsSerializer.registerSerializers(this)
        SynchronizedCollectionsSerializer.registerSerializers(this)

        // TODO: JIRA:1492
        addDefaultSerializer(java.lang.Throwable::class.java, JavaSerializer())

        val factory = object : SerializerFactory.FieldSerializerFactory() {}
        factory.config.ignoreSyntheticFields = true
        factory.config.serializeTransient = false
        factory.config.fieldsCanBeNull = true
        this.setDefaultSerializer(factory)

        // Registration of the classes of our protocol commands.
        Protocol::class.nestedClasses.forEach {
            register(it.java)
        }
    }

    /**
     * Specific serializer for [TimeoutException] - [JavaSerializer] is not applicable
     * because [TimeoutException] is not in class loader.
     *
     * This serializer is very simple - it just writes [TimeoutException.message]
     * because we do not need other components.
     */
    private class TimeoutExceptionSerializer : Serializer<TimeoutException>() {
        override fun write(kryo: Kryo, output: Output, value: TimeoutException) {
            output.writeString(value.message)
        }

        override fun read(kryo: Kryo?, input: Input, type: Class<out TimeoutException>?): TimeoutException =
            TimeoutException(input.readString())
    }
}