package org.utbot.framework.codegen.tree

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.utbot.framework.codegen.domain.RegularImport
import org.utbot.framework.codegen.domain.StaticImport
import org.utbot.framework.codegen.domain.builtin.setArrayElement
import org.utbot.framework.codegen.domain.context.CgContextOwner
import org.utbot.framework.codegen.domain.models.CgAllocateInitializedArray
import org.utbot.framework.codegen.domain.models.CgArrayInitializer
import org.utbot.framework.codegen.domain.models.CgClassId
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgTypeCast
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.services.access.CgCallableAccessManager
import org.utbot.framework.codegen.util.at
import org.utbot.framework.codegen.util.isAccessibleFrom
import org.utbot.framework.fields.ArrayElementAccess
import org.utbot.framework.fields.FieldAccess
import org.utbot.framework.fields.FieldPath
import org.utbot.framework.plugin.api.BuiltinClassId
import org.utbot.framework.plugin.api.BuiltinMethodId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.WildcardTypeParameter
import org.utbot.framework.plugin.api.util.arrayLikeName
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.builtinStaticMethodId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.denotableType
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.enclosingClass
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.signature
import org.utbot.framework.plugin.api.util.underlyingType
import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class EnvironmentFieldStateCache(
    val thisInstance: FieldStateCache,
    val arguments: Array<FieldStateCache>,
    val classesWithStaticFields: MutableMap<ClassId, FieldStateCache>
) {
    companion object {
        fun emptyCacheFor(execution: UtExecution): EnvironmentFieldStateCache {
            val argumentsCache = Array(execution.stateBefore.parameters.size) { FieldStateCache() }

            val staticFields = execution.stateBefore.statics.keys
            val classesWithStaticFields = staticFields.groupBy { it.declaringClass }.keys
            val staticFieldsCache = mutableMapOf<ClassId, FieldStateCache>().apply {
                for (classId in classesWithStaticFields) {
                    put(classId, FieldStateCache())
                }
            }

            return EnvironmentFieldStateCache(
                thisInstance = FieldStateCache(),
                arguments = argumentsCache,
                classesWithStaticFields = staticFieldsCache
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EnvironmentFieldStateCache

        if (thisInstance != other.thisInstance) return false
        if (!arguments.contentEquals(other.arguments)) return false
        if (classesWithStaticFields != other.classesWithStaticFields) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thisInstance.hashCode()
        result = 31 * result + arguments.contentHashCode()
        result = 31 * result + classesWithStaticFields.hashCode()
        return result
    }
}

class FieldStateCache {
    val before: MutableMap<FieldPath, CgFieldState> = mutableMapOf()
    val after: MutableMap<FieldPath, CgFieldState> = mutableMapOf()

    val paths: List<FieldPath>
        get() = (before.keys union after.keys).toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FieldStateCache

        if (before != other.before) return false
        if (after != other.after) return false

        return true
    }

    override fun hashCode(): Int {
        var result = before.hashCode()
        result = 31 * result + after.hashCode()
        return result
    }
}

data class CgFieldState(val variable: CgVariable, val model: UtModel)

data class ExpressionWithType(val type: ClassId, val expression: CgExpression)

/**
 * Check if a method is an util method of the current class
 */
internal fun CgContextOwner.isUtil(method: MethodId): Boolean {
    return method in utilMethodProvider.utilMethodIds
}

val classCgClassId = CgClassId(Class::class.id, typeParameters = WildcardTypeParameter(), isNullable = false)

/**
 * A [MethodId] to add an item into [ArrayList].
 */
internal val addToListMethodId: MethodId
    get() = methodId(
        classId = ArrayList::class.id,
        name = "add",
        returnType = booleanClassId,
        arguments = arrayOf(Object::class.id),
    )

/**
 * A [ClassId] of class `org.junit.jupiter.params.provider.Arguments`
 */
internal val argumentsClassId: BuiltinClassId
    get() = BuiltinClassId(
        simpleName = "Arguments",
        canonicalName = "org.junit.jupiter.params.provider.Arguments",
        packageName = "org.junit.jupiter.params.provider"
    )

/**
 * A [MethodId] to call JUnit Arguments method.
 */
internal val argumentsMethodId: BuiltinMethodId
    get() = builtinStaticMethodId(
        classId = argumentsClassId,
        name = "arguments",
        returnType = argumentsClassId,
        // vararg of Objects
        arguments = arrayOf(objectArrayClassId)
    )

internal fun getStaticFieldVariableName(owner: ClassId, path: FieldPath): String {
    val elements = mutableListOf<String>()
    elements += owner.simpleName.decapitalize()
    // TODO: check how capitalize() works with numeric strings e.g. "0"
    elements += path.toStringList().map { it.capitalize() }
    return elements.joinToString("")
}

internal fun getFieldVariableName(owner: CgValue, path: FieldPath): String {
    val elements = mutableListOf<String>()
    if (owner is CgVariable) {
        elements += owner.name
    }
    // TODO: check how capitalize() works with numeric strings e.g. "0"
    elements += path.toStringList().map { it.capitalize() }
    if (elements.size > 0) {
        elements[0] = elements[0].decapitalize()
    }
    return elements.joinToString("")
}

private fun FieldPath.toStringList(): List<String> =
    elements.map {
        when (it) {
            is FieldAccess -> it.field.name
            is ArrayElementAccess -> it.index.toString()
        }
    }

internal fun infiniteInts(): Sequence<Int> =
    generateSequence(1) { it + 1 }

internal const val MAX_ARRAY_INITIALIZER_SIZE = 10

private var simpleNamesNotRequiringImports = emptySet<String>()

/**
 * Some class names do not require imports, but may lead to simple names clash.
 * For example, custom class Compiler may clash with java.lang.Compiler.
 */
private fun findSimpleNamesNotRequiringImports(): Set<String> {
    if (simpleNamesNotRequiringImports.isEmpty()) {
        val reflectionsStore = Reflections("java.lang", Scanners.SubTypes).store.values
        val fullNamesNotRequiringImports = reflectionsStore.flatMap { it.entries }.flatMap { it.value }

        simpleNamesNotRequiringImports = fullNamesNotRequiringImports.map { it.substringAfterLast('.') }.toSet()
    }

    return simpleNamesNotRequiringImports
}

/**
 * Checks if we have already imported a class with such simple name.
 * If so, we cannot import [type] (because it will be used with simple name and will be clashed with already imported)
 * and should use its fully qualified name instead.
 */
private fun CgContextOwner.doesNotHaveSimpleNameClash(type: ClassId): Boolean =
    importedClasses.none { it.simpleName == type.simpleName } && type.simpleName !in findSimpleNamesNotRequiringImports()

fun CgContextOwner.importIfNeeded(type: ClassId) {
    // TODO: for now we consider that tests are generated in the same package as CUT, but this may change
    val underlyingType = type.underlyingType

    underlyingType
        .takeIf { (it.isRefType && it.packageName != testClassPackageName && it.packageName != "java.lang") || it.isNested }
        // we cannot import inaccessible classes (builtin classes like JUnit are accessible here because they are public)
        ?.takeIf { it.isAccessibleFrom(testClassPackageName) }
        // don't import classes from default package
        ?.takeIf { !it.isInDefaultPackage }
        // cannot import anonymous classes
        ?.takeIf { !it.isAnonymous }
        // do not import if there is a simple name clash
        ?.takeIf { doesNotHaveSimpleNameClash(it) }
        ?.let {
            importedClasses += it
            collectedImports += it.toImport()
        }

    // for nested classes we need to import enclosing class
    if (underlyingType.isNested) {
        importIfNeeded(underlyingType.enclosingClass!!)
    }
}

internal fun CgContextOwner.importIfNeeded(method: MethodId) {
    val name = method.name
    val packageName = method.classId.packageName
    method.takeIf { it.isStatic && packageName != testClassPackageName && packageName != "java.lang" }
        .takeIf { importedStaticMethods.none { it.name == name } }
        // do not import method under test in order to specify the declaring class directly for its calls
        .takeIf { currentExecutable != method }
        ?.let {
            importedStaticMethods += method
            collectedImports += StaticImport(method.classId.canonicalName, method.name)
        }
}

/**
 * Casts [expression] to [targetType].
 *
 * This method uses [denotableType] of the given [targetType] to ensure
 * that we are using a denotable type in the type cast.
 *
 * Specifically, if we attempt to do a type cast to an anonymous class,
 * then we will actually perform a type cast to its supertype.
 *
 * @see denotableType
 *
 * @param isSafetyCast shows if we should render "as?" instead of "as" in Kotlin
 */
internal fun CgContextOwner.typeCast(
    targetType: ClassId,
    expression: CgExpression,
    isSafetyCast: Boolean = false
): CgExpression {
    val denotableTargetType = targetType.denotableType
    importIfNeeded(denotableTargetType)
    return CgTypeCast(denotableTargetType, expression, isSafetyCast)
}

/**
 * Sets an element of arguments array in parameterized test,
 * if test framework represents arguments as array.
 */
internal fun <T> T.setArgumentsArrayElement(
    array: CgVariable,
    index: Int,
    value: CgExpression,
    constructor: CgStatementConstructor
) where T : CgContextOwner, T: CgCallableAccessManager {
    when (array.type) {
        objectClassId -> {
            +java.lang.reflect.Array::class.id[setArrayElement](array, index, value)
        }
        else -> with(constructor) { array.at(index) `=` value }
    }
}

@Suppress("unused")
internal fun newArrayOf(elementType: ClassId, values: List<CgExpression>): CgAllocateInitializedArray {
    val arrayType = arrayTypeOf(elementType)
    return CgAllocateInitializedArray(arrayInitializer(arrayType, elementType, values))
}

internal fun arrayInitializer(arrayType: ClassId, elementType: ClassId, values: List<CgExpression>): CgArrayInitializer =
    CgArrayInitializer(arrayType, elementType, values)


/**
 * For a given [elementType] returns a [ClassId] of an array with elements of this type.
 * For example, for an id of `int` the result will be an id of `int[]`.
 *
 * @param elementType the element type of the returned array class id
 * @param isNullable a flag whether returned array is nullable or not
 */
fun arrayTypeOf(elementType: ClassId, isNullable: Boolean = false): ClassId {
    val arrayIdName = "[${elementType.arrayLikeName}"
    return when (elementType) {
        is BuiltinClassId -> BuiltinClassId(
            canonicalName = "${elementType.canonicalName}[]",
            simpleName = "${elementType.simpleName}[]",
            elementClassId = elementType,
            isNullable = isNullable
        )
        else -> ClassId(
            name = arrayIdName,
            elementClassId = elementType,
            isNullable = isNullable
        )
    }
}

internal fun Class<*>.overridesEquals(): Boolean =
    when {
        // Object does not override equals
        this == Any::class.java -> false
        id isSubtypeOf Map::class.id -> true
        id isSubtypeOf Collection::class.id -> true
        else -> declaredMethods.any { it.name == "equals" && it.parameterTypes.contentEquals(arrayOf(Any::class.java)) }
    }

/**
 * Returns all methods of [this] class (including inherited), except base methods (i.e., if any method is overridden,
 * only the latest overriding will be included).
 * NOTE: for the reference [see also](https://stackoverflow.com/a/28408148)
 */
private fun Class<*>.allMethodsWithoutBaseMethods(): Set<Method> {
    val collectedMethods = mutableSetOf<Method>(*methods)
    val types = collectedMethods.map { it.signature }.associateWithTo(mutableMapOf()) { mutableSetOf<Package>() }
    val access = Modifier.PUBLIC or Modifier.PROTECTED or Modifier.PRIVATE

    var currentClass: Class<*>? = this
    while (currentClass != null) {
        for (method in currentClass.declaredMethods) {
            val modifiers = method.modifiers

            if (!Modifier.isStatic(modifiers)) {
                when (modifiers and access) {
                    Modifier.PUBLIC -> continue
                    Modifier.PROTECTED -> {
                        if (types.putIfAbsent(method.signature, mutableSetOf()) != null) {
                            continue
                        }
                    }
                    Modifier.PRIVATE -> {}
                    else -> { // package-private
                        val pkg = types.computeIfAbsent(method.signature) { mutableSetOf() }

                        if (pkg.isNotEmpty() && pkg.add(currentClass.getPackage())) {
                            break
                        } else {
                            continue
                        }
                    }
                }
            }

            collectedMethods += method
        }

        currentClass = currentClass.superclass
    }

    return collectedMethods
}

// NOTE: this function does not consider executable return type because it is not important in our case
internal fun ClassId.getAmbiguousOverloadsOf(executableId: ExecutableId): Sequence<ExecutableId> {
    val allExecutables = when (executableId) {
        is MethodId -> {
            // For method we should check all overloadings and inherited methods, but do not consider base methods
            // in case of overriding (for example, for ArrayList#add we should not take List#add and AbstractCollection#add)
            executableId.classId.jClass.allMethodsWithoutBaseMethods().map { it.executableId }.asSequence()
        }
        is ConstructorId -> allConstructors
    }

    return allExecutables.filter {
        it.name == executableId.name && it.parameters.size == executableId.executable.parameters.size
    }
}


internal infix fun ClassId.hasAmbiguousOverloadsOf(executableId: ExecutableId): Boolean {
    // TODO: for now we assume that builtin classes do not have ambiguous overloads
    if (this is BuiltinClassId) return false

    return getAmbiguousOverloadsOf(executableId).toList().size > 1
}

private val defaultByPrimitiveType: Map<ClassId, Any> = mapOf(
    booleanClassId to false,
    byteClassId to 0.toByte(),
    charClassId to '\u0000',
    shortClassId to 0.toShort(),
    intClassId to 0,
    longClassId to 0L,
    floatClassId to 0.0f,
    doubleClassId to 0.0
)

/**
 * By 'default' here we mean a value that is used for a type in one of the two cases:
 * - When we allocate an array of some type in the following manner: `new int[10]`,
 * the array is filled with some value. In case of `int` this value is `0`, for `boolean`
 * it is `false`, etc.
 * - When we allocate an instance of some reference type e.g. `new A()` and the class `A` has field `int a`.
 * If the constructor we use does not initialize `a` directly and `a` is not assigned to some value
 * at the declaration site, then `a` will be assigned to some `default` value, e.g. `0` for `int`.
 *
 * Here we do not consider default values of nested arrays of multidimensional arrays,
 * because they depend on the way the outer array is allocated:
 * - An array allocated using `new int[10][]` will contain 10 `null` elements.
 * - An array allocated using `new int[10][5]` will contain 10 arrays of 5 elements,
 *   where each element is `0`.
 */
internal infix fun UtModel.isDefaultValueOf(type: ClassId): Boolean =
    when (this) {
        is UtNullModel -> type.isRefType // null is default for ref types
        is UtPrimitiveModel -> value == defaultByPrimitiveType[type]
        else -> false
    }

internal infix fun UtModel.isNotDefaultValueOf(type: ClassId): Boolean = !this.isDefaultValueOf(type)

/**
 * If the model contains a store for the given [index], return the model of this store.
 * Otherwise, return a [UtArrayModel.constModel] of this array model.
 */
internal operator fun UtArrayModel.get(index: Int): UtModel = stores[index] ?: constModel


internal fun ClassId.utilMethodId(
    name: String,
    returnType: ClassId,
    vararg arguments: ClassId,
    // usually util methods are static, so this argument is true by default
    isStatic: Boolean = true
): MethodId =
    BuiltinMethodId(this, name, returnType, arguments.toList(), isStatic = isStatic)

fun ClassId.toImport(): RegularImport = RegularImport(packageName, simpleNameWithEnclosingClasses)

// Immutable collections utils

internal operator fun <T> PersistentList<T>.plus(element: T): PersistentList<T> =
    this.add(element)

internal operator fun <T> PersistentList<T>.plus(other: PersistentList<T>): PersistentList<T> =
    this.addAll(other)

internal operator fun <T> PersistentSet<T>.plus(element: T): PersistentSet<T> =
    this.add(element)

internal operator fun <T> PersistentSet<T>.plus(other: PersistentSet<T>): PersistentSet<T> =
    this.addAll(other)
