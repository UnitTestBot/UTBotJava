package org.utbot.framework.codegen.model.tree

import org.utbot.framework.codegen.Import
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.codegen.model.util.CgExceptionHandler
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.jcdb.api.ClassId

interface CgBuilder<T : CgElement> {
    fun build(): T
}

// Code entities

class CgTestClassFileBuilder : CgBuilder<CgTestClassFile> {
    val imports: MutableList<Import> = mutableListOf()
    lateinit var testClass: CgTestClass
    lateinit var testsGenerationReport: TestsGenerationReport

    override fun build() = CgTestClassFile(imports, testClass, testsGenerationReport)
}

fun buildTestClassFile(init: CgTestClassFileBuilder.() -> Unit) = CgTestClassFileBuilder().apply(init).build()

class CgTestClassBuilder : CgBuilder<CgTestClass> {
    lateinit var id: ClassId
    val annotations: MutableList<CgAnnotation> = mutableListOf()
    var superclass: ClassId? = null
    val interfaces: MutableList<ClassId> = mutableListOf()
    var isStatic: Boolean = false
    var isNested: Boolean = false
    lateinit var body: CgTestClassBody

    override fun build() = CgTestClass(id, annotations, superclass, interfaces, body, isStatic, isNested)
}

fun buildTestClass(init: CgTestClassBuilder.() -> Unit) = CgTestClassBuilder().apply(init).build()

class CgTestClassBodyBuilder : CgBuilder<CgTestClassBody> {
    val testMethodRegions: MutableList<CgExecutableUnderTestCluster> = mutableListOf()

    val dataProvidersAndUtilMethodsRegion: MutableList<CgRegion<CgElement>> = mutableListOf()

    val nestedClassRegions: MutableList<CgRegion<CgTestClass>> = mutableListOf()

    override fun build() = CgTestClassBody(testMethodRegions, dataProvidersAndUtilMethodsRegion, nestedClassRegions)
}

fun buildTestClassBody(init: CgTestClassBodyBuilder.() -> Unit) = CgTestClassBodyBuilder().apply(init).build()

// Methods

interface CgMethodBuilder<T : CgMethod> : CgBuilder<T> {
    val name: String
    val returnType: ClassId
    val parameters: List<CgParameterDeclaration>
    val statements: List<CgStatement>
    val exceptions: Set<ClassId>
    val annotations: List<CgAnnotation>
    val documentation: CgDocumentationComment
}

class CgTestMethodBuilder : CgMethodBuilder<CgTestMethod> {
    override lateinit var name: String
    override val returnType: ClassId = voidClassId
    override lateinit var parameters: List<CgParameterDeclaration>
    override lateinit var statements: List<CgStatement>
    override val exceptions: MutableSet<ClassId> = mutableSetOf()
    override val annotations: MutableList<CgAnnotation> = mutableListOf()
    lateinit var methodType: CgTestMethodType
    override var documentation: CgDocumentationComment = CgDocumentationComment(emptyList())

    override fun build() = CgTestMethod(
        name,
        returnType.type(false),
        parameters,
        statements,
        exceptions,
        annotations,
        methodType,
        documentation,
    )
}

fun buildTestMethod(init: CgTestMethodBuilder.() -> Unit) = CgTestMethodBuilder().apply(init).build()

class CgErrorTestMethodBuilder : CgMethodBuilder<CgErrorTestMethod> {
    override lateinit var name: String
    override val returnType: ClassId = voidClassId
    override val parameters: List<CgParameterDeclaration> = emptyList()
    override lateinit var statements: List<CgStatement>
    override val exceptions: Set<ClassId> = emptySet()
    override val annotations: List<CgAnnotation> = emptyList()
    override var documentation: CgDocumentationComment = CgDocumentationComment(emptyList())

    override fun build() = CgErrorTestMethod(name, statements, documentation)
}

fun buildErrorTestMethod(init: CgErrorTestMethodBuilder.() -> Unit) = CgErrorTestMethodBuilder().apply(init).build()

