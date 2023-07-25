package org.utbot.instrumentation.instrumentation.transformation.adapters

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.properties.Delegates

/**
 * Class for transforming an if statement with call of the [String.equals] method with a constant string into
 * a sequence of comparisons of each char of the string with each char of the constant string.
 */
class StringEqualsMethodAdapter(api: Int, methodVisitor: MethodVisitor) : PatternMethodAdapter(api, methodVisitor) {
    private enum class State {
        SEEN_NOTHING,
        SEEN_ALOAD,
        SEEN_LDC_STRING_CONST,
        SEEN_INVOKEVIRTUAL_STRING_EQUALS,
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
        if (state == State.SEEN_LDC_STRING_CONST) {
            if (!isInterface && opcode == Opcodes.INVOKEVIRTUAL && owner == "java/lang/String" && name == "equals" && descriptor == "(Ljava/lang/Object;)Z") {
                state = State.SEEN_INVOKEVIRTUAL_STRING_EQUALS
                return
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
        if (state == State.SEEN_INVOKEVIRTUAL_STRING_EQUALS && opcode == Opcodes.IFEQ) {
            state = State.SEEN_NOTHING
            // code transformation
            // if (str.length() == constString.length())
            mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
            mv.visitIntInsn(Opcodes.BIPUSH, constString.length)
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, label)
            if (constString.isEmpty()) {
                return
            }
            constString.forEachIndexed { index, c ->
                // if (str.charAt(index) == c)
                val l = Label()
                mv.visitLabel(l)
                mv.visitVarInsn(Opcodes.ALOAD, indexOfLocalVariable)
                mv.visitIntInsn(Opcodes.BIPUSH, index)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false)
                mv.visitIntInsn(Opcodes.BIPUSH, c.code)
                mv.visitJumpInsn(Opcodes.IF_ICMPNE, label)
            }
            return
        }
        resetState()
        mv.visitJumpInsn(opcode, label)
    }
}