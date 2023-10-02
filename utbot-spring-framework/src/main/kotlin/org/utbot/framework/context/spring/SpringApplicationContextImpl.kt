package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.common.dynamicPropertiesOf
import org.utbot.common.isAbstract
import org.utbot.common.isStatic
import org.utbot.common.withValue
import org.utbot.external.api.UtBotSpringApi
import org.utbot.framework.codegen.generator.AbstractCodeGenerator
import org.utbot.framework.codegen.generator.CodeGeneratorParams
import org.utbot.framework.codegen.generator.SpringCodeGenerator
import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.context.TypeReplacer
import org.utbot.framework.context.custom.CoverageFilteringConcreteExecutionContext
import org.utbot.framework.context.custom.RerunningConcreteExecutionContext
import org.utbot.framework.context.custom.useMocks
import org.utbot.framework.context.utils.transformInstrumentationFactory
import org.utbot.framework.context.utils.transformJavaFuzzingContext
import org.utbot.framework.context.utils.transformValueProvider
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.SpringTestType
import org.utbot.framework.plugin.api.util.SpringModelUtils.entityClassIds
import org.utbot.framework.plugin.api.util.allSuperTypes
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzing.spring.JavaLangObjectValueProvider
import org.utbot.fuzzing.spring.FuzzedTypeFlag
import org.utbot.fuzzing.spring.addProperties
import org.utbot.fuzzing.spring.decorators.replaceTypes
import org.utbot.fuzzing.spring.properties
import org.utbot.fuzzing.spring.unit.InjectMockValueProvider
import org.utbot.fuzzing.toFuzzerType
import org.utbot.instrumentation.instrumentation.execution.RemovingConstructFailsUtExecutionInstrumentation

