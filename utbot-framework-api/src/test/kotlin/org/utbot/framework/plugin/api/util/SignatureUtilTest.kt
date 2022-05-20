package org.utbot.framework.plugin.api.util

import NoPackageClassSample
import java.lang.reflect.Method
import java.util.ArrayList
import kotlin.reflect.jvm.javaMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

internal class SignatureUtilTest {
    @ParameterizedTest
    @MethodSource("classSignatures")
    fun testClassJavaSignature(from: Class<*>, result: String) {
        assertEquals(result, from.bytecodeSignature()) { "${from.canonicalName} to \"$result\"" }
    }

    @ParameterizedTest
    @MethodSource("constructorSignatures")
    fun testConstructorJavaSignature(from: Class<*>, result: Set<String>) {
        val signatures = from.constructors.map { it.bytecodeSignature() }.toSet()
        assertEquals(result, signatures) { "$from constructors to $result" }
    }

    @ParameterizedTest
    @MethodSource("methodSignatures")
    fun testMethodJavaSignature(from: Method, result: String) {
        assertEquals(result, from.signature) { "$from to \"$result\"" }
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        fun classSignatures() = listOf(
                // primitives
                arguments(java.lang.Byte.TYPE, "B"),
                arguments(java.lang.Short.TYPE, "S"),
                arguments(Character.TYPE, "C"),
                arguments(Integer.TYPE, "I"),
                arguments(java.lang.Long.TYPE, "J"),
                arguments(java.lang.Float.TYPE, "F"),
                arguments(java.lang.Double.TYPE, "D"),
                arguments(java.lang.Boolean.TYPE, "Z"),

                // arrays of primitives
                arguments(ByteArray::class.java, "[B"),
                arguments(ShortArray::class.java, "[S"),
                arguments(CharArray::class.java, "[C"),
                arguments(IntArray::class.java, "[I"),
                arguments(LongArray::class.java, "[J"),
                arguments(FloatArray::class.java, "[F"),
                arguments(DoubleArray::class.java, "[D"),
                arguments(BooleanArray::class.java, "[Z"),

                // objects
                arguments("Some string".javaClass, "Ljava/lang/String;"),
                arguments(ArrayList<Int>().javaClass, "Ljava/util/ArrayList;"),
                arguments(Array<String>::class.java, "[Ljava/lang/String;"),
                arguments(NoPackageClassSample::class.java, "LNoPackageClassSample;")
        )

        @JvmStatic
        fun constructorSignatures() = listOf(
                arguments(
                        ArrayList<Int>().javaClass,
                        setOf(
                                "<init>()V",
                                "<init>(I)V",
                                "<init>(Ljava/util/Collection;)V"
                        )
                ),
                arguments(
                        SignatureUtilTestSample::class.java,
                        setOf(
                                "<init>()V",
                                "<init>(IJ)V",
                                "<init>([I[[J)V",
                                "<init>(Ljava/lang/Object;Ljava/lang/String;Ljava/util/List;)V"
                        )
                ),
                arguments(
                        NoPackageClassSample::class.java,
                        setOf(
                                "<init>()V",
                                "<init>(LNoPackageClassSample;)V",
                        )
                )
        )

        @JvmStatic
        fun methodSignatures() = listOf(
                // primitives
                arguments(SignatureUtilTestSample::oneByte.javaMethod, "oneByte(B)V"),
                arguments(SignatureUtilTestSample::allPrimitives.javaMethod, "allPrimitives(BSCIJFDZ)V"),

                // arrays
                arguments(SignatureUtilTestSample::allPrimitiveArrays.javaMethod, "allPrimitiveArrays([B[S[C[I[J[F[D[Z)V"),
                arguments(SignatureUtilTestSample::multiDimensional.javaMethod, "multiDimensional([[B[[[I[[[[J)V"),
                arguments(SignatureUtilTestSample::stringArray.javaMethod, "stringArray([Ljava/lang/String;)V"),

                // objects
                arguments(SignatureUtilTestSample::oneObject.javaMethod, "oneObject(Ljava/lang/Object;)V"),
                arguments(
                        SignatureUtilTestSample::objectsAndCollections.javaMethod,
                        "objectsAndCollections(Ljava/lang/Object;Ljava/lang/String;Ljava/util/List;)V"
                ),

                // return types
                arguments(SignatureUtilTestSample::returnsPrimitive.javaMethod, "returnsPrimitive()J"),
                arguments(SignatureUtilTestSample::returnsArray.javaMethod, "returnsArray()[[F"),
                arguments(SignatureUtilTestSample::returnsString.javaMethod, "returnsString()Ljava/lang/String;"),

                // no package class
                arguments(NoPackageClassSample::multipleNoPackageArgs.javaMethod, "multipleNoPackageArgs(LNoPackageClassSample;LNoPackageClassSample;)V"),
                arguments(NoPackageClassSample::mixedArgs.javaMethod, "mixedArgs(LNoPackageClassSample;ILjava/lang/Object;)V"),
                arguments(NoPackageClassSample::returnsNoPackageNoArgs.javaMethod, "returnsNoPackageNoArgs()LNoPackageClassSample;"),
                arguments(NoPackageClassSample::returnsNoPackage.javaMethod, "returnsNoPackage(LNoPackageClassSample;)LNoPackageClassSample;")
        )
    }
}