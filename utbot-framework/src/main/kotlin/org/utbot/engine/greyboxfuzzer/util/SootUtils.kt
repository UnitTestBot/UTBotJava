package org.utbot.engine.greyboxfuzzer.util

import org.utbot.framework.plugin.api.util.signature
import soot.ArrayType
import soot.Hierarchy
import soot.RefType
import soot.Scene
import soot.SootClass
import soot.SootField
import soot.SootMethod
import soot.jimple.internal.JAssignStmt
import soot.jimple.internal.JCastExpr
import soot.jimple.internal.JInstanceFieldRef
import soot.jimple.internal.JInstanceOfExpr
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

fun SootClass.getImplementersOfWithChain(): List<List<SootClass>> {
    this.checkLevel(SootClass.HIERARCHY)
//    if (!this.isInterface && !this.isAbstract) {
//        throw RuntimeException("interfaced needed; got $this")
//    }
    val hierarchy = Hierarchy()
    val res = mutableListOf(mutableListOf(this))
    val queue = ArrayDeque<SootClass>()
    queue.add(this)
    while (queue.isNotEmpty()) {
        val curSootClass = queue.removeFirst()
        val implementers =
            if (curSootClass.isInterface) {
                hierarchy.getDirectImplementersOf(curSootClass)
                    .filter { it.interfaces.contains(curSootClass) } + hierarchy.getDirectSubinterfacesOf(curSootClass)
            } else {
                hierarchy.getDirectSubclassesOf(curSootClass)
            }
        if (implementers.isEmpty()) continue
        val oldLists = res.removeIfAndReturnRemovedElements { it.last() == curSootClass }
        if (curSootClass.isConcrete) {
            oldLists.forEach { res.add(it.toMutableList()) }
        }
        for (implementer in implementers) {
            queue.add(implementer)
            oldLists.forEach { res.add((it + listOf(implementer)).toMutableList()) }
        }
    }
    return res
}

fun SootMethod.getAllTypesFromCastAndInstanceOfInstructions(): Set<Class<*>> =
    this.activeBody.units.asSequence().filterIsInstance<JAssignStmt>()
        .map { it.rightOp }
        .filter { it is JCastExpr || it is JInstanceOfExpr }
        .mapNotNull {
            when (it) {
                is JCastExpr -> {
                    val type = it.type
                    when (type) {
                        is RefType -> type.sootClass?.toJavaClass()
                        is ArrayType -> {
                            if (type.elementType is RefType) {
                                try {
                                    Class.forName("[L${type.elementType};")
                                } catch (e: Throwable) {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                    //(it.type as? RefType)?.sootClass?.toJavaClass()
                }
                else -> {
                    val type = (it as JInstanceOfExpr).checkType
                    when (type) {
                        is RefType -> type.sootClass?.toJavaClass()
                        is ArrayType -> {
                            if (type.elementType is RefType) {
                                try {
                                    Class.forName("[L${type.elementType};")
                                } catch (e: Throwable) {
                                    null
                                }
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                }
            }
        }.toSet()

fun SootMethod.getClassFieldsUsedByFunc(clazz: Class<*>) =
    activeBody.units
        .asSequence()
        .mapNotNull { it as? JAssignStmt }
        .map { it.rightOp }
        .mapNotNull { it as? JInstanceFieldRef }
        .mapNotNull { fieldRef -> clazz.getAllDeclaredFields().find { it.name == fieldRef.field.name } }
        .toSet()

fun SootClass.toJavaClass(): Class<*>? =
    try {
        Class.forName(this.name)
    } catch (e: Throwable) {
        try {
            CustomClassLoader.classLoader.loadClass(this.name)
        } catch (e: Throwable) {
            null
        }
    }

fun KFunction<*>.toSootMethod(): SootMethod? = this.javaMethod?.toSootMethod()

fun Class<*>.toSootClass() =
    Scene.v().classes.find { it.name == this.name }
fun Method.toSootMethod(): SootMethod? {
    val cl = declaringClass.toSootClass() ?: return null
    return cl.methods.find {
        val sig = it.bytecodeSignature.drop(1).dropLast(1).substringAfter("${cl.name}: ")
        this.signature == sig
    }
}

fun SootMethod.toJavaMethod(): Executable? =
    declaringClass.toJavaClass()?.getAllDeclaredMethodsAndConstructors()?.find {
        it.signature == this.bytecodeSignature.drop(1).dropLast(1).substringAfter("${declaringClass.name}: ")
    }

fun SootField.toJavaField(): Field? =
    declaringClass.toJavaClass()?.getAllDeclaredFields()?.find { it.name == name }

fun Field.toSootField(): SootField? =
    declaringClass.toSootClass()?.fields?.find { it.name == name }

fun SootClass.getAllAncestors(): List<SootClass> {
    val queue = ArrayDeque<SootClass>()
    val res = mutableSetOf<SootClass>()
    this.superclassOrNull?.let { queue.add(it) }
    queue.addAll(this.interfaces)
    while (queue.isNotEmpty()) {
        val el = queue.removeFirst()
        el.superclassOrNull?.let {
            if (!res.contains(it) && !queue.contains(it)) queue.add(it)
        }
        el.interfaces.map { if (!res.contains(it) && !queue.contains(it)) queue.add(it) }
        res.add(el)
    }
    return res.toList()
}

val SootClass.children
    get() =
        Scene.v().classes.filter { it.getAllAncestors().contains(this) }

val SootClass.superclassOrNull
    get() =
        try {
            superclass
        } catch (e: Exception) {
            null
        }

//TODO add stuff with generics
object SootStaticsCollector {

    private val classToStaticMethodsInstanceProviders = mutableMapOf<Class<*>, List<Method>>()
    private val classToStaticFieldsInstanceProviders = mutableMapOf<Class<*>, List<Field>>()
    fun getStaticMethodsInitializersOf(clazz: Class<*>): List<Method> {
        if (classToStaticMethodsInstanceProviders.contains(clazz)) return classToStaticMethodsInstanceProviders[clazz]!!
        val classes = Scene.v().classes.filter { !it.name.contains("$") }
        val sootMethodsToProvideInstance = classes.flatMap {
            it.methods
                .asSequence()
                .filter { it.isStatic && it.returnType.toString() == clazz.name }
                .filter { it.parameterTypes.all { !it.toString().contains(clazz.name) } }
                .filter { !it.toString().contains('$') }
                .toList()
        }
        val javaMethodsToProvideInstance = sootMethodsToProvideInstance.mapNotNull { it.toJavaMethod() as? Method }
        classToStaticMethodsInstanceProviders[clazz] = javaMethodsToProvideInstance
        return javaMethodsToProvideInstance
    }

    fun getStaticFieldsInitializersOf(clazz: Class<*>): List<Field> {
        if (classToStaticFieldsInstanceProviders.contains(clazz)) return classToStaticFieldsInstanceProviders[clazz]!!
        val classes = Scene.v().classes.filter { !it.name.contains("$") }
        val sootFieldsToProvideInstance = classes.flatMap {
            it.fields
                .asSequence()
                .filter { it.isStatic && it.type.toString() == clazz.name }
                .toList()
        }
        val javaFieldsToProvideInstance = sootFieldsToProvideInstance.mapNotNull { it.toJavaField() }
        classToStaticFieldsInstanceProviders[clazz] = javaFieldsToProvideInstance
        return javaFieldsToProvideInstance
    }
}