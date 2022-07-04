package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.SerializerFactory
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.serializers.JavaSerializer
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import de.javakaffee.kryoserializers.GregorianCalendarSerializer
import de.javakaffee.kryoserializers.JdkProxySerializer
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import mu.KotlinLogging
import org.objenesis.instantiator.ObjectInstantiator
import org.objenesis.strategy.StdInstantiatorStrategy
import org.utbot.framework.plugin.api.TimeoutException
import org.utbot.instrumentation.instrumentation.Instrumentation
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.InvocationHandler
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger("kryo logger")
var shouldLog = true

object JsonSubtypeRegistration {
    val instrumentationPolymorphicSubclasses = mutableListOf<PolymorphicModuleBuilder<Instrumentation<*>>.() -> Unit>()
}

var customJson = Json {
    allowStructuredMapKeys = true
    serializersModule = SerializersModule {
        polymorphic(Instrumentation::class) {
            JsonSubtypeRegistration.instrumentationPolymorphicSubclasses.forEach { it() }
        }
    }
}

val counter = AtomicInteger(0)
val success = AtomicInteger(0)
val notEqual = AtomicInteger(0)
val stackOverflow = AtomicInteger(0)

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

    private data class KotlinxTestResult(
        val serializeResult: Boolean,
        val deserializeResult: Boolean = false,
        val isEqual: Boolean = false
    )

    private fun testKotlinx(cmd: Command): KotlinxTestResult {
        val serialized: String

        try {
            serialized = customJson.encodeToString(cmd)
        }
        catch (e: StackOverflowError) {
            stackOverflow.incrementAndGet()
            return KotlinxTestResult(false)
        }
        catch (e: Throwable) {
//            logger.error(e) { "serialization failed" }
            return KotlinxTestResult(false)
        }

        val deserialized: Command

        try {
            deserialized = customJson.decodeFromString(serialized)
        } catch (e: Throwable) {
            return KotlinxTestResult(serializeResult = true, deserializeResult = false)
        }

        try {
            return KotlinxTestResult(serializeResult = true, deserializeResult = true, isEqual = cmd == deserialized)
        } catch (e: Exception) {
            return KotlinxTestResult(serializeResult = true, deserializeResult = true, isEqual = false)
        }
    }

    private fun reportKotlinx(cmd: Command) {
        if (shouldLog) {
            logger.info {
                counter.incrementAndGet()
                val (serializeResult, deserializeResult, isEqual) = testKotlinx(cmd)
                if (serializeResult && deserializeResult && isEqual)
                    success.incrementAndGet()

                if (serializeResult && deserializeResult && !isEqual)
                    notEqual.incrementAndGet()

                "Kotlinx.serialization result of $cmd:\nserialize - $serializeResult, deserialize - $deserializeResult, equality - $isEqual\n" +
                        "all - ${counter.get()}, success - ${success.get()}, notEqual - ${notEqual.get()}\n" +
                        "stackOverflow - ${stackOverflow.get()}"
            }
        }
    }

    /**
     * Kryo tries to write the [cmd] to the [temporaryBuffer].
     * If no exception occurs, the output is flushed to the [outputStream].
     *
     * If an exception occurs, rethrows it wrapped in [WritingToKryoException].
     */
    fun <T : Command> writeCommand(id: Long, cmd: T) {
        reportKotlinx(cmd)

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
    fun readCommand(): Command =
        try {
            receiveKryo.readClassAndObject(kryoInput) as Command
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
        Command::class.nestedClasses.forEach {
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