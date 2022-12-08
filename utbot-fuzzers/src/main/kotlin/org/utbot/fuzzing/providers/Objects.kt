package org.utbot.fuzzing.providers

import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.providers.ConstantsModelProvider.fuzzed
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.hex
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ObjectValueProvider(
    val idGenerator: IdGenerator<Int>,
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {

    private val unwantedConstructorsClasses = listOf(
        stringClassId,
        dateClassId,
        NumberValueProvider.classId
    )

    override fun accept(type: FuzzedType) = !isIgnored(type.classId)

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        val classId = type.classId
        val constructors = findTypesOfNonRecursiveConstructor(type, description.description.packageName)
            .takeIf { it.isNotEmpty() }
            ?.asSequence()
            ?: classId.allConstructors.filter {
                isAccessible(it.constructor, description.description.packageName)
            }
        constructors.forEach { constructorId ->
            yield(createValue(classId, constructorId, description))
        }
    }

    private fun createValue(classId: ClassId, constructorId: ConstructorId, description: FuzzedDescription): Seed.Recursive<FuzzedType, FuzzedValue> {
        return Seed.Recursive(
            construct = Routine.Create(constructorId.parameters.map { FuzzedType(it) }) { values ->
                val id = idGenerator.createId()
                UtAssembleModel(
                    id = id,
                    classId = classId,
                    modelName = "${constructorId.classId.name}${constructorId.parameters}#" + id.hex(),
                    instantiationCall = UtExecutableCallModel(
                        null,
                        constructorId,
                        values.map { it.model }),
                    modificationsChainProvider = { mutableListOf() }
                ).fuzzed {
                    summary = "%var% = ${classId.simpleName}(${constructorId.parameters.joinToString { it.simpleName }})"
                }
            },
            modify = sequence {
                findAccessibleModifableFields(classId, description.description.packageName).forEach { fd ->
                    when {
                        fd.canBeSetDirectly -> {
                            yield(Routine.Call(listOf(FuzzedType(fd.classId))) { self, values ->
                                val model = self.model as UtAssembleModel
                                model.modificationsChain as MutableList += UtDirectSetFieldModel(
                                    model,
                                    FieldId(classId, fd.name),
                                    values.first().model
                                )
                            })
                        }

                        fd.setter != null && fd.getter != null -> {
                            yield(Routine.Call(listOf(FuzzedType(fd.classId))) { self, values ->
                                val model = self.model as UtAssembleModel
                                model.modificationsChain as MutableList += UtExecutableCallModel(
                                    model,
                                    fd.setter.executableId,
                                    values.map { it.model })
                            })
                        }
                    }
                }
            },
            empty = Routine.Empty {
                UtNullModel(classId).fuzzed {
                    summary = "%var% = null"
                }
            }
        )
    }

    private fun isIgnored(type: ClassId): Boolean {
        return unwantedConstructorsClasses.contains(type)
                || type.isIterableOrMap
                || type.isPrimitiveWrapper
                || type.isEnum
                || type.isAbstract
                || (type.isInner && !type.isStatic)
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

    private fun findTypesOfNonRecursiveConstructor(type: FuzzedType, packageName: String?): List<ConstructorId> {
        return type.classId.allConstructors
            .filter { isAccessible(it.constructor, packageName) }
            .filter { c ->
                c.parameters.all { it.isPrimitive || it == stringClassId || it.isArray }
            }.toList()
    }
}

@Suppress("unused")
object NullValueProvider : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {
    override fun accept(type: FuzzedType) = type.classId.isRefType

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequenceOf<Seed<FuzzedType, FuzzedValue>>(
        Seed.Simple(UtNullModel(type.classId).fuzzed {
            summary = "%var% = null"
        })
    )
}

internal class PublicSetterGetter(
    val setter: Method,
    val getter: Method,
)

internal class FieldDescription(
    val name: String,
    val classId: ClassId,
    val canBeSetDirectly: Boolean,
    val setter: Method?,
    val getter: Method?
)

internal fun findAccessibleModifableFields(classId: ClassId, packageName: String?): List<FieldDescription>  {
    val jClass = classId.jClass
    return jClass.declaredFields.map { field ->
        val setterAndGetter = jClass.findPublicSetterGetterIfHasPublicGetter(field, packageName)
        FieldDescription(
            name = field.name,
            classId = field.type.id,
            canBeSetDirectly = isAccessible(
                field,
                packageName
            ) && !Modifier.isFinal(field.modifiers) && !Modifier.isStatic(field.modifiers),
            setter = setterAndGetter?.setter,
            getter = setterAndGetter?.getter,
        )
    }
}

internal fun Class<*>.findPublicSetterGetterIfHasPublicGetter(field: Field, packageName: String?): PublicSetterGetter? {
    @Suppress("DEPRECATION") val postfixName = field.name.capitalize()
    val setterName = "set$postfixName"
    val getterName = "get$postfixName"
    val getter = try { getDeclaredMethod(getterName) } catch (_: NoSuchMethodException) { return null }
    return if (isAccessible(getter, packageName) && getter.returnType == field.type) {
        declaredMethods.find {
            isAccessible(it, packageName) &&
                    it.name == setterName &&
                    it.parameterCount == 1 &&
                    it.parameterTypes[0] == field.type
        }?.let { PublicSetterGetter(it, getter) }
    } else {
        null
    }
}



internal fun isAccessible(member: Member, packageName: String?): Boolean {
    return Modifier.isPublic(member.modifiers) ||
            (packageName != null && isPackagePrivate(member.modifiers) && member.declaringClass.`package`?.name == packageName)
}

internal fun isPackagePrivate(modifiers: Int): Boolean {
    val hasAnyAccessModifier = Modifier.isPrivate(modifiers)
            || Modifier.isProtected(modifiers)
            || Modifier.isProtected(modifiers)
    return !hasAnyAccessModifier
}