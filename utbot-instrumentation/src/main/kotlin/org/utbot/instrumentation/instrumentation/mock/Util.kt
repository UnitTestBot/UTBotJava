package org.utbot.instrumentation.instrumentation.mock

import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.InstanceFieldInitializer
import org.utbot.instrumentation.instrumentation.instrumenter.visitors.util.StaticFieldInitializer
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * Helper class for static primitive field of specific [type].
 *
 * If [value] is not null, then field will be initialized with the given value.
 */
internal class StaticPrimitiveInitializer(
    private val internalClassName: String,
    override val name: String,
    val type: Type,
    private val value: Any? = null
) : StaticFieldInitializer {
    override val descriptor: String = type.descriptor
    override val signature: String? = null

    override fun initField(mv: MethodVisitor): MethodVisitor =
        mv.apply {
            if (value != null) {
                visitLdcInsn(value)
                visitFieldInsn(Opcodes.PUTSTATIC, internalClassName, name, descriptor)
            }
        }
}

/**
 * Helper class for initializing static array field with specific [elementType].
 *
 * if [size] is less than `0` or [size] is `null`, field will be null.
 */
internal class StaticArrayInitializer(
    private val internalClassName: String,
    override val name: String,
    private val elementType: Type,
    private val size: Int? = 0
) : StaticFieldInitializer {
    override val descriptor: String = "[${elementType.descriptor}"
    override val signature: String? = null // array does not use generics

    override fun initField(mv: MethodVisitor): MethodVisitor =
        mv.apply {
            if (size != null && size >= 0) {
                visitLdcInsn(size)
                val opcode = getOpcode(elementType.sort)
                opcode?.let { visitIntInsn(Opcodes.NEWARRAY, it) } // primitive type
                    ?: visitTypeInsn(Opcodes.ANEWARRAY, elementType.internalName) // reference type
                visitFieldInsn(Opcodes.PUTSTATIC, internalClassName, name, descriptor)
            }
        }
}

/**
 * Helper class for instance primitive field of specific [type].
 *
 * If [value] is not null, then field will be initialized with the given value.
 */
internal class InstancePrimitiveInitializer(
    private val internalClassName: String,
    override val name: String,
    val type: Type,
    private val value: Any? = null
) : InstanceFieldInitializer {
    override val descriptor: String = type.descriptor
    override val signature: String? = null

    override fun initField(mv: MethodVisitor): MethodVisitor =
        mv.apply {
            if (value != null) {
                visitLdcInsn(value)
                visitFieldInsn(Opcodes.PUTFIELD, internalClassName, name, descriptor)
            }
        }
}

/**
 * Helper class for initializing static array field with specific [elementType].
 *
 * if [size] is less than `0` or [size] is `null`, field will be null.
 */
internal class InstanceArrayInitializer(
    private val internalClassName: String,
    override val name: String,
    private val elementType: Type,
    private val size: Int? = 0
) : InstanceFieldInitializer {
    override val descriptor: String = "[${elementType.descriptor}"
    override val signature: String? = null // array does not use generics

    override fun initField(mv: MethodVisitor): MethodVisitor =
        mv.apply {
            if (size != null && size >= 0) {
                visitLdcInsn(size)
                val opcode = getOpcode(elementType.sort)
                opcode?.let { visitIntInsn(Opcodes.NEWARRAY, it) } // primitive type
                    ?: visitTypeInsn(Opcodes.ANEWARRAY, elementType.internalName) // reference type
                visitFieldInsn(Opcodes.PUTFIELD, internalClassName, name, descriptor)
            }
        }
}

private fun getOpcode(sort: Int) = when (sort) {
    Type.BOOLEAN -> Opcodes.T_BOOLEAN
    Type.CHAR -> Opcodes.T_CHAR
    Type.BYTE -> Opcodes.T_BYTE
    Type.SHORT -> Opcodes.T_SHORT
    Type.INT -> Opcodes.T_INT
    Type.FLOAT -> Opcodes.T_FLOAT
    Type.LONG -> Opcodes.T_LONG
    Type.DOUBLE -> Opcodes.T_DOUBLE
    else -> null
}