package org.utbot.framework.context

import mu.KotlinLogging
import org.utbot.common.isAbstract
import org.utbot.common.isStatic
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringCodeGenerationContext
import org.utbot.framework.plugin.api.SpringContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.SpringTestType
import org.utbot.framework.plugin.api.TypeReplacementMode
import org.utbot.framework.plugin.api.UtError
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.id
import org.utbot.framework.plugin.api.isAbstractType
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.allSuperTypes
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.isSubtypeOf
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import soot.RefType
import soot.SootField

/**
 * Data we get from Spring application context
 * to manage engine and code generator behaviour.
 *
 * @param beanDefinitions describes bean definitions (bean name, type, some optional additional data)
 * @param shouldUseImplementors describes it we want to replace interfaces with injected types or not
 */
// TODO move this class to utbot-framework so we can use it as abstract factory
//  to get rid of numerous `when`s and polymorphically create things like:
//    - Instrumentation<UtConcreteExecutionResult>
//    - FuzzedType (to get rid of thisInstanceFuzzedTypeWrapper)
//    - JavaValueProvider
//    - CgVariableConstructor
//    - CodeGeneratorResult (generateForSpringClass)
//  Right now this refactoring is blocked because some interfaces need to get extracted and moved to utbot-framework-api
//  As an alternative we can just move ApplicationContext itself to utbot-framework
class SpringApplicationContext(
    mockInstalled: Boolean,
    staticsMockingIsConfigured: Boolean,
    val beanDefinitions: List<BeanDefinitionData> = emptyList(),
    private val shouldUseImplementors: Boolean,
    override val springTestType: SpringTestType,
    override val springSettings: SpringSettings,
): ApplicationContext(mockInstalled, staticsMockingIsConfigured), SpringCodeGenerationContext {

    override var springContextLoadingResult: SpringContextLoadingResult? = null

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private var areInjectedClassesInitialized : Boolean = false
    private var areAllInjectedTypesInitialized: Boolean = false

    // Classes representing concrete types that are actually used in Spring application
    private val springInjectedClasses: Set<ClassId>
        get() {
            if (!areInjectedClassesInitialized) {
                for (beanTypeName in beanDefinitions.map { it.beanTypeName }) {
                    try {
                        val beanClass = utContext.classLoader.loadClass(beanTypeName)
                        if (!beanClass.isAbstract && !beanClass.isInterface &&
                            !beanClass.isLocalClass && (!beanClass.isMemberClass || beanClass.isStatic)) {
                            springInjectedClassesStorage += beanClass.id
                        }
                    } catch (e: Throwable) {
                        // For some Spring beans (e.g. with anonymous classes)
                        // it is possible to have problems with classes loading.
                        when (e) {
                            is ClassNotFoundException, is NoClassDefFoundError, is IllegalAccessError ->
                                logger.warn { "Failed to load bean class for $beanTypeName (${e.message})" }

                            else -> throw e
                        }
                    }
                }

                // This is done to be sure that this storage is not empty after the first class loading iteration.
                // So, even if all loaded classes were filtered out, we will not try to load them again.
                areInjectedClassesInitialized = true
            }

            return springInjectedClassesStorage
        }

    private val allInjectedTypes: Set<ClassId>
        get() {
            if (!areAllInjectedTypesInitialized) {
                allInjectedTypesStorage = springInjectedClasses.flatMap { it.allSuperTypes() }.toSet()
                areAllInjectedTypesInitialized = true
            }

            return allInjectedTypesStorage
        }

    // imitates `by lazy` (we can't use actual `by lazy` because communication via RD breaks it)
    private var allInjectedTypesStorage: Set<ClassId> = emptySet()

    // This is a service field to model the lazy behavior of [springInjectedClasses].
    // Do not call it outside the getter.
    //
    // Actually, we should just call [springInjectedClasses] with `by lazy`, but  we had problems
    // with a strange `kotlin.UNINITIALIZED_VALUE` in `speculativelyCannotProduceNullPointerException` method call.
    private val springInjectedClassesStorage = mutableSetOf<ClassId>()

    override val typeReplacementMode: TypeReplacementMode =
        if (shouldUseImplementors) TypeReplacementMode.KnownImplementor else TypeReplacementMode.NoImplementors

    /**
     * Replaces an interface type with its implementor type
     * if there is the unique implementor in bean definitions.
     */
    override fun replaceTypeIfNeeded(type: RefType): ClassId? =
        if (type.isAbstractType) {
            springInjectedClasses.singleOrNull { it.isSubtypeOf(type.id) }
        } else {
            null
        }

    /**
     * In Spring applications we can mark as speculatively not null
     * fields if they are mocked and injecting into class under test so on.
     *
     * Fields are not mocked if their actual type is obtained from [springInjectedClasses].
     *
     */
    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean = field.fieldId in classUnderTest.allDeclaredFieldIds && field.type.classId !in allInjectedTypes

    override fun preventsFurtherTestGeneration(): Boolean =
        super.preventsFurtherTestGeneration() || springContextLoadingResult?.contextLoaded == false

    override fun getErrors(): List<UtError> =
        springContextLoadingResult?.exceptions?.map { exception ->
            UtError(
                "Failed to load Spring application context",
                exception
            )
        }.orEmpty() + super.getErrors()

    fun getBeansAssignableTo(classId: ClassId): List<BeanDefinitionData> = beanDefinitions.filter { beanDef ->
        // some bean classes may fail to load
        runCatching {
            val beanClass = ClassId(beanDef.beanTypeName).jClass
            classId.jClass.isAssignableFrom(beanClass)
        }.getOrElse { false }
    }
}