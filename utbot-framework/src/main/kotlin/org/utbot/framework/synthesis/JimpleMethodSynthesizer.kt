package org.utbot.framework.synthesis

import org.utbot.engine.assignStmt
import org.utbot.engine.createSootMethod
import org.utbot.engine.identityStmt
import org.utbot.engine.newExpr
import org.utbot.engine.parameterRef
import org.utbot.engine.pureJavaSignature
import org.utbot.engine.returnStatement
import org.utbot.engine.toGraphBody
import org.utbot.engine.toInvokeStmt
import org.utbot.engine.toSpecialInvoke
import org.utbot.engine.toVirtualInvoke
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.synthesis.postcondition.constructors.toSoot
import org.utbot.framework.synthesis.postcondition.constructors.toSootType
import java.util.IdentityHashMap
import soot.RefType
import soot.SootClass
import soot.SootMethod
import soot.Type
import soot.jimple.IdentityStmt
import soot.jimple.JimpleBody
import soot.jimple.Stmt
import soot.jimple.internal.JimpleLocal

data class SynthesisParameter(
    val type: Type,
    val number: Int
)

class JimpleMethodSynthesizer {
    fun synthesize(unit: SynthesisUnit): SynthesisContext {
        System.err.println("Synthesizing JimpleMethod...")

        return SynthesisContext(unit)
    }

    class SynthesisContext(
        private val rootUnit: SynthesisUnit
    ) {
        private var localCounter = 0
        private fun nextName() = "\$r${localCounter++}"

        private var parameterCount = 0
        private fun nextParameterCount() = parameterCount++

        private val identities = mutableListOf<IdentityStmt>()
        private val parameters_ = mutableListOf<SynthesisParameter>()
        private val stmts = mutableListOf<Stmt>()

        val parameters: List<SynthesisParameter> by ::parameters_
        val returnType: Type
        val body: JimpleBody

        val unitToParameter = IdentityHashMap<SynthesisUnit, SynthesisParameter>()

        init {
            val local = synthesizeUnit(rootUnit)
            returnType = local.type

            val returnStmt = returnStatement(local)

            body = (identities + stmts + returnStmt).toGraphBody()
        }

        fun method(name: String, declaringClass: SootClass): SootMethod {
            val parameterTypes = parameters.map { it.type }

            return createSootMethod(name, parameterTypes, returnType, declaringClass, body, isStatic = true).also {
                System.err.println("Done!")
            }
        }

        fun resolve(parameterModels: List<UtModel>): UtModel {
            val resolver = Resolver(parameterModels, listOf(), unitToParameter)
            return resolver.resolve(rootUnit)
        }

        private fun synthesizeUnit(unit: SynthesisUnit): JimpleLocal = when (unit) {
            is ObjectUnit -> synthesizeCompositeUnit(unit)
            is MethodUnit -> synthesizeMethodUnit(unit)
            else -> TODO()
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
                    this is MethodId && isStatic -> TODO()
                    this is MethodId -> synthesizeVirtualInvoke(this, parameterLocals)
                    else -> TODO()
                }
            }
            return result
        }

        private fun synthesizeVirtualInvoke(method: MethodId, parameterLocals: List<JimpleLocal>): JimpleLocal {
            val local = parameterLocals.firstOrNull() ?: error("No this parameter found for $method")
            val parametersWithoutThis = parameterLocals.drop(1)

            val sootMethod = method.classId.toSoot().methods.first { it.pureJavaSignature == method.signature }
            val invokeStmt = sootMethod.toVirtualInvoke(local, parametersWithoutThis).toInvokeStmt()

            stmts += invokeStmt

            return local
        }

        private fun synthesizeConstructorInvoke(method: ConstructorId, parameterLocals: List<JimpleLocal>): JimpleLocal {
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

/*
        private fun synthesizePrimitiveUnit(unit: ObjectUnit): JimpleLocal {

        }
*/
    }
}