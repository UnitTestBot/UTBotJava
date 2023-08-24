package org.utbot.fuzzing.providers

import mu.KotlinLogging
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtDirectSetFieldModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.constructor
import org.utbot.framework.plugin.api.util.dateClassId
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.executableId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isAbstract
import org.utbot.framework.plugin.api.util.isCollectionOrMap
import org.utbot.framework.plugin.api.util.isEnum
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.isRefType
import org.utbot.framework.plugin.api.util.isStatic
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.FuzzedType
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.IdentityPreservingIdGenerator
import org.utbot.fuzzer.fuzzed
import org.utbot.fuzzing.FuzzedDescription
import org.utbot.fuzzing.JavaValueProvider
import org.utbot.fuzzing.Routine
import org.utbot.fuzzing.Scope
import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.toFuzzerType
import org.utbot.fuzzing.traverseHierarchy
import org.utbot.fuzzing.utils.hex
import org.utbot.modifications.AnalysisMode
import org.utbot.modifications.FieldInvolvementMode
import org.utbot.modifications.UtBotFieldsModificatorsSearcher
import soot.Scene
import soot.SootClass
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.TypeVariable

private val logger = KotlinLogging.logger {}

private fun isKnownTypes(type: ClassId): Boolean {
    return type == stringClassId
            || type == dateClassId
            || type == NumberValueProvider.classId
            || type.isCollectionOrMap
            || type.isPrimitiveWrapper
            || type.isEnum
}

private fun isIgnored(type: ClassId): Boolean {
    return isKnownTypes(type)
            || type.isAbstract
            || (type.isInner && !type.isStatic)
}

fun anyObjectValueProvider(idGenerator: IdentityPreservingIdGenerator<Int>) =
    ObjectValueProvider(idGenerator).letIf(UtSettings.fuzzingImplementationOfAbstractClasses) { ovp ->
        ovp.withFallback(AbstractsObjectValueProvider(idGenerator))
    }

