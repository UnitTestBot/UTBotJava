package org.utbot.instrumentation.util

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.KryoException
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.serializers.JavaSerializer
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass

/**
 * This ad-hoc solution for ClassNotFoundException
 */
class JavaSerializerWrapper : JavaSerializer() {
    override fun read(kryo: Kryo, input: Input?, type: Class<*>?): Any? {
        return try {
            val graphContext = kryo.graphContext
            var objectStream = graphContext.get<Any>(this) as ObjectInputStream?
            if (objectStream == null) {
                objectStream = WrappingObjectInputStream(input, kryo)
                graphContext.put(this, objectStream)
            }
            objectStream.readObject()
        } catch (ex: java.lang.Exception) {
            throw KryoException("Error during Java deserialization.", ex)
        }
    }
}

class WrappingObjectInputStream(iss : InputStream?, private val kryo: Kryo) : ObjectInputStream(iss) {
    override fun resolveClass(type: ObjectStreamClass): Class<*>? {
        return try {
            Class.forName(type.name, false, kryo.classLoader)
        } catch (ex: ClassNotFoundException) {
            try {
                return Kryo::class.java.classLoader.loadClass(type.name)
            } catch (e: ClassNotFoundException) {
                try {
                    return super.resolveClass(type);
                } catch (e: ClassNotFoundException) {
                    throw KryoException("Class not found: " + type.name, e)
                }
            }
        }
    }
}