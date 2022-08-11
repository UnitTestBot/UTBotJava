package org.utbot.framework.synthesis

import org.utbot.engine.*
import org.utbot.framework.plugin.api.*
import org.utbot.framework.synthesis.postcondition.constructors.toSoot
import org.utbot.framework.synthesis.postcondition.constructors.toSootType
import soot.ArrayType
import soot.RefType
import soot.SootClass
import soot.SootMethod
import soot.Type
import soot.VoidType
import soot.jimple.IdentityStmt
import soot.jimple.JimpleBody
import soot.jimple.NullConstant
import soot.jimple.Stmt
import soot.jimple.internal.JimpleLocal
import java.util.*

data class SynthesisParameter(
    val type: Type,
    val number: Int
)


class SynthesisMethodContext(
    private val context: SynthesisUnitContext
) {
    private var localCounter = 0
    private fun nextName() = "\$r${localCounter++}"

    private var parameterCount = 0
    private fun nextParameterCount() = parameterCount++

    private val identities = mutableListOf<IdentityStmt>()
    private val parameters_ = mutableListOf<SynthesisParameter>()
    private val stmts = mutableListOf<Stmt>()
    private val unitToLocal_ = IdentityHashMap<SynthesisUnit, JimpleLocal>()

    val parameters: List<SynthesisParameter> by ::parameters_
    val returnType: Type = VoidType.v()
    val body: JimpleBody
    val unitToLocal: Map<SynthesisUnit, JimpleLocal> get() = unitToLocal_

    val unitToParameter = IdentityHashMap<SynthesisUnit, SynthesisParameter>()

    init {
        for (model in context.models) {
            val unit = context[model]
            val local = synthesizeUnit(unit)
            unitToLocal_[unit] = local
        }
        val returnStmt = returnVoidStatement()

        body = (identities + stmts + returnStmt).toGraphBody()
    }

    fun method(name: String, declaringClass: SootClass): SootMethod {
        val parameterTypes = parameters.map { it.type }

        return createSootMethod(name, parameterTypes, returnType, declaringClass, body, isStatic = true)
    }

    fun resolve(parameterModels: List<UtModel>): List<UtModel> {
        val resolver = Resolver(parameterModels, context, unitToParameter)
        return context.models.map { resolver.resolve(context[it]) }
    }

    private fun synthesizeUnit(unit: SynthesisUnit): JimpleLocal = when (unit) {
        is ObjectUnit -> synthesizeCompositeUnit(unit)
        is MethodUnit -> synthesizeMethodUnit(unit)
        is NullUnit -> synthesizeNullUnit(unit)
        is ElementContainingUnit -> synthesizeElementContainingUnit(unit)
        is ReferenceToUnit -> synthesizeRefUnit(unit)
    }.also {
        unitToLocal_[unit] = it
    }

    private fun synthesizeCompositeUnit(unit: SynthesisUnit): JimpleLocal {
        val sootType = unit.classId.toSootType()
        val parameterNumber = nextParameterCount()
        val parameterRef = parameterRef(sootType, parameterNumber)
        val local = JimpleLocal(nextName(), sootType)
        val identity = identityStmt(local, parameterRef)

        identities += identity
        val parameter = SynthesisParameter(sootType, parameterNumber)
        parameters_ += parameter
        unitToParameter[unit] = parameter

        return local
    }

    private fun synthesizeMethodUnit(unit: MethodUnit): JimpleLocal {
        val parameterLocals = unit.params.map { synthesizeUnit(it) }
        val result = with(unit.method) {
            when {
                this is ConstructorId -> synthesizeConstructorInvoke(this, parameterLocals)
                this is MethodId && isStatic -> synthesizeStaticInvoke(this, parameterLocals)
                this is MethodId -> synthesizeVirtualInvoke(this, parameterLocals)
                else -> TODO()
            }
        }
        return result
    }

    private fun synthesizeNullUnit(unit: NullUnit): JimpleLocal {
        val sootType = unit.classId.toSootType()
        val local = JimpleLocal(nextName(), sootType)
        stmts += assignStmt(local, NullConstant.v())
        return local
    }

    private fun synthesizeRefUnit(unit: ReferenceToUnit): JimpleLocal {
        val sootType = unit.classId.toSootType()
        val ref = unitToLocal[context[unit.reference]]!!
        val local = JimpleLocal(nextName(), sootType)
        stmts += assignStmt(local, ref)
        return local
    }

    private fun synthesizeElementContainingUnit(unit: ElementContainingUnit): JimpleLocal {
        val lengthLocal = synthesizeUnit(context[unit.length])
        val unitLocal = synthesizeCreateExpr(unit, lengthLocal)
        for ((key, value) in unit.elements) {
            val indexLocal = synthesizeUnit(context[key])
            val valueLocal = synthesizeUnit(context[value])

            synthesizeSetExpr(unit, unitLocal, indexLocal, valueLocal)
        }
        return unitLocal
    }

    private fun synthesizeCreateExpr(unit: ElementContainingUnit, lengthLocal: JimpleLocal): JimpleLocal = when (unit) {
        is ArrayUnit -> {
            val arrayType = unit.classId.toSootType() as ArrayType
            val arrayLocal = JimpleLocal(nextName(), arrayType)
            val arrayExpr = newArrayExpr(arrayType.elementType, lengthLocal)
            stmts += assignStmt(arrayLocal, arrayExpr)
            arrayLocal
        }

        is ListUnit -> synthesizeConstructorInvoke(unit.constructorId, listOf())
        is SetUnit -> synthesizeConstructorInvoke(unit.constructorId, listOf())
    }

    private fun synthesizeSetExpr(
        unit: ElementContainingUnit,
        unitLocal: JimpleLocal,
        key: JimpleLocal,
        value: JimpleLocal
    ): Any = when (unit) {
        is ArrayUnit -> {
            val arrayRef = newArrayRef(unitLocal, key)
            stmts += assignStmt(arrayRef, value)
        }

        is ListUnit -> synthesizeVirtualInvoke(unit.addId, listOf(unitLocal, value))

        is SetUnit -> synthesizeVirtualInvoke(unit.addId, listOf(unitLocal, value))
    }

    private fun synthesizeVirtualInvoke(method: MethodId, parameterLocals: List<JimpleLocal>): JimpleLocal {
        val local = parameterLocals.firstOrNull() ?: error("No this parameter found for $method")
        val parametersWithoutThis = parameterLocals.drop(1)

        val sootMethod = method.classId.toSoot().methods.first { it.pureJavaSignature == method.signature }
        val invokeStmt = when {
            sootMethod.declaringClass.isInterface -> sootMethod.toInterfaceInvoke(local, parametersWithoutThis)
            else -> sootMethod.toVirtualInvoke(local, parametersWithoutThis)
        }.toInvokeStmt()

        stmts += invokeStmt

        return local
    }
    private fun synthesizeStaticInvoke(method: MethodId, parameterLocals: List<JimpleLocal>): JimpleLocal {
        val sootMethod = method.classId.toSoot().methods.first { it.pureJavaSignature == method.signature }
        val invokeExpr = sootMethod.toStaticInvokeExpr(parameterLocals)
        val invokeResult = JimpleLocal(nextName(), sootMethod.returnType)


        stmts += assignStmt(invokeResult, invokeExpr)

        return invokeResult
    }

    private fun synthesizeConstructorInvoke(
        method: ConstructorId,
        parameterLocals: List<JimpleLocal>
    ): JimpleLocal {
        val sootType = method.classId.toSootType() as RefType
        val local = JimpleLocal(nextName(), sootType)
        val new = newExpr(sootType)
        val assignStmt = assignStmt(local, new)

        stmts += assignStmt

        val sootMethod = method.classId.toSoot().methods.first { it.pureJavaSignature == method.signature }
        val invokeStmt = sootMethod.toSpecialInvoke(local, parameterLocals).toInvokeStmt()

        stmts += invokeStmt

        return local
    }
}