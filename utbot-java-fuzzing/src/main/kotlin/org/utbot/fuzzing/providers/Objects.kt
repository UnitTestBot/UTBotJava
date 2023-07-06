package org.utbot.fuzzing.providers

import org.utbot.common.isAbstract
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.*
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
            construct = Routine.Create(constructorId.executable.genericParameterTypes.map {
                toFuzzerType(it, description.typeCache)
            }) { values ->
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
                findAccessibleModifiableFields(description, classId, description.description.packageName).forEach { fd ->
                    when {
                        fd.canBeSetDirectly -> {
                            yield(Routine.Call(listOf(fd.type)) { self, values ->
                                val model = self.model as UtAssembleModel
                                model.modificationsChain as MutableList += UtDirectSetFieldModel(
                                    model,
                                    FieldId(classId, fd.name),
                                    values.first().model
                                )
                            })
                        }

                        fd.setter != null && fd.getter != null -> {
                            yield(Routine.Call(listOf(fd.type)) { self, values ->
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
            empty = nullRoutine(classId)
        )
    }

    private fun isIgnored(type: ClassId): Boolean {
        return unwantedConstructorsClasses.contains(type)
                || type.isCollectionOrMap
                || type.isPrimitiveWrapper
                || type.isEnum
                || type.isAbstract
                || (type.isInner && !type.isStatic)
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

    override fun enrich(description: FuzzedDescription, type: FuzzedType, scope: Scope) {
        // any value in static function is ok to fuzz
        if (description.description.isStatic == true && scope.recursionDepth == 1) {
            scope.putProperty(NULLABLE_PROP, true)
        }
        // any value except this
        if (description.description.isStatic == false && scope.parameterIndex > 0 && scope.recursionDepth == 1) {
            scope.putProperty(NULLABLE_PROP, true)
        }
    }

    override fun accept(type: FuzzedType) = type.classId.isRefType

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence<Seed<FuzzedType, FuzzedValue>> {
        if (description.scope?.getProperty(NULLABLE_PROP) == true) {
            yield(Seed.Simple(nullFuzzedValue(classClassId)))
        }
    }
}

class CreateObjectAnywayValueProvider(
    val idGenerator: IdGenerator<Int>,
    val useMock: Boolean = false,
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {

    override fun accept(type: FuzzedType) = type.classId.isRefType

    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence<Seed<FuzzedType, FuzzedValue>> {
        val methodCalls = description.constants.filter {
            it.value == CreateObjectAnywayValueProvider::class
        }.mapNotNull {
            it.fuzzedContext as? FuzzedContext.Call
        }.map {
            it.method
        }.toSet()

        yield(Seed.Recursive(
            construct = Routine.Create(emptyList()) {
                UtCompositeModel(idGenerator.createId(), type.classId, useMock).fuzzed {
                    summary = "some object"
                }
            },
            modify = sequence {
                // generate all fields
                generateSequence(type.classId.jClass) {
                    it.superclass
                }.flatMap { javaClass ->
                    javaClass.declaredFields.toList()
                }.forEach { field ->
                    yield(Routine.Call(listOf(toFuzzerType(field.type, description.typeCache))) { instance, args ->
                        (instance.model as UtCompositeModel).fields[field.fieldId] = args.first().model
                    })
                }

                generateSequence(listOf(type.classId.jClass)) { classList ->
                    classList.flatMap { listOf(it.superclass) + it.interfaces }.filterNotNull().takeIf { it.isNotEmpty() }
                }.flatten().filter {
                    isAccessible(it, description.description.packageName)
                }.flatMap { javaClass ->
                    javaClass.declaredMethods.filter {
                        javaClass.isInterface || it.isAbstract
                    }.filter {
                        isAccessible(it, description.description.packageName)
                    }
                    // todo filter by methods seen in code
                }.forEach { method ->
                    val executableId = method.executableId
                    if (methodCalls.contains(executableId)) {
                        yield(Routine.Call(listOf(toFuzzerType(method.returnType, description.typeCache))) { instance, args ->
                            (instance.model as UtCompositeModel).mocks[executableId] = args.map(FuzzedValue::model)
                        })
                    }
                }
            },
            empty = Routine.Empty {
                UtCompositeModel(idGenerator.createId(), type.classId, useMock).fuzzed {
                    summary = "some object"
                }
            }
        ))
    }
}

internal class PublicSetterGetter(
    val setter: Method,
    val getter: Method,
)

internal class FieldDescription(
    val name: String,
    val type: FuzzedType,
    val canBeSetDirectly: Boolean,
    val setter: Method?,
    val getter: Method?
)

internal fun findAccessibleModifiableFields(description: FuzzedDescription?, classId: ClassId, packageName: String?): List<FieldDescription>  {
    return generateSequence(classId.jClass) { it.superclass }.flatMap { jClass ->
        jClass.declaredFields.map { field ->
            val setterAndGetter = jClass.findPublicSetterGetterIfHasPublicGetter(field, packageName)
            FieldDescription(
                name = field.name,
                type = if (description != null) toFuzzerType(
                    field.type,
                    description.typeCache
                ) else FuzzedType(field.type.id),
                canBeSetDirectly = isAccessible(
                    field,
                    packageName
                ) && !Modifier.isFinal(field.modifiers) && !Modifier.isStatic(field.modifiers),
                setter = setterAndGetter?.setter,
                getter = setterAndGetter?.getter,
            )
        }
    }.toList()
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
    var clazz = member.declaringClass
    while (clazz != null) {
        if (!isAccessible(clazz, packageName)) return false
        clazz = clazz.enclosingClass
    }
    return Modifier.isPublic(member.modifiers) ||
            (packageName != null && isNotPrivateOrProtected(member.modifiers) && member.declaringClass.`package`?.name == packageName)
}

internal fun isAccessible(clazz: Class<*>, packageName: String?): Boolean {
    return Modifier.isPublic(clazz.modifiers) ||
            (packageName != null && isNotPrivateOrProtected(clazz.modifiers) && clazz.`package`?.name == packageName)
}

private fun isNotPrivateOrProtected(modifiers: Int): Boolean {
    val hasAnyAccessModifier = Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)
    return !hasAnyAccessModifier
}