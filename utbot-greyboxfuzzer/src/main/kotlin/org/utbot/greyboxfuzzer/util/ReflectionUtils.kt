@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.utbot.greyboxfuzzer.util

import org.utbot.greyboxfuzzer.quickcheck.internal.ParameterTypeContext
import org.javaruntype.type.Types
import org.utbot.common.withAccessibility
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsInfo
import sun.reflect.generics.reflectiveObjects.GenericArrayTypeImpl
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl
import java.lang.reflect.*
import kotlin.random.Random

fun Class<*>.getAllDeclaredFields(): List<Field> {
    val res = mutableListOf<Field>()
    var current: Class<*>? = this
    while (current != null) {
        try {
            res.addAll(current.declaredFields)
        } catch (_: Error) {

        }
        current = current.superclass
    }
    return res
}

val java.lang.reflect.Type.rawType: java.lang.reflect.Type
    get() = if (this is ParameterizedType) rawType else this
fun Class<*>.getAllDeclaredMethodsAndConstructors(): List<Executable> {
    val res = mutableListOf<Executable>()
    res.addAll(declaredConstructors)
    var current: Class<*>? = this
    while (current != null) {
        res.addAll(current.declaredMethods)
        current = current.superclass
    }
    return res
}

fun Field.getFieldValue(instance: Any?): Any? {
    try {
        val fixedInstance =
            if (this.isStatic()) {
                null
            } else instance
        return withAccessibility {
            when (this.type) {
                Boolean::class.javaPrimitiveType -> this.getBoolean(fixedInstance)
                Byte::class.javaPrimitiveType -> this.getByte(fixedInstance)
                Char::class.javaPrimitiveType -> this.getChar(fixedInstance)
                Short::class.javaPrimitiveType -> this.getShort(fixedInstance)
                Int::class.javaPrimitiveType -> this.getInt(fixedInstance)
                Long::class.javaPrimitiveType -> this.getLong(fixedInstance)
                Float::class.javaPrimitiveType -> this.getFloat(fixedInstance)
                Double::class.javaPrimitiveType -> this.getDouble(fixedInstance)
                else -> this.get(fixedInstance)
            }
        }
    } catch (_: Throwable) {
        return null
    }
}

fun Field.setFieldValue(instance: Any?, fieldValue: Any?) {
    withAccessibility {
        val fixedInstance =
            if (this.isStatic()) {
                null
            } else instance
        when (this.type) {
            Boolean::class.javaPrimitiveType -> this.setBoolean(fixedInstance, fieldValue as Boolean)
            Byte::class.javaPrimitiveType -> this.setByte(fixedInstance, fieldValue as Byte)
            Char::class.javaPrimitiveType -> this.setChar(fixedInstance, fieldValue as Char)
            Short::class.javaPrimitiveType -> this.setShort(fixedInstance, fieldValue as Short)
            Int::class.javaPrimitiveType -> this.setInt(fixedInstance, fieldValue as Int)
            Long::class.javaPrimitiveType -> this.setLong(fixedInstance, fieldValue as Long)
            Float::class.javaPrimitiveType -> this.setFloat(fixedInstance, fieldValue as Float)
            Double::class.javaPrimitiveType -> this.setDouble(fixedInstance, fieldValue as Double)
            else -> this.set(fixedInstance, fieldValue)
        }
    }
}

fun Type.toClass(): Class<*>? =
    try {
        when (this) {
            is ParameterizedTypeImpl -> this.rawType
            is ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl -> this.rawType.toClass()
            is GenericArrayTypeImpl -> java.lang.reflect.Array.newInstance(
                this.genericComponentType.toClass(),
                0
            ).javaClass
            is ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl -> java.lang.reflect.Array.newInstance(
                this.genericComponentType.toClass(),
                0
            ).javaClass
            is ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable -> this.rawType.toClass()
            else -> this as? Class<*>
        }
    } catch (e: Exception) {
        null
    }