class SpringApplicationContextImpl private constructor(
    private val delegateContext: ApplicationContext,
    override val beanDefinitions: List<BeanDefinitionData>,
    private val springTestType: SpringTestType,
    override val springSettings: SpringSettings,
): ApplicationContext by delegateContext, SpringApplicationContext {
    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Used internally by UtBot to create an instance of [SpringApplicationContextImpl]
         * when [beanDefinitions] are already known.
         *
         * NOTE: Bean definitions defined in config from [springSettings] are IGNORED.
         *
         * API users should use [UtBotSpringApi.createSpringApplicationContext]
         */
        fun internalCreate(
            delegateContext: ApplicationContext,
            beanDefinitions: List<BeanDefinitionData>,
            springTestType: SpringTestType,
            springSettings: SpringSettings,
        ) = SpringApplicationContextImpl(delegateContext, beanDefinitions, springTestType, springSettings)
    }

    private object ReplacedFuzzedTypeFlag : FuzzedTypeFlag

    override val typeReplacer: TypeReplacer = SpringTypeReplacer(delegateContext.typeReplacer, this)
    override val nonNullSpeculator: NonNullSpeculator = SpringNonNullSpeculator(delegateContext.nonNullSpeculator, this)

    override var concreteContextLoadingResult: ConcreteContextLoadingResult? = null

    override fun createConcreteExecutionContext(
        fullClasspath: String,
        classpathWithoutDependencies: String
    ): ConcreteExecutionContext {
        var delegateConcreteExecutionContext = delegateContext.createConcreteExecutionContext(
            fullClasspath,
            classpathWithoutDependencies
        ).transformValueProvider { valueProvider ->
            valueProvider.with(JavaLangObjectValueProvider(
                classesToTryUsingAsJavaLangObject = listOf(objectClassId, classUnderTest)
            ))
        }

        // to avoid filtering out all coverage, we only filter
        // coverage when `classpathWithoutDependencies` is provided
        // (e.g. when we are launched from IDE plugin)
        if (classpathWithoutDependencies.isNotEmpty())
            delegateConcreteExecutionContext = CoverageFilteringConcreteExecutionContext(
                delegateContext = delegateConcreteExecutionContext,
                classpathToIncludeCoverageFrom = classpathWithoutDependencies,
                annotationsToIgnoreCoverage = entityClassIds.toSet(),
                keepOriginalCoverageOnEmptyFilteredCoverage = true
            )

        return when (springTestType) {
            SpringTestType.UNIT_TEST -> delegateConcreteExecutionContext.transformJavaFuzzingContext { fuzzingContext ->
                fuzzingContext
                    .useMocks { type ->
                        ReplacedFuzzedTypeFlag !in type.properties &&
                                mockStrategy.eligibleToMock(
                                    classToMock = type.classId,
                                    classUnderTest = fuzzingContext.classUnderTest
                                )
                    }
                    .transformValueProvider { origValueProvider ->
                        InjectMockValueProvider(
                            idGenerator = fuzzingContext.idGenerator,
                            classUnderTest = fuzzingContext.classUnderTest,
                            isFieldNonNull = { fieldId ->
                                nonNullSpeculator.speculativelyCannotProduceNullPointerException(fieldId, classUnderTest)
                            },
                        )
                            .withFallback(origValueProvider)
                            .replaceTypes { description, type ->
                                typeReplacer.replaceTypeIfNeeded(type.classId)
                                    ?.let { replacementClassId ->
                                        // TODO infer generic type of replacement
                                        val replacement =
                                            if (type.classId == replacementClassId) type
                                            else toFuzzerType(replacementClassId.jClass, description.typeCache)
                                        replacement.addProperties(
                                            dynamicPropertiesOf(ReplacedFuzzedTypeFlag.withValue(Unit))
                                        )
                                    } ?: type
                            }
                    }
            }
            SpringTestType.INTEGRATION_TEST ->
                RerunningConcreteExecutionContext(
                    SpringIntegrationTestConcreteExecutionContext(
                        delegateConcreteExecutionContext,
                        classpathWithoutDependencies,
                        springApplicationContext = this
                    )
                )
        }.transformInstrumentationFactory { delegateInstrumentationFactory ->
            RemovingConstructFailsUtExecutionInstrumentation.Factory(delegateInstrumentationFactory)
        }
    }

    override fun createCodeGenerator(params: CodeGeneratorParams): AbstractCodeGenerator =
        // TODO decorate original `delegateContext.createCodeGenerator(params)`
        SpringCodeGenerator(
            springTestType = springTestType,
            springSettings = springSettings,
            concreteContextLoadingResult = concreteContextLoadingResult,
            params = params,
        )

    override fun getBeansAssignableTo(classId: ClassId): List<BeanDefinitionData> = beanDefinitions.filter { beanDef ->
        // some bean classes may fail to load
        runCatching {
            val beanClass = ClassId(beanDef.beanTypeName).jClass
            classId.jClass.isAssignableFrom(beanClass)
        }.getOrElse { false }
    }

    // Classes representing concrete types that are actually used in Spring application
    override val injectedTypes: Set<ClassId>
        get() {
            if (!areAllInjectedSuperTypesInitialized) {
                for (beanTypeName in beanDefinitions.map { it.beanTypeName }) {
                    try {
                        val beanClass = utContext.classLoader.loadClass(beanTypeName)
                        if (!beanClass.isAbstract && !beanClass.isInterface &&
                            !beanClass.isLocalClass && (!beanClass.isMemberClass || beanClass.isStatic)) {
                            _injectedTypes += beanClass.id
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
                areAllInjectedSuperTypesInitialized = true
            }

            return _injectedTypes
        }

    override val allInjectedSuperTypes: Set<ClassId>
        get() {
            if (!areInjectedTypesInitialized) {
                _allInjectedSuperTypes = injectedTypes.flatMap { it.allSuperTypes() }.toSet()
                areInjectedTypesInitialized = true
            }

            return _allInjectedSuperTypes
        }

    // the following properties help to imitate `by lazy` behaviour, do not use them directly
    // (we can't use actual `by lazy` because communication via RD breaks it)
    private var _allInjectedSuperTypes: Set<ClassId> = emptySet()
    private var areAllInjectedSuperTypesInitialized : Boolean = false

    private val _injectedTypes = mutableSetOf<ClassId>()
    private var areInjectedTypesInitialized: Boolean = false
}