class CgParameterizedTestDataProviderBuilder : CgMethodBuilder<CgParameterizedTestDataProviderMethod> {
    override lateinit var name: String

    override lateinit var returnType: ClassId
    override val parameters: List<CgParameterDeclaration> = mutableListOf()
    override lateinit var statements: List<CgStatement>
    override val annotations: MutableList<CgAnnotation> = mutableListOf()
    override val exceptions: MutableSet<ClassId> = mutableSetOf()
    override var documentation: CgDocumentationComment = CgDocumentationComment(emptyList())

    override fun build() = CgParameterizedTestDataProviderMethod(name, statements, returnType.type(), annotations, exceptions)
}

fun buildParameterizedTestDataProviderMethod(
    init: CgParameterizedTestDataProviderBuilder.() -> Unit
) = CgParameterizedTestDataProviderBuilder().apply(init).build()

// Variable declaration

class CgDeclarationBuilder : CgBuilder<CgDeclaration> {
    lateinit var variableType: CgClassType
    lateinit var variableName: String
    var initializer: CgExpression? = null
    var isMutable: Boolean = false

    override fun build() = CgDeclaration(variableType, variableName, initializer, isMutable)
}

fun buildDeclaration(init: CgDeclarationBuilder.() -> Unit) = CgDeclarationBuilder().apply(init).build()

// Variable assignment
class CgAssignmentBuilder : CgBuilder<CgAssignment> {
    lateinit var lValue: CgExpression
    lateinit var rValue: CgExpression

    override fun build() = CgAssignment(lValue, rValue)
}

fun buildAssignment(init: CgAssignmentBuilder.() -> Unit) = CgAssignmentBuilder().apply(init).build()

class CgTryCatchBuilder : CgBuilder<CgTryCatch> {
    lateinit var statements: List<CgStatement>
    private val handlers: MutableList<CgExceptionHandler> = mutableListOf()
    var finally: List<CgStatement>? = null
    var resources: List<CgDeclaration>? = null

    override fun build(): CgTryCatch = CgTryCatch(statements, handlers, finally, resources)
}

fun buildTryCatch(init: CgTryCatchBuilder.() -> Unit): CgTryCatch = CgTryCatchBuilder().apply(init).build()

// Loops
interface CgLoopBuilder<T : CgLoop> : CgBuilder<T> {
    val condition: CgExpression
    val statements: List<CgStatement>
}

class CgForLoopBuilder : CgLoopBuilder<CgForLoop> {
    lateinit var initialization: CgDeclaration
    override lateinit var condition: CgExpression
    lateinit var update: CgStatement
    override lateinit var statements: List<CgStatement>

    override fun build() = CgForLoop(initialization, condition, update, statements)
}

fun buildForLoop(init: CgForLoopBuilder.() -> Unit) = CgForLoopBuilder().apply(init).build()

class CgWhileLoopBuilder : CgLoopBuilder<CgWhileLoop> {
    override lateinit var condition: CgExpression
    override val statements: MutableList<CgStatement> = mutableListOf()

    override fun build() = CgWhileLoop(condition, statements)
}

fun buildWhileLoop(init: CgWhileLoopBuilder.() -> Unit) = CgWhileLoopBuilder().apply(init).build()

class CgDoWhileLoopBuilder : CgLoopBuilder<CgDoWhileLoop> {
    override lateinit var condition: CgExpression
    override val statements: MutableList<CgStatement> = mutableListOf()

    override fun build() = CgDoWhileLoop(condition, statements)
}

fun buildDoWhileLoop(init: CgDoWhileLoopBuilder.() -> Unit) = CgDoWhileLoopBuilder().apply(init).build()

class CgForEachLoopBuilder : CgLoopBuilder<CgForEachLoop> {
    override lateinit var condition: CgExpression
    override lateinit var statements: List<CgStatement>
    lateinit var iterable: CgReferenceExpression

    override fun build(): CgForEachLoop = CgForEachLoop(condition, statements, iterable)
}

fun buildCgForEachLoop(init: CgForEachLoopBuilder.() -> Unit): CgForEachLoop =
    CgForEachLoopBuilder().apply(init).build()
