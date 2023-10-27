package org.utbot.contest.usvm.executor

import io.ksmt.utils.asExpr
import kotlinx.coroutines.runBlocking
import org.jacodb.api.*
import org.jacodb.api.ext.*
import org.jacodb.impl.fs.BuildFolderLocation
import org.jacodb.impl.fs.JarLocation
import org.usvm.*
import org.usvm.api.JcCoverage
import org.usvm.api.JcTest
import org.usvm.api.typeStreamOf
import org.usvm.collection.array.UArrayIndexLValue
import org.usvm.collection.array.length.UArrayLengthLValue
import org.usvm.collection.field.UFieldLValue
import org.usvm.instrumentation.executor.UTestConcreteExecutor
import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.*
import org.usvm.machine.JcContext
import org.usvm.machine.extractBool
import org.usvm.machine.extractByte
import org.usvm.machine.extractChar
import org.usvm.machine.extractDouble
import org.usvm.machine.extractFloat
import org.usvm.machine.extractInt
import org.usvm.machine.extractLong
import org.usvm.machine.extractShort
import org.usvm.machine.state.JcState
import org.usvm.machine.state.localIdx
import org.usvm.memory.ULValue
import org.usvm.memory.UReadOnlyMemory
import org.usvm.memory.URegisterStackLValue
import org.usvm.model.UModelBase
import org.usvm.types.first
import org.usvm.types.firstOrNull

/**
 * A class, responsible for resolving a single [JcExecution] for a specific method from a symbolic state.
 *
 * Uses reflection to resolve objects.
 */
