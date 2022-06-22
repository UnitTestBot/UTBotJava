package org.utbot.engine

import org.utbot.api.annotation.UtClassMock
import org.utbot.api.annotation.UtConstructorMock
import org.utbot.api.annotation.UtInternalUsage
import org.utbot.framework.plugin.api.util.bytecodeSignature
import soot.SootClass
import soot.SootField
import soot.SootMethod
import soot.tagkit.AnnotationIntElem
import soot.tagkit.Host
import soot.tagkit.VisibilityAnnotationTag
import soot.tagkit.VisibilityParameterAnnotationTag

/**
 * Returns visibilityAnnotationTag for the host, or null if there is no such tag.
 *
 * @see VisibilityAnnotationTag
 * @see Host
 */
val Host.visibilityAnnotationOrNull
    get() = getTag(ANNOTATIONS_TAG_NAME) as? VisibilityAnnotationTag

/**
 * Returns annotations list for the host.
 *
 * @see VisibilityAnnotationTag
 * @see Host
 */
val Host.annotationsOrNull
    get() = visibilityAnnotationOrNull?.annotations

/**
 * Returns annotations for the parameters of the soot method.
 *
 * @see VisibilityParameterAnnotationTag
 */
val SootMethod.paramAnnotationsOrNull
    get() = run {
        val tag = getTag(PARAMS_ANNOTATION_TAG_NAME) as? VisibilityParameterAnnotationTag
        tag?.visibilityAnnotations
    }

/**
 * Returns true if the param with [paramIndex] of the soot method is marked with one of the notNull annotations
 * represented in the [notNullAnnotationsBytecodeSignatures] set, false otherwise.
 *
 * @see notNullAnnotationsBytecodeSignatures
 */
fun SootMethod.paramHasNotNullAnnotation(paramIndex: Int): Boolean =
    paramAnnotationsOrNull
        ?.get(paramIndex)
        ?.hasNotNullAnnotation() ?: false

/**
 * Returns true if the soot method is marked with one of the notNull annotations
 * represented in the [notNullAnnotationsBytecodeSignatures] set, false otherwise.
 *
 * @see notNullAnnotationsBytecodeSignatures
 */
fun SootMethod.returnValueHasNotNullAnnotation(): Boolean = visibilityAnnotationOrNull?.hasNotNullAnnotation() ?: false

/**
 * Returns true if the tag's annotations contains one of the notNull annotations
 * represented in the [notNullAnnotationsBytecodeSignatures] set, false otherwise.
 *
 * @see notNullAnnotationsBytecodeSignatures
 */
fun VisibilityAnnotationTag.hasNotNullAnnotation(): Boolean = this
    .annotations
    .singleOrNull { it.type in notNullAnnotationsBytecodeSignatures } != null

/**
 * Returns true if the soot field is marked with one of the notNull annotations
 * represented in the [notNullAnnotationsBytecodeSignatures] set, false otherwise.
 *
 * @see notNullAnnotationsBytecodeSignatures
 */
fun SootField.hasNotNullAnnotation(): Boolean = visibilityAnnotationOrNull?.hasNotNullAnnotation() != null

/**
 * Returns [UtClassMock] annotation if it presents in the annotations list, null otherwise.
 */
val SootClass.findMockAnnotationOrNull
    get() = annotationsOrNull?.singleOrNull { it.type == utClassMockBytecodeSignature }

/**
 * Returns true if the class has a [UtInternalUsage] annotation in the annotations list or internalUsage param
 * of the [UtClassMock] set in true.
 */
val SootClass.allMethodsAreInternalMocks
    get() = run {
        val internalUsageMark = findMockAnnotationOrNull
            ?.elems
            ?.singleOrNull { it.name == "internalUsage" } as? AnnotationIntElem
        val value = internalUsageMark?.value
        value == 1 || annotationsOrNull?.singleOrNull { it.type == utInternalUsageBytecodeSignature } != null
    }

/**
 * Returns [UtConstructorMock] annotation if it presents in the annotations list, null otherwise.
 */
val SootMethod.findMockAnnotationOrNull
    get() = annotationsOrNull?.singleOrNull { it.type == utConstructorMockBytecodeSignature }

/**
 * Returns true if the method has a [UtInternalUsage] annotation in the annotations list
 */
val SootMethod.hasInternalMockAnnotation
    get() = annotationsOrNull?.singleOrNull { it.type == utInternalUsageBytecodeSignature } != null

val utClassMockBytecodeSignature = UtClassMock::class.java.bytecodeSignature()
val utConstructorMockBytecodeSignature = UtConstructorMock::class.java.bytecodeSignature()
val utInternalUsageBytecodeSignature = UtInternalUsage::class.java.bytecodeSignature()

/**
 * Set of the known NotNull annotations.
 *
 * They are hardcoded to avoid additional dependencies in the project.
 */
val notNullAnnotationsBytecodeSignatures = setOf(
    "Lorg/jetbrains/annotations/NotNull;",
    "Ljavax/validation/constraints/NotNull;",
    "Ledu/umd/cs/findbugs/annotations/NonNull;",
    "Ljavax/annotation/Nonnull;",
    "Llombok/NonNull;",
)

const val ANNOTATIONS_TAG_NAME = "VisibilityAnnotationTag"
const val PARAMS_ANNOTATION_TAG_NAME = "VisibilityParameterAnnotationTag"