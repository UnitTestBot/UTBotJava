package org.utbot.greyboxfuzzer.quickcheck.generator.java.util

import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorContext
import org.utbot.greyboxfuzzer.util.classIdForType
import org.utbot.framework.plugin.api.UtModel
import org.utbot.greyboxfuzzer.quickcheck.generator.GenerationStatus
import org.utbot.greyboxfuzzer.quickcheck.generator.Generator
import org.utbot.greyboxfuzzer.quickcheck.generator.GeneratorConfiguration
import org.utbot.greyboxfuzzer.quickcheck.generator.java.lang.StringGenerator
import org.utbot.greyboxfuzzer.quickcheck.random.SourceOfRandomness
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID

/**
 * Home for machinery to produce [UUID]s according to
 * [RFC 4122](http://www.ietf.org/rfc/rfc4122.txt).
 */
object RFC4122 {

    abstract class AbstractUUIDGenerator : Generator(UUID::class.java) {
        protected fun setVersion(bytes: ByteArray, mask: Byte) {
            bytes[6] = (bytes[6].toInt() and 0x0F).toByte()
            bytes[6] = (bytes[6].toInt() or mask.toInt()).toByte()
        }

        protected fun setVariant(bytes: ByteArray) {
            bytes[8] = (bytes[8].toInt() and 0x3F).toByte()
            bytes[8] = (bytes[8].toInt() or 0x80).toByte()
        }

        protected fun newUUID(bytes: ByteArray?): UUID {
            val bytesIn = ByteBuffer.wrap(bytes)
            return UUID(bytesIn.long, bytesIn.long)
        }
    }

    abstract class NameBasedUUIDGenerator(hashAlgorithmName: String?, private val versionMask: Int) :
        AbstractUUIDGenerator() {
        private val strings = StringGenerator()
        private val digest: MessageDigest
        private var namespace: Namespace? = null

        init {
            digest = MessageDigests[hashAlgorithmName]
        }

        override fun generate(
            random: SourceOfRandomness,
            status: GenerationStatus
        ): UtModel {
            digest.reset()
            val namespaces = if (namespace == null) Namespaces.URL else namespace!!.value
            digest.update(namespaces.bytes)
            digest.update(
                strings.generateValue(random, status)
                    .toByteArray(StandardCharsets.UTF_8)
            )
            val hash = digest.digest()
            setVersion(hash, versionMask.toByte())
            setVariant(hash)
            val generatedUUID = newUUID(hash)
            return generatorContext.utModelConstructor.construct(generatedUUID, classIdForType(UUID::class.java))
        }

        protected fun setNamespace(namespace: Namespace?) {
            this.namespace = namespace
        }
    }

    internal class MessageDigests private constructor() {
        init {
            throw UnsupportedOperationException()
        }

        companion object {
            operator fun get(algorithmName: String?): MessageDigest {
                return try {
                    MessageDigest.getInstance(algorithmName)
                } catch (shouldNeverHappen: NoSuchAlgorithmException) {
                    throw IllegalStateException(shouldNeverHappen)
                }
            }
        }
    }

    /**
     * Produces values of type [UUID] that are RFC 4122 Version 3
     * identifiers.
     */
    class Version3 : NameBasedUUIDGenerator("MD5", 0x30) {
        /**
         * Tells this generator to prepend the given "namespace" UUID to the
         * names it generates for UUID production.
         *
         * @param namespace a handle for a "namespace" UUID
         */
        fun configure(namespace: Namespace?) {
            setNamespace(namespace)
        }
    }

    /**
     * Produces values of type [UUID] that are RFC 4122 Version 4
     * identifiers.
     */
    class Version4 : AbstractUUIDGenerator() {
        override fun generate(
            random: SourceOfRandomness,
            status: GenerationStatus
        ): UtModel {
            val bytes = random.nextBytes(16)
            setVersion(bytes, 0x40.toByte())
            setVariant(bytes)
            val generatedUUID = newUUID(bytes)
            return generatorContext.utModelConstructor.construct(generatedUUID, classIdForType(UUID::class.java))
        }
    }

    /**
     * Produces values of type [UUID] that are RFC 4122 Version 5
     * identifiers.
     */
    class Version5 : NameBasedUUIDGenerator("SHA-1", 0x50) {
        /**
         * Tells this generator to prepend the given "namespace" UUID to the
         * names it generates for UUID production.
         *
         * @param namespace a handle for a "namespace" UUID
         */
        fun configure(namespace: Namespace?) {
            setNamespace(namespace)
        }
    }

    /**
     * Used in version 3 and version 5 UUID generation to specify a
     * "namespace" UUID for use in generation.
     */
    @Target(
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.FIELD,
        AnnotationTarget.ANNOTATION_CLASS,
        AnnotationTarget.TYPE
    )
    @Retention(AnnotationRetention.RUNTIME)
    @GeneratorConfiguration
    annotation class Namespace(
        /**
         * @return a handle on a "namespace" UUID to use in generation
         */
        val value: Namespaces = Namespaces.URL
    )

    /**
     * Well-known "namespace" UUIDs.
     */
    enum class Namespaces(difference: Int) {
        /** Fully-qualified DNS name.  */
        DNS(0x10),

        /** URL.  */
        URL(0x11),

        /** ISO object identifier.  */
        ISO_OID(0x12),

        /** X.500 distinguished name.  */
        X500_DN(0x14);

        val bytes: ByteArray

        init {
            bytes = byteArrayOf(
                0x6B, 0xA7.toByte(), 0xB8.toByte(), difference.toByte(), 0x9D.toByte(), 0xAD.toByte(),
                0x11, 0xD1.toByte(), 0x80.toByte(), 0xB4.toByte(),
                0x00, 0xC0.toByte(), 0x4F, 0xD4.toByte(), 0x30, 0xC8.toByte()
            )
        }
    }
}