fun Field.generateInstance(instance: Any, generatedValue: Any?) {
    if (this.isStatic() && this.isFinal) return
    this.isAccessible = true
    this.isFinal = false
    if (this.isEnumConstant || this.isSynthetic) return
    if (this.type.isPrimitive) {
        val definedValue = generatedValue
        when (definedValue?.javaClass) {
            null -> this.set(instance, null)
            Boolean::class.javaObjectType -> this.setBoolean(instance, definedValue as Boolean)
            Byte::class.javaObjectType -> this.setByte(instance, definedValue as Byte)
            Char::class.javaObjectType -> this.setChar(instance, definedValue as Char)
            Short::class.javaObjectType -> this.setShort(instance, definedValue as Short)
            Int::class.javaObjectType -> this.setInt(instance, definedValue as Int)
            Long::class.javaObjectType -> this.setLong(instance, definedValue as Long)
            Float::class.javaObjectType -> this.setFloat(instance, definedValue as Float)
            Double::class.javaObjectType -> this.setDouble(instance, definedValue as Double)
            else -> return
        }
    } else {
        this.set(instance, generatedValue)
    }
}

private fun Class<*>.processArray(value: Any): List<Field> {
//    return (0 until JArray.getLength(value)).map { JArray.get(value, it) }
//    val subFields = mutableListOf<Field>()
//    for (i in 0 until JArray.getLength(value)) {
//        val field = JArray.get(value, i)
//    }
    return emptyList()
}

//private fun Field

var Field.isFinal: Boolean
    get() = (this.modifiers and Modifier.FINAL) == Modifier.FINAL
    set(value) {
        if (value == this.isFinal) return
        val modifiersField = this.javaClass.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(this, this.modifiers and if (value) Modifier.FINAL else Modifier.FINAL.inv())
    }

fun Method.isStatic() = modifiers.and(Modifier.STATIC) > 0
fun Field.isStatic() = modifiers.and(Modifier.STATIC) > 0
fun Method.hasModifiers(vararg modifiers: Int) = modifiers.all { it.and(this.modifiers) > 0 }
fun Field.hasModifiers(vararg modifiers: Int) = modifiers.all { it.and(this.modifiers) > 0 }
fun Class<*>.hasModifiers(vararg modifiers: Int) = modifiers.all { it.and(this.modifiers) > 0 }
fun Constructor<*>.hasModifiers(vararg modifiers: Int) = modifiers.all { it.and(this.modifiers) > 0 }
fun Constructor<*>.hasAtLeastOneOfModifiers(vararg modifiers: Int) = modifiers.any { it.and(this.modifiers) > 0 }
fun Class<*>.hasAtLeastOneOfModifiers(vararg modifiers: Int) = modifiers.any { it.and(this.modifiers) > 0 }
fun Class<*>.canBeInstantiated() = !hasAtLeastOneOfModifiers(Modifier.ABSTRACT, Modifier.INTERFACE)
fun Field.hasAtLeastOneOfModifiers(vararg modifiers: Int) = modifiers.any { it.and(this.modifiers) > 0 }

fun ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl.getActualArguments(): Array<Type> {
    val args = this.javaClass.getAllDeclaredFields().find { it.name == "actualArguments" } ?: return arrayOf()
    return args.let {
        it.isAccessible = true
        it.get(this) as Array<Type>
    }.also { args.isAccessible = false }
}

fun List<Constructor<*>>.chooseRandomConstructor() =
    if (Random.getTrue(60)) {
        this.shuffled().minByOrNull { it.parameterCount }
    } else this.randomOrNull()

fun generateParameterizedTypeImpl(
    clazz: Class<*>,
    actualTypeParameters: Array<Type>
): ParameterizedTypeImpl {
    val constructor = ParameterizedTypeImpl::class.java.declaredConstructors.first()
    constructor.isAccessible = true
    return constructor.newInstance(clazz, actualTypeParameters, null) as ParameterizedTypeImpl
}

object ReflectionUtils {
    fun getRandomClassWithBounds(bound: Class<*>): Class<*> {
        return Any::class.java
    }

    fun forJavaReflectTypeSafe(type: Type): org.javaruntype.type.Type<*> {
        val strType = type.toString()
        val safeType =
            if (type is WildcardType) {
                if ((strType.contains(" super ") && strType.contains("extends")) || strType.contains("extends")) {
                    type.upperBounds.firstOrNull() ?: Any::class.java.rawType
                } else if (strType.contains(" super ")) {
                    type.lowerBounds.firstOrNull() ?: Any::class.java.rawType
                } else {
                    Any::class.java.rawType
                }
            } else type
        return try {
            Types.forJavaLangReflectType(safeType)
        } catch (e: Throwable) {
            try {
                Types.forJavaLangReflectType(safeType.toClass())
            } catch (e: Throwable) {
                Types.forJavaLangReflectType(Any::class.java)
            }
        }
    }

}