// TODO usvm-sbft-refactoring: copied from `usvm/usvm-jvm/test`, extract this class back to USVM project
class JcTestExecutor(
    val classpath: JcClasspath,
) {

    private val runner: UTestConcreteExecutor
        get() {
            if (!UTestRunner.isInitialized()) {
                val pathToJars =
                    classpath.locations
                        .filter { it is BuildFolderLocation || (it is JarLocation && it.type == LocationType.APP) }
                        .map { it.path }
                UTestRunner.initRunner(pathToJars, classpath)
            }
            return UTestRunner.runner
        }

    /**
     * Resolves a [JcTest] from a [method] from a [state].
     */
    fun resolve(method: JcTypedMethod, state: JcState): JcExecution {
        val model = state.models.first()

        val ctx = state.ctx

        val memoryScope = MemoryScope(ctx, model, model, method)

        val uTest = memoryScope.createUTest()

        val execResult = runBlocking {
            runner.executeAsync(uTest)
        }

        val coverage = resolveCoverage(method, state)

        return JcExecution(method, uTest, execResult, coverage)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun resolveCoverage(method: JcTypedMethod, state: JcState): JcCoverage {
        // TODO: extract coverage
        return JcCoverage(emptyMap())
    }

    /**
     * An actual class for resolving objects from [UExpr]s.
     *
     * @param model a model to which compose expressions.
     * @param memory a read-only memory to read [ULValue]s from.
     */
    private class MemoryScope(
        private val ctx: JcContext,
        private val model: UModelBase<JcType>,
        private val memory: UReadOnlyMemory<JcType>,
        private val method: JcTypedMethod,
    ) {

        private val resolvedCache = mutableMapOf<UConcreteHeapAddress, Pair<UTestExpression, List<UTestInst>>>()

        fun createUTest(): UTest {
            val thisInstance = if (!method.isStatic) {
                val ref = URegisterStackLValue(ctx.addressSort, idx = 0)
                resolveLValue(ref, method.enclosingType)
            } else {
                UTestNullExpression(ctx.cp.objectType) to listOf()
            }

            val parameters = method.parameters.mapIndexed { idx, param ->
                val registerIdx = method.method.localIdx(idx)
                val ref = URegisterStackLValue(ctx.typeToSort(param.type), registerIdx)
                resolveLValue(ref, param.type)
            }

            val initStmts = thisInstance.second + parameters.flatMap { it.second }
            val callExpr = if (method.isStatic) {
                UTestStaticMethodCall(method.method, parameters.map { it.first })
            } else {
                UTestMethodCall(thisInstance.first, method.method, parameters.map { it.first })
            }
            return UTest(initStmts, callExpr)
        }


        fun resolveLValue(lvalue: ULValue<*, *>, type: JcType): Pair<UTestExpression, List<UTestInst>> =
            resolveExpr(memory.read(lvalue), type)


        fun resolveExpr(expr: UExpr<out USort>, type: JcType): Pair<UTestExpression, List<UTestInst>> =
            when (type) {
                is JcPrimitiveType -> resolvePrimitive(expr, type)
                is JcRefType -> resolveReference(expr.asExpr(ctx.addressSort), type)
                else -> error("Unexpected type: $type")
            }

        fun resolvePrimitive(
            expr: UExpr<out USort>, type: JcPrimitiveType
        ): Pair<UTestExpression, List<UTestInst>> {
            val exprInModel = evaluateInModel(expr)
            return when (type) {
                ctx.cp.boolean -> UTestBooleanExpression(extractBool(exprInModel) ?: false, ctx.cp.boolean)
                ctx.cp.short -> UTestShortExpression(extractShort(exprInModel) ?: 0, ctx.cp.short)
                ctx.cp.int -> UTestIntExpression(extractInt(exprInModel) ?: 0, ctx.cp.int)
                ctx.cp.long -> UTestLongExpression(extractLong(exprInModel) ?: 0L, ctx.cp.long)
                ctx.cp.float -> UTestFloatExpression(extractFloat(exprInModel) ?: 0.0f, ctx.cp.float)
                ctx.cp.double -> UTestDoubleExpression(extractDouble(exprInModel) ?: 0.0, ctx.cp.double)
                ctx.cp.byte -> UTestByteExpression(extractByte(exprInModel) ?: 0, ctx.cp.byte)
                ctx.cp.char -> UTestCharExpression(extractChar(exprInModel) ?: '\u0000', ctx.cp.char)
                ctx.cp.void -> UTestNullExpression(ctx.cp.void)
                else -> error("Unexpected type: ${type.typeName}")
            }.let { it to listOf() }
        }

        fun resolveReference(heapRef: UHeapRef, type: JcRefType): Pair<UTestExpression, List<UTestInst>> {
            val ref = evaluateInModel(heapRef) as UConcreteHeapRef
            if (ref.address == NULL_ADDRESS) {
                return UTestNullExpression(type) to listOf()
            }
            // to find a type, we need to understand the source of the object
            val typeStream = if (ref.address <= INITIAL_INPUT_ADDRESS) {
                // input object
                model.typeStreamOf(ref)
            } else {
                // allocated object
                memory.typeStreamOf(ref)
            }.filterBySupertype(type)

            // We filter allocated object type stream, because it could be stored in the input array,
            // which resolved to a wrong type, since we do not build connections between element types
            // and array types right now.
            // In such cases, we need to resolve this element to null.

            val evaluatedType = typeStream.firstOrNull() ?: return UTestNullExpression(type) to listOf()

            // We check for the type stream emptiness firsly and only then for the resolved cache,
            // because even if the object is already resolved, it could be incompatible with the [type], if it
            // is an element of an array of the wrong type.

            return resolvedCache.getOrElse(ref.address) {
                when (evaluatedType) {
                    is JcArrayType -> resolveArray(ref, heapRef, evaluatedType)
                    is JcClassType -> resolveObject(ref, heapRef, evaluatedType)
                    else -> error("Unexpected type: $type")
                }
            }
        }

        private fun resolveArray(
            ref: UConcreteHeapRef, heapRef: UHeapRef, type: JcArrayType
        ): Pair<UTestExpression, List<UTestInst>> {
            val arrayDescriptor = ctx.arrayDescriptorOf(type)
            val lengthRef = UArrayLengthLValue(heapRef, arrayDescriptor, ctx.sizeSort)
            val resolvedLength = resolveLValue(lengthRef, ctx.cp.int).first as UTestIntExpression
            // TODO hack
            val length =
                if (resolvedLength.value in 0..10_000) {
                    resolvedLength
                } else {
                    UTestIntExpression(0, ctx.cp.int)
                }

            val cellSort = ctx.typeToSort(type.elementType)

            fun resolveElement(idx: Int): Pair<UTestExpression, List<UTestInst>> {
                val elemRef = UArrayIndexLValue(cellSort, heapRef, ctx.mkBv(idx), arrayDescriptor)
                return resolveLValue(elemRef, type.elementType)
            }

            //val arrLength = UTestIntExpression(length, ctx.cp.int)
            val arrayInstance = UTestCreateArrayExpression(type.elementType, length)

            val arraySetters = buildList {
                for (i in 0 until length.value) {
                    with(resolveElement(i)) {
                        add(UTestArraySetStatement(arrayInstance, UTestIntExpression(i, ctx.cp.int), first))
                        addAll(second)
                    }
                }
            }

            resolvedCache[ref.address] = arrayInstance to arraySetters
            return arrayInstance to arraySetters
        }

        private fun resolveObject(
            ref: UConcreteHeapRef, heapRef: UHeapRef, type: JcRefType
        ): Pair<UTestExpression, List<UTestInst>> {

            if (type.jcClass == ctx.classType.jcClass && ref.address <= INITIAL_STATIC_ADDRESS) {
                // Note that non-negative addresses are possible only for the result value.
                return resolveAllocatedClass(ref)
            }

            if (type.jcClass == ctx.stringType.jcClass && ref.address <= INITIAL_STATIC_ADDRESS) {
                // Note that non-negative addresses are possible only for the result value.
                return resolveAllocatedString(ref)
            }

            val anyEnumAncestor = type.getEnumAncestorOrNull()
            if (anyEnumAncestor != null) {
                return resolveEnumValue(heapRef, anyEnumAncestor)
            }


            val exprs = mutableListOf<UTestExpression>()
            val instance = UTestAllocateMemoryCall(type.jcClass)

            val fieldSetters = mutableListOf<UTestInst>()
            resolvedCache[ref.address] = instance to fieldSetters

            exprs.add(instance)

            val fields =
                generateSequence(type.jcClass) { it.superClass }
                    .map { it.toType() }
                    .flatMap { it.declaredFields }
                    .filter { !it.isStatic }

            for (field in fields) {
                val lvalue = UFieldLValue(ctx.typeToSort(field.fieldType), heapRef, field.field)
                val fieldValue = resolveLValue(lvalue, field.fieldType)
                val uTestSetFieldStmt = UTestSetFieldStatement(instance, field.field, fieldValue.first)
                fieldSetters.addAll(fieldValue.second)
                fieldSetters.add(uTestSetFieldStmt)
            }
            return instance to fieldSetters
        }

        private fun resolveEnumValue(
            heapRef: UHeapRef,
            enumAncestor: JcClassOrInterface
        ): Pair<UTestExpression, List<UTestInst>> {
            with(ctx) {
                val ordinalLValue = UFieldLValue(sizeSort, heapRef, enumOrdinalField)
                val ordinalFieldValue = resolveLValue(ordinalLValue, cp.int).first as UTestIntExpression
                val enumField = enumAncestor.enumValues?.get(ordinalFieldValue.value)
                    ?: error("Cant find enum field with index ${ordinalFieldValue.value}")

                return UTestGetStaticFieldExpression(enumField) to listOf()
            }
        }

        private fun resolveAllocatedClass(ref: UConcreteHeapRef): Pair<UTestExpression, List<UTestInst>> {
            val classTypeField = ctx.classTypeSyntheticField
            val classTypeLValue = UFieldLValue(ctx.addressSort, ref, classTypeField)
            val classTypeRef = memory.read(classTypeLValue) as? UConcreteHeapRef
                ?: error("No type for allocated class")

            val classType = memory.typeStreamOf(classTypeRef).first()
            return UTestClassExpression(classType) to listOf()
        }

        private fun resolveAllocatedString(ref: UConcreteHeapRef): Pair<UTestExpression, List<UTestInst>> {
            val valueField = ctx.stringValueField
            val strValueLValue = UFieldLValue(ctx.typeToSort(valueField.fieldType), ref, valueField.field)
            return resolveLValue(strValueLValue, valueField.fieldType)
        }

        /**
         * If we resolve state after, [expr] is read from a state memory, so it requires concretization via [model].
         *
         * @return a concretized expression.
         */
        private fun <T : USort> evaluateInModel(expr: UExpr<T>): UExpr<T> {
            return model.eval(expr)
        }

        // TODO simple org.jacodb.api.ext.JcClasses.isEnum does not work with enums with abstract methods
        private fun JcRefType.getEnumAncestorOrNull(): JcClassOrInterface? =
            (sequenceOf(jcClass) + jcClass.allSuperHierarchySequence).firstOrNull { it.isEnum }
    }

}