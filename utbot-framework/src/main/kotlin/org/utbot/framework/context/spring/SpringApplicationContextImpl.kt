package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.common.isAbstract
import org.utbot.common.isStatic
import org.utbot.framework.context.ApplicationContext
import org.utbot.framework.context.ConcreteExecutionContext
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.context.TypeReplacer
import org.utbot.framework.plugin.api.BeanDefinitionData
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.SpringTestType
import org.utbot.framework.plugin.api.util.allSuperTypes
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext

class SpringApplicationContextImpl(
    private val delegateContext: ApplicationContext,
    override val beanDefinitions: List<BeanDefinitionData> = emptyList(),
    override val springTestType: SpringTestType,
    override val springSettings: SpringSettings,
): ApplicationContext by delegateContext, SpringApplicationContext {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override val typeReplacer: TypeReplacer = SpringTypeReplacer(delegateContext.typeReplacer, this)
    override val nonNullSpeculator: NonNullSpeculator = SpringNonNullSpeculator(delegateContext.nonNullSpeculator, this)

    override var springContextLoadingResult: SpringContextLoadingResult? = null

    override fun createConcreteExecutionContext(
        fullClasspath: String,
        classpathWithoutDependencies: String
    ): ConcreteExecutionContext = SpringConcreteExecutionContext(
        delegateContext.createConcreteExecutionContext(fullClasspath, classpathWithoutDependencies),
        this
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