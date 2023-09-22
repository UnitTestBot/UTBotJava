package org.utbot.framework.context.spring

import mu.KotlinLogging
import org.utbot.framework.context.NonNullSpeculator
import org.utbot.framework.context.spring.SpringNonNullSpeculator.InjectionKind.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.util.SpringModelUtils.autowiredClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.componentClassId
import org.utbot.framework.plugin.api.util.SpringModelUtils.injectClassIds
import org.utbot.framework.plugin.api.util.allDeclaredFieldIds
import org.utbot.framework.plugin.api.util.executable
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.jField
import org.utbot.framework.plugin.api.util.jFieldOrNull
import org.utbot.modifications.ExecutableAnalyzer
import soot.SootField
import java.lang.reflect.AnnotatedElement

class SpringNonNullSpeculator(
    private val delegateNonNullSpeculator: NonNullSpeculator,
    private val springApplicationContext: SpringApplicationContext
) : NonNullSpeculator {
    private val executableAnalyzer = ExecutableAnalyzer()

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun speculativelyCannotProduceNullPointerException(
        field: SootField,
        classUnderTest: ClassId,
    ): Boolean =
         ((field.jFieldOrNull?.let { it.fieldId in getInjectedFields(it.declaringClass.id) } ?: false) ||
            delegateNonNullSpeculator.speculativelyCannotProduceNullPointerException(field, classUnderTest))

    override fun speculativelyCannotProduceNullPointerException(field: FieldId, classUnderTest: ClassId): Boolean =
        (field in getInjectedFields(field.declaringClass) ||
                delegateNonNullSpeculator.speculativelyCannotProduceNullPointerException(field, classUnderTest))

    private val injectedFieldsCache = mutableMapOf<ClassId, Set<FieldId>>()

    /**
     * As described in [this issue](https://github.com/UnitTestBot/UTBotJava/issues/2589) we should consider
     * `FieldType fieldName` to be non-`null`, iff one of the following statements holds:
     *   - the field itself is annotated with `@Autowired(required=true)`, `@Autowired` or `@Inject`
     *   - all the following statements hold:
     *     - one of the following statements hold:
     *       - there’s a constructor or a method annotated with `@Autowired(required=true)`, `@Autowired` or `@Inject`
     *       - there’s only one constructor defined in the class, said constructor is not annotated with
     *         `@Autowired(required=false)`, while the class itself is annotated with `@Component`-like annotation
     *         (either `@Component` itself or other annotation that is itself annotated with `@Component`)
     *     - said constructor/method accepts a parameter of type `FieldType` (or its subtype)
     *     - said parameter is not annotated with `@Nullable` nor with `@Autowired(required=false)`
     *     - said constructor/method contains an assignment of said parameter to `fieldName`
     */
    private fun getInjectedFields(classId: ClassId): Set<FieldId> = injectedFieldsCache.getOrPut(classId) {
         try {
             (classId.allDeclaredFieldIds.filter { it.jField.getInjectionAnnotationKind() == INJECTED_AND_REQUIRED } +
                     classId.getInjectingExecutables().flatMap { injectingExecutable ->
                         executableAnalyzer.analyze(injectingExecutable).params
                             .map { (paramIdx, fieldId) -> injectingExecutable.executable.parameters[paramIdx] to fieldId }
                             .filter { (param, fieldId) ->
                                 fieldId.type.jClass.isAssignableFrom(param.type) &&
                                         param.annotations.none {
                                             it.annotationClass.simpleName?.endsWith("Nullable") ?: false
                                         } &&
                                         param.getInjectionAnnotationKind() != INJECTED_BUT_NOT_REQUIRED
                             }
                             .map { (_, fieldId) -> fieldId }
                     }).toSet().also {
                         logger.info { "Injected fields for $classId: $it" }
                    }
         } catch (e: Throwable) {
            logger.warn(e) { "Failed to determine injected fields for class $classId" }
            emptySet()
         }
    }

    private fun ClassId.getInjectingExecutables(): Sequence<ExecutableId> {
        return (allConstructors + allMethods).filter {
            it.executable.getInjectionAnnotationKind() == INJECTED_AND_REQUIRED
        } + listOfNotNull(allConstructors.singleOrNull()?.takeIf { constructor ->
            isBeanDefiningClass() && constructor.executable.getInjectionAnnotationKind() == NOT_INJECTED
        })
    }

    private fun ClassId.isBeanDefiningClass(): Boolean =
        this in springApplicationContext.injectedTypes || jClass.annotations.any { it.isComponentLike() }

    private fun Annotation.isComponentLike() =
        annotationClass.id == componentClassId ||
                annotationClass.annotations.any { it.annotationClass.id == componentClassId }

    private fun AnnotatedElement.getInjectionAnnotationKind(): InjectionKind {
        if(annotations.any { it.annotationClass.id in injectClassIds })
            return INJECTED_AND_REQUIRED
        val autowiredAnnotation = annotations.firstOrNull { it.annotationClass.id == autowiredClassId }
            ?: return NOT_INJECTED
        return if (autowiredAnnotation.annotationClass.java.getMethod("required").invoke(autowiredAnnotation) as Boolean)
            INJECTED_AND_REQUIRED
        else
            INJECTED_BUT_NOT_REQUIRED
    }

    private enum class InjectionKind {
        NOT_INJECTED,
        INJECTED_BUT_NOT_REQUIRED,
        INJECTED_AND_REQUIRED,
    }
}