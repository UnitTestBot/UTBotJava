package org.utbot.framework.codegen.model.constructor

import org.utbot.framework.codegen.model.constructor.context.CgContextOwner
import org.utbot.framework.codegen.model.tree.CgAnnotation
import org.utbot.framework.codegen.model.tree.CgMethod
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.codegen.model.tree.CgTestClass

/**
 * This class stores context information needed to build [CgTestClass].
 * Should only be used in [CgContextOwner].
 */
internal data class TestClassContext(
    // set of interfaces that the test class must inherit
    val collectedTestClassInterfaces: MutableSet<ClassId> = mutableSetOf(),

    // set of annotations of the test class
    val collectedTestClassAnnotations: MutableSet<CgAnnotation> = mutableSetOf(),

    // list of data provider methods that test class must implement
    val cgDataProviderMethods: MutableList<CgMethod> = mutableListOf(),
) {
    // test class superclass (if needed)
    var testClassSuperclass: ClassId? = null
        set(value) {
            // Assigning a value to the testClassSuperclass when it is already non-null
            // means that we need the test class to have more than one superclass
            // which is impossible in Java and Kotlin.
            require(value == null || field == null) { "It is impossible for the test class to have more than one superclass" }
            field = value
        }

    fun clear() {
        collectedTestClassAnnotations.clear()
        collectedTestClassInterfaces.clear()
        cgDataProviderMethods.clear()
        testClassSuperclass = null
    }
}