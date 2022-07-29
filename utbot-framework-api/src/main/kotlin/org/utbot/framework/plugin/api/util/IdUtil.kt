package org.utbot.framework.plugin.api.util

import kotlinx.coroutines.runBlocking
import org.objectweb.asm.Type
import org.utbot.framework.plugin.api.*
import org.utbot.jcdb.api.*
import org.utbot.jcdb.api.ext.findClass
import org.utbot.jcdb.impl.types.*
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
import kotlin.reflect.jvm.*

// ClassId utils

@Suppress("unused")
val ClassId.enclosingClass: ClassId?
    get() = runBlocking { outerClass() }

/**
 * Returns underlying type:
 * - for arrays e.g. [[[I return I
 * - otherwise return the given type
 */
val ClassId.underlyingType: ClassId
    get() = generateSequence(this) { it.ifArrayGetElementClass() }.last()

// TODO: check
val ClassId.isRefType: Boolean
    get() = !isPrimitive && !isArray

/**
 * If [this] represents a primitive type, then return its jvm name, otherwise return null.
 * Here we also consider void type primitive, despite the fact that technically it is not a primitive type.
 */
fun ClassId.primitiveTypeJvmNameOrNull(): String? {
    if (!isPrimitive) return null
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
//infix fun ClassId.isSubtypeOf(type: ClassId): Boolean {
//    if (this.isArray && type.isArray) {
//        if (this.isPrimitiveArray && type != this) return false
//        return this.elementClassId!! isSubtypeOf type.elementClassId!!
//    }
//    if (type == objectClassId) return true
//    // unwrap primitive wrappers
//    val left = primitiveByWrapper[this] ?: this
//    val right = primitiveByWrapper[type] ?: type
//    if (left == right) {
//        return true
//    }
//    val leftClass = this.jClass
//    val interfaces = sequence {
//        var types = listOf(leftClass)
//        while (types.isNotEmpty()) {
//            yieldAll(types)
//            types = types.map { it.interfaces }.flatMap { it.toList() }
//        }
//    }
//    val superclasses = generateSequence(leftClass) { it.superclass }
//    val superTypes = interfaces + superclasses
//    return right in superTypes.map { it.id }
//}

//infix fun ClassId.isNotSubtypeOf(type: ClassId): Boolean = !(this isSubtypeOf type)

//val ClassId.kClass: KClass<*>
//    get() = jClass.kotlin

//val ClassId.isFloatType: Boolean
//    get() = this == floatClassId || this == floatWrapperClassId
//
//val ClassId.isDoubleType: Boolean
//    get() = this == doubleClassId || this == doubleWrapperClassId

val voidClassId get() = utContext.classpath.void
val booleanClassId get() = utContext.classpath.boolean
val byteClassId get() = utContext.classpath.byte
val charClassId get() = utContext.classpath.char
val shortClassId get() = utContext.classpath.short
val intClassId get() = utContext.classpath.int
val longClassId get() = utContext.classpath.long
val floatClassId get() = utContext.classpath.float
val doubleClassId get() = utContext.classpath.double

// primitive wrappers ids
val voidWrapperClassId get() = java.lang.Void::class.id
val booleanWrapperClassId get() = java.lang.Boolean::class.id
val byteWrapperClassId get() = java.lang.Byte::class.id
val charWrapperClassId get() = java.lang.Character::class.id
val shortWrapperClassId get() = java.lang.Short::class.id
val intWrapperClassId get() = java.lang.Integer::class.id
val longWrapperClassId get() = java.lang.Long::class.id
val floatWrapperClassId get() = java.lang.Float::class.id
val doubleWrapperClassId get() = java.lang.Double::class.id

// We consider void wrapper as primitive wrapper
// because voidClassId is considered primitive here
val primitiveWrappers
    get() = setOf(
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

val primitiveByWrapper
    get() = mapOf(
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
val primitives
    get() = setOf(
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

val booleanArrayClassId get() = utContext.classpath.booleanArray
val byteArrayClassId get() = utContext.classpath.byteArray
val charArrayClassId get() = utContext.classpath.charArray
val shortArrayClassId get() = utContext.classpath.shortArray
val intArrayClassId get() = utContext.classpath.intArray
val longArrayClassId get() = utContext.classpath.longArray
val floatArrayClassId get() = utContext.classpath.floatArray
val doubleArrayClassId get() = utContext.classpath.doubleArray

val stringClassId get() = java.lang.String::class.id

val objectClassId get() = java.lang.Object::class.id

val objectArrayClassId get() = Array<Any>::class.id

val atomicIntegerClassId get() = AtomicInteger::class.id

val atomicIntegerGet: MethodId
    get() {
        return runBlocking {
            atomicIntegerClassId.methods().first { it.name == "get" }
        }
    }
val atomicIntegerGetAndIncrement: MethodId
    get() {
        return runBlocking {
            atomicIntegerClassId.methods().first { it.name == "getAndIncrement" }
        }
    }

val iterableClassId get() = java.lang.Iterable::class.id
val mapClassId get() = java.util.Map::class.id

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
val Class<*>.id: ClassId get() = runBlocking { asClassId() }

@Suppress("MapGetWithNotNullAssertionOperator")
suspend fun Class<*>.asClassId(): ClassId {
    fun nameOf(clazz: Class<*>): String {
        return when {
            clazz.isArray -> nameOf(clazz.componentType) + "[]"
            else -> clazz.name
        }
    }

    val name = nameOf(this)
    return utContext.classpath.findClass(name)
}

suspend inline fun <reified T> asClass(): ClassId {
    return T::class.java.asClassId()
}

val KClass<*>.id: ClassId
    get() = runBlocking {
        utContext.classpath.findClass(jvmName)
    }

suspend fun KClass<*>.asClassId(): ClassId {
    return java.asClassId()
}

val ClassId.isArray: Boolean
    get() = this is ArrayClassId

val ClassId.isPrimitiveArray: Boolean
    get() = this is ArrayClassId && elementClass.isPrimitive

val ClassId.isPrimitiveWrapper: Boolean
    get() = runBlocking { autoboxIfNeeded() == this }

val ClassId.isIterable: Boolean
    get() = runBlocking { isSubtypeOf(iterableClassId) }

val ClassId.isMap: Boolean
    get() = runBlocking { isSubtypeOf(mapClassId) }

val ClassId.isIterableOrMap: Boolean
    get() = isIterable || isMap

fun ClassId.findFieldOrNull(fieldName: String): FieldId? = runBlocking {
    fields().firstOrNull { it.name == fieldName }
}

fun ClassId.findField(fieldName: String): FieldId = runBlocking {
    fields().firstOrNull { it.name == fieldName } ?: error("Can't find field $name#$fieldName")
}

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
    get() = classId.jClass.declaredFields.firstOrNull { it.name == name }
        ?: error("Field $name is not found in class ${classId.name}")

// https://docstore.mik.ua/orelly/java-ent/jnut/ch03_13.htm
val FieldId.isInnerClassEnclosingClassReference: Boolean
    get() = classId.isNested && name == "this$0"

val KProperty<*>.fieldId: FieldId
    get() = javaField?.fieldId ?: error("Expected field, but got: $this")

val Field.fieldId: FieldId
    get() = declaringClass.id.findFieldOrNull(name) ?: error("field not found $this but expected to be existed")

// ExecutableId utils

val ExecutableId.executable: Executable
    get() = when (this) {
        is MethodExecutableId -> method
        is ConstructorExecutableId -> constructor
    }

val ExecutableId.exceptions: List<ClassId>
    get() = executable.exceptionTypes.map { it.id }

// TODO: maybe cache it somehow in the future
val MethodExecutableId.method: Method
    get() {
        val declaringClass = classId.jClass
        return declaringClass.singleMethodOrNull(signature)
            ?: error("Can't find method $signature in ${declaringClass.name}")
    }

// TODO: maybe cache it somehow in the future
val ConstructorExecutableId.constructor: Constructor<*>
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

val Method.executableId: MethodExecutableId
    get() = runBlocking {
        val classId = declaringClass.asClassId()
        val descriptor = name + Type.getType(this@executableId).descriptor
        MethodExecutableId(classId.methods().first { it.signature(true) == descriptor })
    }

val Constructor<*>.executableId: ConstructorExecutableId
    get() = runBlocking {
        val classId = declaringClass.id
        val descriptor = "<init>" + Type.getType(this@executableId).descriptor
        ConstructorExecutableId(classId.methods().first { it.signature(true) == descriptor })
    }

@ExperimentalContracts
fun ExecutableId.isMethod(): Boolean {
    contract {
        returns(true) implies (this@isMethod is MethodExecutableId)
        returns(false) implies (this@isMethod is ConstructorExecutableId)
    }
    return this is MethodExecutableId
}

@ExperimentalContracts
fun ExecutableId.isConstructor(): Boolean {
    contract {
        returns(true) implies (this@isConstructor is ConstructorExecutableId)
        returns(false) implies (this@isConstructor is MethodExecutableId)
    }
    return this is ConstructorExecutableId
}

/**
 * Construct MethodId
 */
fun methodId(classId: ClassId, name: String, returnType: ClassId, vararg arguments: ClassId): MethodId = runBlocking {
    classId.methods().first {
        it.name == name && it.returnType == returnType && it.parameters().toTypedArray().contentEquals(arguments)
    }
}

fun MethodId.asExecutable(): ExecutableId {
    if (isConstructor()) {
        return ConstructorExecutableId(this)
    }
    return MethodExecutableId(this)
}

fun MethodId.asExecutableMethod(): MethodExecutableId {
    if (isConstructor()) {
        throw IllegalStateException("Method $this is a constructor")
    }
    return MethodExecutableId(this)
}

/**
 * Construct ConstructorId
 */
fun constructorId(classId: ClassId, arguments: List<ClassId> = emptyList()): ConstructorExecutableId = runBlocking {
    val argsArray = arguments.toTypedArray()
    ConstructorExecutableId(classId.methods().first {
        it.name == "<init>" && it.parameters().toTypedArray().contentEquals(argsArray)
    })
}

fun constructorId(classId: ClassId, vararg arguments: ClassId) = constructorId(classId, arguments.toList())

fun builtinMethodId(
    classId: BuiltinClassId,
    name: String,
    returnType: ClassId,
    vararg arguments: ClassId
): BuiltinMethodId {
    return BuiltinMethodId(classId, name, returnType, arguments.toList(), false)
}

fun builtinStaticMethodId(
    classId: BuiltinClassId,
    name: String,
    returnType: ClassId,
    vararg arguments: ClassId
): BuiltinMethodId {
    return BuiltinMethodId(classId, name, returnType, arguments.toList(), true).also {
        classId.withMethod(it)
    }
}