val ParameterizedTypeImpl.actualTypeArgumentsRecursive: List<Type>
    get() {
        val queue = ArrayDeque<Type>()
        val res = mutableListOf<Type>()
        if (this is TypeVariable<*>) {
            queue.add(this)
        }
        this.actualTypeArguments.map { queue.add(it) }
        while (queue.isNotEmpty()) {
            val el = queue.removeFirst()
            if (el is ParameterizedTypeImpl) {
                el.actualTypeArguments.map { queue.add(it) }
            }
            res.add(el)
        }
        return res
    }


//fun Parameter.replaceUnresolvedGenericsToRandomTypes() {
//    val allUnresolvedTypesInType = (this.parameterizedType as? ParameterizedTypeImpl)
//        ?.actualTypeArgumentsRecursive
//        ?.filter { it is WildcardType || it is TypeVariable<*> }
//        ?: return
//    val allUnresolvedTypesInAnnotatedType = (this.annotatedType.type as? ParameterizedTypeImpl)
//        ?.actualTypeArgumentsRecursive
//        ?.filter { it is WildcardType || it is TypeVariable<*> }
//        ?: return
//    val allUnresolvedTypes = allUnresolvedTypesInType.zip(allUnresolvedTypesInAnnotatedType)
//    for ((unresolvedType, unresolvedTypeCopy) in allUnresolvedTypes) {
//        val upperBound =
//            if (unresolvedType is WildcardType) {
//                unresolvedType.upperBounds.firstOrNull() ?: continue
//            } else if (unresolvedType is TypeVariable<*>) {
//                unresolvedType.bounds?.firstOrNull() ?: continue
//            } else continue
//        val upperBoundAsSootClass = upperBound.toClass()?.toSootClass() ?: continue
//        val randomChild =
//            upperBoundAsSootClass.children.filterNot { it.name.contains("$") }.randomOrNull()?.toJavaClass() ?: continue
//        val upperBoundsFields =
//            if (unresolvedType is WildcardType) {
//                unresolvedType.javaClass.getAllDeclaredFields().find { it.name.contains("upperBounds") }!! to
//                        unresolvedTypeCopy.javaClass.getAllDeclaredFields().find { it.name.contains("upperBounds") }!!
//            } else if (unresolvedType is TypeVariable<*>) {
//                unresolvedType.javaClass.getAllDeclaredFields().find { it.name.contains("bounds") }!! to
//                        unresolvedTypeCopy.javaClass.getAllDeclaredFields().find { it.name.contains("bounds") }!!
//            } else continue
//        upperBoundsFields.first.setFieldValue(unresolvedType, arrayOf(randomChild))
//        upperBoundsFields.second.setFieldValue(unresolvedType, arrayOf(randomChild))
//    }
//}

fun Method.resolveMethod(
    parameterTypeContext: ParameterTypeContext,
    typeArguments: List<Type>
): Pair<LinkedHashMap<String, Type>, GenericsContext> {
    val cl = this.declaringClass//parameterTypeContext.resolved.rawClass
    val resolvedJavaType =
        parameterTypeContext.generics.resolveType(parameterTypeContext.type()) as? ParameterizedType
    val gm = LinkedHashMap<String, Type>()
    if (resolvedJavaType != null) {
        typeArguments.zip(resolvedJavaType.actualTypeArguments.toList()).forEach {
            gm[it.first.typeName] = it.second
        }
    }
    val m = mutableMapOf(cl to gm)
    val generics = LinkedHashMap<String, Type>()
    typeArguments.forEachIndexed { index, typeVariable ->
        generics[typeVariable.typeName] =
            (resolvedJavaType as ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl).actualTypeArguments[index]
    }
    val gctx = GenericsContext(GenericsInfo(cl, m), cl)
    gctx.method(this).methodGenericsMap().forEach { (s, type) -> generics.getOrPut(s) { type } }
    return generics to gctx
}

