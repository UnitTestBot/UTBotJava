package org.utbot.engine.greyboxfuzzer.quickcheck.internal

import org.javaruntype.type.Type
import java.lang.reflect.AnnotatedArrayType
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.AnnotatedParameterizedType
import java.lang.reflect.AnnotatedType
import java.lang.reflect.AnnotatedWildcardType
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.security.AccessController
import java.security.PrivilegedAction
import java.util.Arrays
import java.lang.annotation.Annotation as JavaAnnotation

object Reflection {
    private val PRIMITIVES = HashMap<Class<*>, Class<*>>(16)

    init {
        PRIMITIVES[java.lang.Boolean.TYPE] = Boolean::class.java
        PRIMITIVES[java.lang.Byte.TYPE] = Byte::class.java
        PRIMITIVES[Character.TYPE] = Char::class.java
        PRIMITIVES[java.lang.Double.TYPE] = Double::class.java
        PRIMITIVES[java.lang.Float.TYPE] = Float::class.java
        PRIMITIVES[Integer.TYPE] = Int::class.java
        PRIMITIVES[java.lang.Long.TYPE] = Long::class.java
        PRIMITIVES[java.lang.Short.TYPE] = Short::class.java
    }

    fun maybeWrap(clazz: Class<*>): Class<*> {
        val wrapped = PRIMITIVES[clazz]
        return wrapped ?: clazz
    }

    fun <T> findConstructor(
        type: Class<T>,
        vararg parameterTypes: Class<*>?
    ): Constructor<T> {
        return try {
            type.getConstructor(*parameterTypes)
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun <T> findDeclaredConstructor(
        type: Class<T>,
        vararg parameterTypes: Class<*>?
    ): Constructor<T> {
        return try {
            val ctor = type.getDeclaredConstructor(*parameterTypes)
            ctor.isAccessible = true
            ctor
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun <T> singleAccessibleConstructor(
        type: Class<T>
    ): Constructor<T> {
        val constructors = type.constructors
        if (constructors.size != 1) {
            throw ReflectionException(
                "$type needs a single accessible constructor"
            )
        }
        return constructors[0] as Constructor<T>
    }

    fun <T> instantiate(clazz: Class<T>): T {
        return try {
            clazz.newInstance()
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun <T> instantiate(ctor: Constructor<T>, vararg args: Any?): T {
        return try {
            ctor.newInstance(*args)
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun supertypes(bottom: Type<*>): Set<Type<*>> {
        val supertypes: MutableSet<Type<*>> = HashSet()
        supertypes.add(bottom)
        supertypes.addAll(bottom.allTypesAssignableFromThis)
        return supertypes
    }

    fun defaultValueOf(
        annotationType: Class<out Annotation?>,
        attribute: String
    ): Any {
        return try {
            annotationType.getMethod(attribute).defaultValue
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun allAnnotations(e: AnnotatedElement): List<JavaAnnotation> {
        val thisAnnotations = nonSystemAnnotations(e)
        val annotations = ArrayList<JavaAnnotation>()
        for (each in thisAnnotations) {
            annotations.add(each)
            annotations.addAll(allAnnotations(each.annotationType()))
        }
        return annotations
    }

    fun <T : Annotation?> allAnnotationsByType(
        e: AnnotatedElement,
        type: Class<T>?
    ): List<T> {
        val annotations = ArrayList<T>(e.getAnnotationsByType(type).toList())
        val thisAnnotations = nonSystemAnnotations(e)
        for (each in thisAnnotations) {
            annotations.addAll(allAnnotationsByType(each.annotationType(), type))
        }
        return annotations
    }

    fun findMethod(
        target: Class<*>,
        methodName: String,
        vararg argTypes: Class<*>?
    ): Method {
        return try {
            target.getMethod(methodName, *argTypes)
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    operator fun invoke(method: Method, target: Any?, vararg args: Any?): Any {
        return try {
            method.invoke(target, *args)
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun findField(type: Class<*>, fieldName: String): Field {
        return try {
            type.getDeclaredField(fieldName)
        } catch (ex: NoSuchFieldException) {
            throw reflectionException(ex)
        }
    }

    fun allDeclaredFieldsOf(type: Class<*>?): List<Field> {
        val allFields = ArrayList<Field>()
        var c = type
        while (c != null) {
            allFields.addAll(c.declaredFields)
            c = c.superclass
        }
        val results = allFields.filter { !it.isSynthetic }
        results.forEach { it.isAccessible = true }
        return results
    }

    fun setField(
        field: Field,
        target: Any?,
        value: Any?,
        suppressProtection: Boolean
    ) {
        AccessController.doPrivileged(PrivilegedAction<Void?> {
            field.isAccessible = suppressProtection
            null
        })
        try {
            field[target] = value
        } catch (ex: Exception) {
            throw reflectionException(ex)
        }
    }

    fun jdk9OrBetter(): Boolean {
        return try {
            Runtime::class.java.getMethod("version")
            true
        } catch (e: NoSuchMethodException) {
            false
        }
    }

    fun singleAbstractMethodOf(rawClass: Class<*>): Method? {
        if (!rawClass.isInterface) return null
        var abstractCount = 0
        var singleAbstractMethod: Method? = null
        for (each in rawClass.methods) {
            if (Modifier.isAbstract(each.modifiers)
                && !overridesJavaLangObjectMethod(each)
            ) {
                singleAbstractMethod = each
                ++abstractCount
            }
        }
        return if (abstractCount == 1) singleAbstractMethod else null
    }

    fun isMarkerInterface(clazz: Class<*>): Boolean {
        return if (!clazz.isInterface) false else Arrays.stream(clazz.methods)
            .filter { m: Method -> !m.isDefault }
            .allMatch { method: Method -> overridesJavaLangObjectMethod(method) }
    }

    private fun overridesJavaLangObjectMethod(method: Method): Boolean {
        return isEquals(method) || isHashCode(method) || isToString(method)
    }

    private fun isEquals(method: Method): Boolean {
        return "equals" == method.name && method.parameterTypes.size == 1 && Any::class.java == method.parameterTypes[0]
    }

    private fun isHashCode(method: Method): Boolean {
        return "hashCode" == method.name && method.parameterTypes.size == 0
    }

    private fun isToString(method: Method): Boolean {
        return "toString" == method.name && method.parameterTypes.size == 0
    }

    fun reflectionException(ex: Exception?): RuntimeException {
        if (ex is InvocationTargetException) {
            return ReflectionException(
                ex.targetException
            )
        }
        return if (ex is RuntimeException) ex else ReflectionException(
            ex!!
        )
    }

    private fun nonSystemAnnotations(e: AnnotatedElement): List<JavaAnnotation> {
        return e.annotations.map { it as JavaAnnotation }
            .filter { !it.annotationType().name.startsWith("java.lang.annotation.") }
            .filter { !it.annotationType().name.startsWith("kotlin.") }
    }

    fun annotatedComponentTypes(
        annotatedType: AnnotatedType?
    ): List<AnnotatedType> {
        if (annotatedType is AnnotatedParameterizedType) {
            return Arrays.asList(
                *annotatedType
                    .annotatedActualTypeArguments
            )
        }
        if (annotatedType is AnnotatedArrayType) {
            return listOf(
                annotatedType
                    .annotatedGenericComponentType
            )
        }
        if (annotatedType is AnnotatedWildcardType) {
            val wildcard = annotatedType
            return if (wildcard.annotatedLowerBounds.size > 0) listOf(wildcard.annotatedLowerBounds[0]) else Arrays.asList(
                *wildcard.annotatedUpperBounds
            )
        }
        return emptyList()
    }

}