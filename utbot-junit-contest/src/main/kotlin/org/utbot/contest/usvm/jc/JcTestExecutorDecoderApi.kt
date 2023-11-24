package org.utbot.contest.usvm.jc

import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcField
import org.jacodb.api.JcMethod
import org.jacodb.api.JcType
import org.jacodb.api.ext.boolean
import org.jacodb.api.ext.byte
import org.jacodb.api.ext.char
import org.jacodb.api.ext.double
import org.jacodb.api.ext.float
import org.jacodb.api.ext.int
import org.jacodb.api.ext.long
import org.jacodb.api.ext.objectType
import org.jacodb.api.ext.short
import org.jacodb.api.ext.toType
import org.usvm.api.decoder.DecoderApi
import org.usvm.instrumentation.testcase.api.UTestArrayGetExpression
import org.usvm.instrumentation.testcase.api.UTestArrayLengthExpression
import org.usvm.instrumentation.testcase.api.UTestArraySetStatement
import org.usvm.instrumentation.testcase.api.UTestBooleanExpression
import org.usvm.instrumentation.testcase.api.UTestByteExpression
import org.usvm.instrumentation.testcase.api.UTestCastExpression
import org.usvm.instrumentation.testcase.api.UTestCharExpression
import org.usvm.instrumentation.testcase.api.UTestClassExpression
import org.usvm.instrumentation.testcase.api.UTestConstructorCall
import org.usvm.instrumentation.testcase.api.UTestCreateArrayExpression
import org.usvm.instrumentation.testcase.api.UTestDoubleExpression
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestFloatExpression
import org.usvm.instrumentation.testcase.api.UTestGetFieldExpression
import org.usvm.instrumentation.testcase.api.UTestGetStaticFieldExpression
import org.usvm.instrumentation.testcase.api.UTestInst
import org.usvm.instrumentation.testcase.api.UTestIntExpression
import org.usvm.instrumentation.testcase.api.UTestLongExpression
import org.usvm.instrumentation.testcase.api.UTestMethodCall
import org.usvm.instrumentation.testcase.api.UTestNullExpression
import org.usvm.instrumentation.testcase.api.UTestSetFieldStatement
import org.usvm.instrumentation.testcase.api.UTestSetStaticFieldStatement
import org.usvm.instrumentation.testcase.api.UTestShortExpression
import org.usvm.instrumentation.testcase.api.UTestStaticMethodCall
import org.usvm.instrumentation.testcase.api.UTestStringExpression
import org.usvm.instrumentation.util.stringType
import org.usvm.machine.JcContext

// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JcTestExecutorDecoderApi(
    private val ctx: JcContext
) : DecoderApi<UTestExpression> {
    private val instructions = mutableListOf<UTestInst>()

    fun initializerInstructions(): List<UTestInst> = instructions

    override fun setField(field: JcField, instance: UTestExpression, value: UTestExpression) {
        instructions += if (field.isStatic) {
            UTestSetStaticFieldStatement(field, value)
        } else {
            UTestSetFieldStatement(instance, field, value)
        }
    }

    override fun getField(field: JcField, instance: UTestExpression): UTestExpression =
        if (field.isStatic) {
            UTestGetStaticFieldExpression(field)
        } else {
            UTestGetFieldExpression(instance, field)
        }

    override fun invokeMethod(method: JcMethod, args: List<UTestExpression>): UTestExpression =
        when {
            method.isConstructor -> UTestConstructorCall(method, args)
            method.isStatic -> UTestStaticMethodCall(method, args)
            else -> UTestMethodCall(args.first(), method, args.drop(1))
        }.also {
            instructions += it
        }

    override fun createBoolConst(value: Boolean): UTestExpression =
        UTestBooleanExpression(value, ctx.cp.boolean)

    override fun createByteConst(value: Byte): UTestExpression =
        UTestByteExpression(value, ctx.cp.byte)

    override fun createShortConst(value: Short): UTestExpression =
        UTestShortExpression(value, ctx.cp.short)

    override fun createIntConst(value: Int): UTestExpression =
        UTestIntExpression(value, ctx.cp.int)

    override fun createLongConst(value: Long): UTestExpression =
        UTestLongExpression(value, ctx.cp.long)

    override fun createFloatConst(value: Float): UTestExpression =
        UTestFloatExpression(value, ctx.cp.float)

    override fun createDoubleConst(value: Double): UTestExpression =
        UTestDoubleExpression(value, ctx.cp.double)

    override fun createCharConst(value: Char): UTestExpression =
        UTestCharExpression(value, ctx.cp.char)

    override fun createStringConst(value: String): UTestExpression =
        UTestStringExpression(value, ctx.cp.stringType())

    override fun createClassConst(type: JcType): UTestExpression =
        UTestClassExpression(type)

    override fun createNullConst(type: JcType): UTestExpression =
        UTestNullExpression(type)

    override fun setArrayIndex(array: UTestExpression, index: UTestExpression, value: UTestExpression) {
        instructions += UTestArraySetStatement(array, index, value)
    }

    override fun getArrayIndex(array: UTestExpression, index: UTestExpression): UTestExpression =
        UTestArrayGetExpression(array, index)

    override fun getArrayLength(array: UTestExpression): UTestExpression =
        UTestArrayLengthExpression(array)

    override fun createArray(elementType: JcType, size: UTestExpression): UTestExpression =
        UTestCreateArrayExpression(elementType, size)

    override fun castClass(type: JcClassOrInterface, obj: UTestExpression): UTestExpression =
        UTestCastExpression(obj, type.toType())
}
