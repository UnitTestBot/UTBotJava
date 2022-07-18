package org.utbot.framework.plugin.api.util

import org.utbot.common.findFieldOrNull
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicInteger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

// ClassId utils

@Suppress("unused")
val ClassId.enclosingClass: ClassId?
    get() = jClass.enclosingClass?.id
/**
 * Returns underlying type:
 * - for arrays e.g. [[[I return I
 * - otherwise return the given type
 */
val ClassId.underlyingType: ClassId
    get() = generateSequence(this) { it.elementClassId }.last()

// TODO: check
val ClassId.isRefType: Boolean
    get() = !isPrimitive && !isArray

/**
 * If [this] represents a primitive type, then return its jvm name, otherwise return null.
 * Here we also consider void type primitive, despite the fact that technically it is not a primitive type.
 */
fun ClassId.primitiveTypeJvmNameOrNull(): String? {
    if (!this.isPrimitive) return null
    return when (this) {
        voidClassId -> "V"
        booleanClassId -> "Z"
        byteClassId -> "B"
        charClassId -> "C"
        shortClassId -> "S"
        intClassId -> "I"
        longClassId -> "J"
        floatClassId -> "F"
        doubleClassId -> "D"
        else -> error("Primitive type expected here, but got: $this")
    }
}

// TODO: maybe cache it somehow in the future
@Suppress("MapGetWithNotNullAssertionOperator")
// TODO: Make jClass, method, constructor and field private for IdUtil and rewrite all the code depending on it.
// TODO: This is needed to avoid accessing actual Class, Method, Constructor and Field instances for builtin ids.
// TODO: All the properties of ids can be accessed via corresponding properties in the public API.
val ClassId.jClass: Class<*>
    get() = when {
        isPrimitive -> idToPrimitive[this]!!
        isArray -> Class.forName(name, true, utContext.classLoader) // TODO: probably rewrite
        else -> utContext.classLoader.loadClass(name)
    }

/**
 * @return true if the given ids are equal or the first one is a subtype of the second one
 *
 * Note: primitive types and their corresponding wrappers are considered interchangeable
 * Checks ids for equality, but also counts primitive wrappers and their corresponding primitives as matching
 */
infix fun ClassId.isSubtypeOf(type: ClassId): Boolean {
    if (this.isArray && type.isArray) {
        if (this.isPrimitiveArray && type != this) return false
        return this.elementClassId!! isSubtypeOf type.elementClassId!!
    }
    if (type == objectClassId) return true
    // unwrap primitive wrappers
    val left = primitiveByWrapper[this] ?: this
    val right = primitiveByWrapper[type] ?: type
    if (left == right) {
        return true
    }
    val leftClass = this.jClass
    val interfaces = sequence {
        var types = listOf(leftClass)
        while (types.isNotEmpty()) {
            yieldAll(types)
            types = types.map { it.interfaces }.flatMap { it.toList() }
        }
    }
    val superclasses = generateSequence(leftClass) { it.superclass }
    val superTypes = interfaces + superclasses
    return right in superTypes.map { it.id }
}

infix fun ClassId.isNotSubtypeOf(type: ClassId): Boolean = !(this isSubtypeOf type)

val ClassId.kClass: KClass<*>
    get() = jClass.kotlin

val ClassId.isFloatType: Boolean
    get() = this == floatClassId || this == floatWrapperClassId

val ClassId.isDoubleType: Boolean
    get() = this == doubleClassId || this == doubleWrapperClassId

val voidClassId = ClassId("void")
val booleanClassId = ClassId("boolean")
val byteClassId = ClassId("byte")
val charClassId = ClassId("char")
val shortClassId = ClassId("short")
val intClassId = ClassId("int")
val longClassId = ClassId("long")
val floatClassId = ClassId("float")
val doubleClassId = ClassId("double")

// primitive wrappers ids
val voidWrapperClassId = java.lang.Void::class.id
val booleanWrapperClassId = java.lang.Boolean::class.id
val byteWrapperClassId = java.lang.Byte::class.id
val charWrapperClassId = java.lang.Character::class.id
val shortWrapperClassId = java.lang.Short::class.id
val intWrapperClassId = java.lang.Integer::class.id
val longWrapperClassId = java.lang.Long::class.id
val floatWrapperClassId = java.lang.Float::class.id
val doubleWrapperClassId = java.lang.Double::class.id

// We consider void wrapper as primitive wrapper
// because voidClassId is considered primitive here
val primitiveWrappers = setOf(
    voidWrapperClassId,
    booleanWrapperClassId,
    byteWrapperClassId,
    charWrapperClassId,
    shortWrapperClassId,
    intWrapperClassId,
    longWrapperClassId,
    floatWrapperClassId,
    doubleWrapperClassId,
)

