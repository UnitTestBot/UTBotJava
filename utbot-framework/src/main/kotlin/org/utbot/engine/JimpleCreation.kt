package org.utbot.engine

import soot.Local
import soot.Modifier
import soot.RefType
import soot.SootClass
import soot.SootMethod
import soot.Type
import soot.Unit
import soot.Value
import soot.jimple.AddExpr
import soot.jimple.AssignStmt
import soot.jimple.DynamicInvokeExpr
import soot.jimple.GeExpr
import soot.jimple.GotoStmt
import soot.jimple.IdentityStmt
import soot.jimple.IfStmt
import soot.jimple.IntConstant
import soot.jimple.InterfaceInvokeExpr
import soot.jimple.InvokeExpr
import soot.jimple.InvokeStmt
import soot.jimple.Jimple
import soot.jimple.JimpleBody
import soot.jimple.NewArrayExpr
import soot.jimple.NewExpr
import soot.jimple.ParameterRef
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.SpecialInvokeExpr
import soot.jimple.StaticInvokeExpr
import soot.jimple.VirtualInvokeExpr

fun SootMethod.toStaticInvokeExpr(parameters: List<Value> = emptyList()): StaticInvokeExpr =
    Jimple.v().newStaticInvokeExpr(this.makeRef(), parameters)

fun SootMethod.toSpecialInvoke(base: Local, parameters: List<Value> = emptyList()): SpecialInvokeExpr =
    Jimple.v().newSpecialInvokeExpr(base, this.makeRef(), parameters)

fun SootMethod.toVirtualInvoke(base: Local, parameters: List<Value> = emptyList()): VirtualInvokeExpr =
    Jimple.v().newVirtualInvokeExpr(base, this.makeRef(), parameters)

fun SootMethod.toInterfaceInvoke(base: Local, parameters: List<Value> = emptyList()): InterfaceInvokeExpr =
    Jimple.v().newInterfaceInvokeExpr(base, this.makeRef(), parameters)

fun InvokeExpr.toInvokeStmt(): InvokeStmt = Jimple.v().newInvokeStmt(this)

fun returnVoidStatement(): ReturnVoidStmt = Jimple.v().newReturnVoidStmt()

fun returnStatement(value: Value): ReturnStmt = Jimple.v().newReturnStmt(value)

fun parameterRef(type: Type, number: Int): ParameterRef = Jimple.v().newParameterRef(type, number)

fun identityStmt(local: Value, identityRef: Value): IdentityStmt = Jimple.v().newIdentityStmt(local, identityRef)

fun newArrayExpr(type: Type, size: Value): NewArrayExpr = Jimple.v().newNewArrayExpr(type, size)

fun newExpr(type: RefType): NewExpr = Jimple.v().newNewExpr(type)

fun assignStmt(variable: Value, rValue: Value): AssignStmt = Jimple.v().newAssignStmt(variable, rValue)

fun intConstant(value: Int): IntConstant = IntConstant.v(value)

fun addExpr(left: Value, right: Value): AddExpr = Jimple.v().newAddExpr(left, right)

fun ifStmt(condition: Value, target: Unit): IfStmt = Jimple.v().newIfStmt(condition, target)

fun geExpr(left: Value, right: Value): GeExpr = Jimple.v().newGeExpr(left, right)

fun gotoStmt(target: Unit): GotoStmt = Jimple.v().newGotoStmt(target)

fun Collection<Unit>.toGraphBody(): JimpleBody = Jimple.v().newBody().apply { units.addAll(this@toGraphBody) }

/**
 * Every SootMethod must have a declaringClass, the declaringClass must contains this method and
 * every JimpleBody must have the method it belongs to. This method creates SootMethod with
 * given parameters and connects it with the declaringClass and graphBody.
 */
fun createSootMethod(
    name: String,
    argsTypes: List<Type>,
    returnType: Type,
    declaringClass: SootClass,
    graphBody: JimpleBody,
    isStatic: Boolean = false
) = SootMethod(name, argsTypes, returnType, Modifier.STATIC.takeIf { isStatic } ?: 0)
    .also {
        it.declaringClass = declaringClass
        if (declaringClass.declaresMethod(it.subSignature)) {
            declaringClass.removeMethod(declaringClass.getMethod(it.subSignature))
        }
        declaringClass.addMethod(it)
        graphBody.method = it
        it.activeBody = graphBody
    }