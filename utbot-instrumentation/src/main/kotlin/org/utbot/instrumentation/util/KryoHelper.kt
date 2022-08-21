package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.objenesis.instantiator.ObjectInstantiator
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.serializers.JavaSerializer
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy
import org.utbot.framework.plugin.api.TimeoutException
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * Helpful class for working with the kryo.
 */
class KryoHelper internal constructor(
    inputStream: InputStream,
    private val outputStream: OutputStream
) : Closeable {
    private val temporaryBuffer = ByteArrayOutputStream()

    private val kryoOutput = Output(temporaryBuffer)
    private val kryoInput = Input(inputStream)

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

            temporaryBuffer.writeTo(outputStream)
            outputStream.flush()
        } catch (e: Exception) {
            throw WritingToKryoException(e)
        } finally {
            kryoOutput.reset()
            temporaryBuffer.reset()
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
        outputStream.close()
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

        this.setOptimizedGenerics(false)
        register(TimeoutException::class.java, TimeoutExceptionSerializer())

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