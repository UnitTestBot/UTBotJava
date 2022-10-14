package org.utbot.fuzzer

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

interface FuzzerPlatform {

    fun collectConstructors(classId: ClassId, description: FuzzedMethodDescription): Sequence<ConstructorId> = emptySequence()

    fun findSuitableFields(classId: ClassId, description: FuzzedMethodDescription): List<FieldDescription> = emptyList()

    fun toPlatformClassId(otherLanguageClassId: ClassId) : ClassId = otherLanguageClassId

    fun toLanguageUtModel(platformUtModel: UtModel, description: FuzzedMethodDescription): UtModel = platformUtModel

    fun isEnum(classId: ClassId) = false

    fun isAbstract(classId: ClassId) = false

    fun isStatic(classId: ClassId) = true

    fun isInner(classId: ClassId) = false

    fun isSubtypeOf(classId: ClassId, superClassId: ClassId) = false

    class FieldDescription(
        val name: String,
        val classId: ClassId,
        val canBeSetDirectly: Boolean,
        val setter: Method?,
    )
}

object NoFuzzerPlaform : FuzzerPlatform {
    override fun collectConstructors(classId: ClassId, description: FuzzedMethodDescription): Sequence<ConstructorId> = emptySequence()
    override fun findSuitableFields(classId: ClassId, description: FuzzedMethodDescription): List<FuzzerPlatform.FieldDescription>  = emptyList()
}

object JavaFuzzerPlatform : FuzzerPlatform {

    override fun collectConstructors(classId: ClassId, description: FuzzedMethodDescription): Sequence<ConstructorId> {
        return classId.jClass.declaredConstructors.asSequence()
            .filter { javaConstructor ->
                isAccessible(javaConstructor, description.packageName)
            }
            .map { javaConstructor ->
                ConstructorId(classId, javaConstructor.parameters.map { it.type.id })
            }
    }

    override fun findSuitableFields(classId: ClassId, description: FuzzedMethodDescription): List<FuzzerPlatform.FieldDescription>  {
        val jClass = classId.jClass
        return jClass.declaredFields.map { field ->
            FuzzerPlatform.FieldDescription(
                field.name,
                field.type.id,
                isAccessible(
                    field,
                    description.packageName
                ) && !Modifier.isFinal(field.modifiers) && !Modifier.isStatic(field.modifiers),
                jClass.findPublicSetterIfHasPublicGetter(field, description)
            )
        }
    }

    private fun isAccessible(member: Member, packageName: String?): Boolean {
        return Modifier.isPublic(member.modifiers) ||
                (packageName != null && isPackagePrivate(member.modifiers) && member.declaringClass.`package`?.name == packageName)
    }

    private fun isPackagePrivate(modifiers: Int): Boolean {
        val hasAnyAccessModifier = Modifier.isPrivate(modifiers)
                || Modifier.isProtected(modifiers)
                || Modifier.isProtected(modifiers)
        return !hasAnyAccessModifier
    }

    private fun Class<*>.findPublicSetterIfHasPublicGetter(field: Field, description: FuzzedMethodDescription): Method? {
        val postfixName = field.name.capitalize()
        val setterName = "set$postfixName"
        val getterName = "get$postfixName"
        val getter = try { getDeclaredMethod(getterName) } catch (_: NoSuchMethodException) { return null }
        return if (isAccessible(getter, description.packageName) && getter.returnType == field.type) {
            declaredMethods.find {
                isAccessible(it, description.packageName) &&
                        it.name == setterName &&
                        it.parameterCount == 1 &&
                        it.parameterTypes[0] == field.type
            }
        } else {
            null
        }
    }

    override fun isEnum(classId: ClassId): Boolean = classId.jClass.isEnum

    override fun isAbstract(classId: ClassId): Boolean = classId.isAbstract

    override fun isStatic(classId: ClassId): Boolean = classId.isStatic

    override fun isInner(classId: ClassId): Boolean = classId.isInner

    override fun isSubtypeOf(classId: ClassId, superClassId: ClassId): Boolean {
        // commented code above doesn't work this case: SomeList<T> extends LinkedList<T> {} and Collection
//        return isSubtypeOf(another)
        return superClassId.jClass.isAssignableFrom(classId.jClass)
    }
}