fun org.javaruntype.type.Type<*>.convertToPrimitiveIfPossible(): org.javaruntype.type.Type<*> {
    val possiblePrimitive = when (this.toString()) {
        java.lang.Short::class.java.name -> Short::class.java
        java.lang.Byte::class.java.name -> Byte::class.java
        java.lang.Integer::class.java.name -> Int::class.java
        java.lang.Long::class.java.name -> Long::class.java
        java.lang.Float::class.java.name -> Float::class.java
        java.lang.Double::class.java.name -> Double::class.java
        java.lang.Character::class.java.name -> Char::class.java
        java.lang.Boolean::class.java.name -> Boolean::class.java
        else -> null
    }
    return possiblePrimitive?.let { ReflectionUtils.forJavaReflectTypeSafe(it) } ?: this
}

fun Type.getActualTypeArguments(): Array<Type> =
    when (this) {
        is ParameterizedTypeImpl -> this.actualTypeArguments
        is ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl -> this.actualTypeArguments
        else -> arrayOf()
    }

class GenericsReplacer {
    private val replacedGenerics = mutableListOf<ReplacedTypeParameter>()

    private data class ReplacedTypeParameter(
        val type: Type,
        val typeBound: Type?,
        val annotatedType: Type,
        val annotatedTypeBound: Type?
    )

    fun replaceUnresolvedGenericsToRandomTypes(parameter: Parameter) {
        if (replacedGenerics.isNotEmpty()) {
            makeReplacement(replacedGenerics)
            return
        }
        val allUnresolvedTypesInType = (parameter.parameterizedType as? ParameterizedTypeImpl)
            ?.actualTypeArgumentsRecursive
            ?.filter { it is WildcardType || it is TypeVariable<*> }
            ?: return
        val allUnresolvedTypesInAnnotatedType = (parameter.annotatedType.type as? ParameterizedTypeImpl)
            ?.actualTypeArgumentsRecursive
            ?.filter { it is WildcardType || it is TypeVariable<*> }
            ?: return
        val allUnresolvedTypes = allUnresolvedTypesInType.zip(allUnresolvedTypesInAnnotatedType)
        replacedGenerics.addAll(
            allUnresolvedTypes.map {
                ReplacedTypeParameter(it.first, getUpperBound(it.first), it.second, getUpperBound(it.second))
            }
        )
        makeReplacement(replacedGenerics)
    }

    fun revert() {
        if (replacedGenerics.isEmpty()) return
        for ((type, upperBound, annotatedType, _) in replacedGenerics) {
            setUpperBoundTo(type, annotatedType, upperBound?.toClass() ?: Any::class.java)
        }
    }

    private fun makeReplacement(allUnresolvedTypes: List<ReplacedTypeParameter>) {
        for ((type, upperBound, annotatedType, _) in allUnresolvedTypes) {
            val upperBoundAsSootClass = upperBound?.toClass()?.toSootClass() ?: continue
            val newRandomBound =
                upperBoundAsSootClass.children
                    .filterNot { it.name.contains("$") }
                    .filterNot { it.javaPackageName.startsWith("sun") }
                    .randomOrNull()?.toJavaClass() ?: continue
            setUpperBoundTo(type, annotatedType, newRandomBound)
        }
    }

    private fun setUpperBoundTo(type: Type, annotatedType: Type, clazz: Class<*>) {
        val upperBoundsFields =
            when (type) {
                is WildcardType -> {
                    type.javaClass.getAllDeclaredFields().find { it.name.contains("upperBounds") }!! to
                            annotatedType.javaClass.getAllDeclaredFields()
                                .find { it.name.contains("upperBounds") }!!
                }
                is TypeVariable<*> -> {
                    type.javaClass.getAllDeclaredFields().find { it.name.contains("bounds") }!! to
                            annotatedType.javaClass.getAllDeclaredFields().find { it.name.contains("bounds") }!!
                }
                else -> return
            }
        upperBoundsFields.first.setFieldValue(type, arrayOf(clazz))
        upperBoundsFields.second.setFieldValue(type, arrayOf(clazz))
    }

    private fun getUpperBound(unresolvedType: Type): Type? =
        when (unresolvedType) {
            is WildcardType -> unresolvedType.upperBounds.firstOrNull()
            is TypeVariable<*> -> unresolvedType.bounds?.firstOrNull()
            else -> null
        }

}