class ObjectValueProvider(
    val idGenerator: IdGenerator<Int>,
) : JavaValueProvider {

    override fun accept(type: FuzzedType) = !isIgnored(type.classId)

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        val classId = type.classId
        val constructors = classId.allConstructors
            .filter {
                isAccessible(it.constructor, description.description.packageName)
            }
        constructors.forEach { constructorId ->
            yield(createValue(classId, constructorId, description))
        }
    }

    private fun createValue(classId: ClassId, constructorId: ConstructorId, description: FuzzedDescription): Seed.Recursive<FuzzedType, FuzzedValue> =
        Seed.Recursive(
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

@Suppress("unused")
object NullValueProvider : JavaValueProvider {

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

/**
 * Unlike [NullValueProvider] can generate `null` values at any depth.
 *
 * Intended to be used as a last fallback.
 */
object AnyDepthNullValueProvider : JavaValueProvider {

    override fun accept(type: FuzzedType) = type.classId.isRefType

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequenceOf<Seed<FuzzedType, FuzzedValue>>(Seed.Simple(nullFuzzedValue(classClassId)))
}

/**
 * Finds and create object from implementations of abstract classes or interfaces.
 */
class AbstractsObjectValueProvider(
    val idGenerator: IdGenerator<Int>,
) : JavaValueProvider {

    override fun accept(type: FuzzedType) = type.classId.isRefType && !isKnownTypes(type.classId)

    override fun generate(description: FuzzedDescription, type: FuzzedType) = sequence<Seed<FuzzedType, FuzzedValue>> {
        val t = try {
            Scene.v().getRefType(type.classId.name).sootClass
        } catch (ignore: NoClassDefFoundError) {
            logger.error(ignore) { "Soot may be not initialized" }
            return@sequence
        }
        fun canCreateClass(sc: SootClass): Boolean {
            try {
                if (!sc.isConcrete) return false
                val packageName = sc.packageName
                if (packageName != null) {
                    if (packageName.startsWith("jdk.internal") ||
                        packageName.startsWith("org.utbot") ||
                        packageName.startsWith("sun."))
                    return false
                }
                val isAnonymousClass = sc.name.matches(""".*\$\d+$""".toRegex())
                if (isAnonymousClass) {
                    return false
                }
                val jClass = sc.id.jClass
                return isAccessible(jClass, description.description.packageName) &&
                        jClass.declaredConstructors.any { isAccessible(it, description.description.packageName) } &&
                        jClass.let {
                            // This won't work in case of implementations with generics like `Impl<T> implements A<T>`.
                            // Should be reworked with accurate generic matching between all classes.
                            toFuzzerType(it, description.typeCache).traverseHierarchy(description.typeCache).contains(type)
                        }
            } catch (ignore: Throwable) {
                return false
            }
        }

        val implementations = when {
            t.isInterface -> Scene.v().fastHierarchy.getAllImplementersOfInterface(t).filter(::canCreateClass)
            t.isAbstract -> Scene.v().fastHierarchy.getSubclassesOf(t).filter(::canCreateClass)
            else -> emptyList()
        }
        implementations.shuffled(description.random).take(10).forEach { concrete ->
            yield(Seed.Recursive(
                construct = Routine.Create(listOf(toFuzzerType(concrete.id.jClass, description.typeCache))) {
                    it.first()
                },
                empty = nullRoutine(type.classId)
            ))
        }
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

internal class MethodDescription(
    val name: String,
    val parameterTypes: List<FuzzedType>,
    val method: Method
)

internal fun findAccessibleModifiableFields(description: FuzzedDescription?, classId: ClassId, packageName: String?): List<FieldDescription>  {
    return generateSequence(classId.jClass) { it.superclass }.flatMap { jClass ->
        jClass.declaredFields.map { field ->
            val setterAndGetter = jClass.findPublicSetterGetterIfHasPublicGetter(field, packageName)
            FieldDescription(
                name = field.name,
                type = if (description != null) toFuzzerType(
                    field.genericType,
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

internal fun findMethodsToModifyWith(
    description: FuzzedDescription,
    valueClassId: ClassId,
    classUnderTest: ClassId,
    ): List<MethodDescription> {
    val packageName = description.description.packageName

    val methodUnderTestName = description.description.name.substringAfter(description.description.className + ".")
    val modifyingMethods = findModifyingMethodNames(methodUnderTestName, valueClassId, classUnderTest)
    return valueClassId.jClass.methods.mapNotNull { method ->
        if (isAccessible(method, packageName)) {
            if (method.name !in modifyingMethods) return@mapNotNull null
            if (method.genericParameterTypes.any { it is TypeVariable<*> }) return@mapNotNull null

            val parameterTypes =
                method
                    .parameterTypes
                    .map { toFuzzerType(it, description.typeCache) }

            MethodDescription(
                name = method.name,
                parameterTypes = parameterTypes,
                method = method
            )
        } else null
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

private fun findModifyingMethodNames(
    methodUnderTestName: String,
    valueClassId: ClassId,
    classUnderTest: ClassId,
    ) : Set<String> =
    UtBotFieldsModificatorsSearcher(fieldInvolvementMode = FieldInvolvementMode.ReadAndWrite)
        .let { searcher ->
            searcher.update(setOf(valueClassId, classUnderTest))
            val modificatorsToFields = searcher.getModificatorToFields(analysisMode = AnalysisMode.Methods)

            modificatorsToFields[methodUnderTestName]?.let { fieldsModifiedByMut ->
                modificatorsToFields
                    .mapNotNull { (methodName, fieldSet) ->
                        val relevantFields = if (UtSettings.tryMutateOtherClassesFieldsWithMethods) {
                            fieldsModifiedByMut
                        } else {
                            fieldsModifiedByMut
                                .filter { field -> field.declaringClass == classUnderTest }
                                .toSet()
                        }

                        val methodIsModifying = fieldSet.intersect(relevantFields).isNotEmpty()
                                && methodName != methodUnderTestName
                        if (methodIsModifying) methodName else null
                    }
                    .toSet()
            }
                ?: setOf()
        }

private fun isNotPrivateOrProtected(modifiers: Int): Boolean {
    val hasAnyAccessModifier = Modifier.isPrivate(modifiers) || Modifier.isProtected(modifiers)
    return !hasAnyAccessModifier
}