val primitiveByWrapper = mapOf(
    booleanWrapperClassId to booleanClassId,
    byteWrapperClassId to byteClassId,
    charWrapperClassId to charClassId,
    shortWrapperClassId to shortClassId,
    intWrapperClassId to intClassId,
    longWrapperClassId to longClassId,
    floatWrapperClassId to floatClassId,
    doubleWrapperClassId to doubleClassId,
)

val wrapperByPrimitive = primitiveByWrapper.entries.associateBy({ it.value }) { it.key }

// We consider void primitive here
// It is sometimes useful even if void is not technically a primitive type
val primitives = setOf(
    voidClassId,
    booleanClassId,
    byteClassId,
    charClassId,
    shortClassId,
    intClassId,
    longClassId,
    floatClassId,
    doubleClassId
)

val booleanArrayClassId = ClassId("[Z", booleanClassId)
val byteArrayClassId = ClassId("[B", byteClassId)
val charArrayClassId = ClassId("[C", charClassId)
val shortArrayClassId = ClassId("[S", shortClassId)
val intArrayClassId = ClassId("[I", intClassId)
val longArrayClassId = ClassId("[J", longClassId)
val floatArrayClassId = ClassId("[F", floatClassId)
val doubleArrayClassId = ClassId("[D", doubleClassId)

val stringClassId = java.lang.String::class.id

val objectClassId = java.lang.Object::class.id

val objectArrayClassId = Array<Any>::class.id

val atomicIntegerClassId = AtomicInteger::class.id

val atomicIntegerGet = MethodId(atomicIntegerClassId, "get", intClassId, emptyList())
val atomicIntegerGetAndIncrement = MethodId(atomicIntegerClassId, "getAndIncrement", intClassId, emptyList())

val iterableClassId = java.lang.Iterable::class.id
val mapClassId = java.util.Map::class.id

@Suppress("RemoveRedundantQualifierName")
val primitiveToId: Map<Class<*>, ClassId> = mapOf(
    java.lang.Void.TYPE to voidClassId,
    java.lang.Byte.TYPE to byteClassId,
    java.lang.Short.TYPE to shortClassId,
    java.lang.Character.TYPE to charClassId,
    java.lang.Integer.TYPE to intClassId,
    java.lang.Long.TYPE to longClassId,
    java.lang.Float.TYPE to floatClassId,
    java.lang.Double.TYPE to doubleClassId,
    java.lang.Boolean.TYPE to booleanClassId
)

@Suppress("RemoveRedundantQualifierName")
val idToPrimitive: Map<ClassId, Class<*>> = mapOf(
        voidClassId to java.lang.Void.TYPE,
        byteClassId to java.lang.Byte.TYPE,
        shortClassId to java.lang.Short.TYPE,
        charClassId to java.lang.Character.TYPE,
        intClassId to java.lang.Integer.TYPE,
        longClassId to java.lang.Long.TYPE,
        floatClassId to java.lang.Float.TYPE,
        doubleClassId to java.lang.Double.TYPE,
        booleanClassId to java.lang.Boolean.TYPE
)

/**
 * Check if type is primitive or String.
 *
 * Used in [org.utbot.framework.codegen.model.constructor.tree.CgVariableConstructor.constructAssemble].
 */
fun isPrimitiveWrapperOrString(type: ClassId): Boolean = (type in primitiveWrappers) || (type == stringClassId)

/**
 * Returns a wrapper of a given type if it is primitive or a type itself otherwise.
 */
fun wrapIfPrimitive(type: ClassId): ClassId = when (type) {
    booleanClassId -> booleanWrapperClassId
    byteClassId -> byteWrapperClassId
    charClassId -> charWrapperClassId
    shortClassId -> shortWrapperClassId
    intClassId -> intWrapperClassId
    longClassId -> longWrapperClassId
    floatClassId -> floatWrapperClassId
    doubleClassId -> doubleWrapperClassId
    else -> type
}

/**
 * Note: currently uses class$innerClass form to load classes with classloader.
 */
@Suppress("MapGetWithNotNullAssertionOperator")
val Class<*>.id: ClassId
    get() = when {
        isArray -> ClassId(name, componentType.id)
        isPrimitive -> primitiveToId[this]!!
        else -> ClassId(name)
    }

val KClass<*>.id: ClassId
    get() = java.id

val ClassId.isArray: Boolean
    get() = elementClassId != null

val ClassId.isPrimitive: Boolean
    get() = this in primitives

val ClassId.isPrimitiveArray: Boolean
    get() = elementClassId != null && elementClassId.isPrimitive

val ClassId.isPrimitiveWrapper: Boolean
    get() = this in primitiveWrappers

