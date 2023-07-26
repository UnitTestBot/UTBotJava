package org.utbot.instrumentation.instrumentation.transformation.adapters

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.properties.Delegates

/**
 * Class for transforming an if statement with call of the [String.equals], [String.startsWith] or [String.endsWith] method
 * with a constant string into a sequence of comparisons of each char of the string with each char of the constant string.
 */
class StringMethodsAdapter(
    api: Int,
    access: Int,
    descriptor: String?,
    methodVisitor: MethodVisitor
) : PatternMethodAdapter(api, access, descriptor, methodVisitor) {
    private enum class State {
        SEEN_NOTHING,
        SEEN_ALOAD,
        SEEN_LDC_STRING_CONST,
        SEEN_INVOKEVIRTUAL_STRING_EQUALS,
        SEEN_INVOKEVIRTUAL_STRING_STARTSWITH,
        SEEN_INVOKEVIRTUAL_STRING_ENDSWITH
    }

    private var state: State = State.SEEN_NOTHING

    private var indexOfLocalVariable by Delegates.notNull<Int>()
    private lateinit var constString: String

    override fun resetState() {
        when (state) {
            State.SEEN_ALOAD -> mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
            State.SEEN_LDC_STRING_CONST -> {
                mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                mv.visitLdcInsn(constString)
            }

            State.SEEN_INVOKEVIRTUAL_STRING_EQUALS -> {
                mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                mv.visitLdcInsn(constString)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false)
            }

            State.SEEN_INVOKEVIRTUAL_STRING_STARTSWITH -> {
                mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                mv.visitLdcInsn(constString)
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "startsWith",
                    "(Ljava/lang/String;)Z",
                    false
                )
            }

            State.SEEN_INVOKEVIRTUAL_STRING_ENDSWITH -> {
                mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                mv.visitLdcInsn(constString)
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "endsWith",
                    "(Ljava/lang/String;)Z",
                    false
                )
            }

            else -> {}
        }
        state = State.SEEN_NOTHING
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        if (state == State.SEEN_NOTHING && opcode == Opcodes.ALOAD) {
            state = State.SEEN_ALOAD
            indexOfLocalVariable = `var`
            return
        }
        resetState()
        if (opcode == Opcodes.ALOAD) {
            state = State.SEEN_ALOAD
            indexOfLocalVariable = `var`
            return
        }
        mv.visitVarInsn(opcode, `var`)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (state == State.SEEN_LDC_STRING_CONST && !isInterface && opcode == Opcodes.INVOKEVIRTUAL && owner == "java/lang/String") {
            when {
                name == "equals" && descriptor == "(Ljava/lang/Object;)Z" -> {
                    state = State.SEEN_INVOKEVIRTUAL_STRING_EQUALS
                    return
                }

                name == "startsWith" && descriptor == "(Ljava/lang/String;)Z" -> {
                    state = State.SEEN_INVOKEVIRTUAL_STRING_STARTSWITH
                    return
                }

                name == "endsWith" && descriptor == "(Ljava/lang/String;)Z" -> {
                    state = State.SEEN_INVOKEVIRTUAL_STRING_ENDSWITH
                    return
                }
            }
        }
        resetState()
        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitLdcInsn(value: Any?) {
        if (state == State.SEEN_ALOAD && value is String) {
            state = State.SEEN_LDC_STRING_CONST
            constString = value
            return
        }
        resetState()
        mv.visitLdcInsn(value)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        if (setOf(
                State.SEEN_INVOKEVIRTUAL_STRING_EQUALS,
                State.SEEN_INVOKEVIRTUAL_STRING_STARTSWITH,
                State.SEEN_INVOKEVIRTUAL_STRING_ENDSWITH
            ).any { state == it } && opcode == Opcodes.IFEQ
        ) {
            // code transformation
            // if (str.length() == constString.length()) for equals method
            // if (str.length() >= constString.length()) for startsWith and endsWith methods
            mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
            mv.visitIntInsn(Opcodes.BIPUSH, constString.length)
            if (state == State.SEEN_INVOKEVIRTUAL_STRING_EQUALS) {
                mv.visitJumpInsn(Opcodes.IF_ICMPNE, label)
            } else {
                mv.visitJumpInsn(Opcodes.IF_ICMPLT, label)
            }

            if (constString.isEmpty()) {
                state = State.SEEN_NOTHING
                return
            }

            if (state == State.SEEN_INVOKEVIRTUAL_STRING_ENDSWITH) {
                // int length = str.length()
                mv.visitLabel(Label())
                mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
                val length = newLocal(Type.INT_TYPE)
                mv.visitVarInsn(Opcodes.ISTORE, length)

                constString.forEachIndexed { index, c ->
                    // if (str.charAt(length - (constString.length() - index)) == c)
                    mv.visitLabel(Label())
                    mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                    mv.visitVarInsn(Opcodes.ILOAD, length)
                    mv.visitIntInsn(Opcodes.BIPUSH, constString.length - index)
                    mv.visitInsn(Opcodes.ISUB)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false)
                    mv.visitIntInsn(Opcodes.BIPUSH, c.code)
                    mv.visitJumpInsn(Opcodes.IF_ICMPNE, label)
                }
            } else {
                constString.forEachIndexed { index, c ->
                    // if (str.charAt(index) == c)
                    mv.visitLabel(Label())
                    mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                    mv.visitIntInsn(Opcodes.BIPUSH, index)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false)
                    mv.visitIntInsn(Opcodes.BIPUSH, c.code)
                    mv.visitJumpInsn(Opcodes.IF_ICMPNE, label)
                }
            }
            state = State.SEEN_NOTHING
            return
        }
        resetState()
        mv.visitJumpInsn(opcode, label)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        resetState()
        mv.visitMaxs(maxStack + 1, maxLocals + 1)
    }
}