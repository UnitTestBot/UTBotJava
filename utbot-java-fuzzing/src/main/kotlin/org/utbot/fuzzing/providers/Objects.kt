package org.utbot.fuzzing.providers

import mu.KotlinLogging
import org.utbot.common.isStatic
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.util.*
import org.utbot.fuzzer.*
import org.utbot.fuzzing.*
import org.utbot.fuzzing.utils.hex
import soot.Scene
import soot.SootClass
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier

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
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {

    override fun accept(type: FuzzedType) = !isIgnored(type.classId)

    override fun generate(
        description: FuzzedDescription,
        type: FuzzedType
    ) = sequence {
        val classId = type.classId
        findAccessibleCreators(description, classId).forEach { creatorExecutableId ->
            yield(createValue(classId, creatorExecutableId, description))
        }
    }

    private fun createValue(classId: ClassId, creatorExecutableId: ExecutableId, description: FuzzedDescription): Seed.Recursive<FuzzedType, FuzzedValue> {
        return Seed.Recursive(
            construct = Routine.Create(creatorExecutableId.executable.genericParameterTypes.map {
                toFuzzerType(it, description.typeCache)
            }) { values ->
                val id = idGenerator.createId()
                UtAssembleModel(
                    id = id,
                    classId = classId,
                    modelName = "${creatorExecutableId.classId.name}.${creatorExecutableId.signature}#" + id.hex(),
                    instantiationCall = UtExecutableCallModel(
                        null,
                        creatorExecutableId,
                        values.map { it.model }),
                    modificationsChainProvider = { mutableListOf() }
                ).fuzzed {
                    summary = "%var% = ${when (creatorExecutableId) {
                        is ConstructorId -> classId.simpleName
                        is MethodId -> creatorExecutableId.simpleNameWithClass
                    }}(${creatorExecutableId.parameters.joinToString { it.simpleName }})"
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

/**
 * Finds and create object from implementations of abstract classes or interfaces.
 */
class AbstractsObjectValueProvider(
    val idGenerator: IdGenerator<Int>,
) : ValueProvider<FuzzedType, FuzzedValue, FuzzedDescription> {

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
                        findAccessibleCreators(description, jClass.id).any()
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

private fun findAccessibleCreators(description: FuzzedDescription, classId: ClassId): Sequence<ExecutableId> =
    (classId.allConstructors + (classId.jClass.methods
                .filter { it.isStatic && it.returnType.id.isSubtypeOf(classId) }
                .map { it.executableId })
    ).filter { isAccessible(it.executable, description.description.packageName) }

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