val ClassId.isIterable: Boolean
    get() = isSubtypeOf(iterableClassId)

val ClassId.isMap: Boolean
    get() = isSubtypeOf(mapClassId)

val ClassId.isIterableOrMap: Boolean
    get() = isIterable || isMap

fun ClassId.findFieldOrNull(fieldName: String): Field? = jClass.findFieldOrNull(fieldName)

fun ClassId.hasField(fieldName: String): Boolean = findFieldOrNull(fieldName) != null

fun ClassId.defaultValueModel(): UtModel = when (this) {
    intClassId -> UtPrimitiveModel(0)
    byteClassId -> UtPrimitiveModel(0.toByte())
    shortClassId -> UtPrimitiveModel(0.toShort())
    longClassId -> UtPrimitiveModel(0L)
    floatClassId -> UtPrimitiveModel(0.0f)
    doubleClassId -> UtPrimitiveModel(0.0)
    booleanClassId -> UtPrimitiveModel(false)
    charClassId -> UtPrimitiveModel('\u0000')
    else -> UtNullModel(this)
}

// FieldId utils

// TODO: maybe cache it somehow in the future
val FieldId.field: Field
    get() = declaringClass.jClass.declaredFields.firstOrNull { it.name == name }
        ?: error("Field $name is not found in class ${declaringClass.jClass.name}")

// https://docstore.mik.ua/orelly/java-ent/jnut/ch03_13.htm
val FieldId.isInnerClassEnclosingClassReference: Boolean
    get() = declaringClass.isNested && name == "this$0"

val KProperty<*>.fieldId: FieldId
    get() = javaField?.fieldId ?: error("Expected field, but got: $this")

val Field.fieldId: FieldId
    get() = FieldId(declaringClass.id, name)

// ExecutableId utils

val ExecutableId.executable: Executable
    get() = when (this) {
        is MethodId -> method
        is ConstructorId -> constructor
    }

val ExecutableId.exceptions: List<ClassId>
    get() = executable.exceptionTypes.map { it.id }

// TODO: maybe cache it somehow in the future
val MethodId.method: Method
    get() {
        val declaringClass = classId.jClass
        return declaringClass.singleMethodOrNull(signature)
                ?: error("Can't find method $signature in ${declaringClass.name}")
    }

// TODO: maybe cache it somehow in the future
val ConstructorId.constructor: Constructor<*>
    get() {
        val declaringClass = classId.jClass
        return declaringClass.singleConstructorOrNull(signature)
                ?: error("Can't find method $signature in ${declaringClass.name}")
    }

val KCallable<*>.executableId: ExecutableId
    get() = when (this) {
        is KFunction<*> -> javaMethod?.executableId
                ?: javaConstructor?.executableId
                ?: error("$this is neither a method nor a constructor")
        is KProperty<*> -> javaGetter?.executableId ?: error("Getter for $this not found")
        else -> error("Unknown KCallable type: ${this::class}")
    }

val Method.executableId: MethodId
    get() {
        val classId = declaringClass.id
        val arguments = parameterTypes.map { it.id }.toTypedArray()
        val retType = returnType.id
        return methodId(classId, name, retType, *arguments)
    }

val Constructor<*>.executableId: ConstructorId
    get() {
        val classId = declaringClass.id
        val arguments = parameterTypes.map { it.id }.toTypedArray()
        return constructorId(classId, *arguments)
    }

@ExperimentalContracts
fun ExecutableId.isMethod(): Boolean {
    contract {
        returns(true) implies (this@isMethod is MethodId)
        returns(false) implies (this@isMethod is ConstructorId)
    }
    return this is MethodId
}

@ExperimentalContracts
fun ExecutableId.isConstructor(): Boolean {
    contract {
        returns(true) implies (this@isConstructor is ConstructorId)
        returns(false) implies (this@isConstructor is MethodId)
    }
    return this is ConstructorId
}

/**
 * Construct MethodId
 */
fun methodId(classId: ClassId, name: String, returnType: ClassId, vararg arguments: ClassId): MethodId {
    return MethodId(classId, name, returnType, arguments.toList())
}

/**
 * Construct ConstructorId
 */
fun constructorId(classId: ClassId, vararg arguments: ClassId): ConstructorId {
    return ConstructorId(classId, arguments.toList())
}

fun builtinMethodId(classId: BuiltinClassId, name: String, returnType: ClassId, vararg arguments: ClassId): BuiltinMethodId {
    return BuiltinMethodId(classId, name, returnType, arguments.toList())
}

fun builtinStaticMethodId(classId: ClassId, name: String, returnType: ClassId, vararg arguments: ClassId): BuiltinMethodId {
    return BuiltinMethodId(classId, name, returnType, arguments.toList(), isStatic = true)
}
