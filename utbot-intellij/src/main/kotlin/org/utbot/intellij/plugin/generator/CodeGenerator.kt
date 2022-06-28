package org.utbot.intellij.plugin.generator

import org.utbot.framework.TestSelectionStrategyType
import org.utbot.framework.UtSettings
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.UtBotTestCaseGenerator
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.intellij.plugin.settings.Settings
import org.utbot.summary.summarize
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.utbot.engine.UtBotSymbolicEngine
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter.Kind
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaType

val logger = Logger.getInstance(CodeGenerator::class.java)

class CodeGenerator(
    private val searchDirectory: Path,
    private val mockStrategy: MockStrategyApi,
    project: Project,
    private val chosenClassesToMockAlways: Set<ClassId>,
    buildDir: String,
    classpath: String,
    pluginJarsPath: String,
    configureEngine: (UtBotSymbolicEngine) -> Unit = {},
    isCanceled: () -> Boolean,
) {
    init {
        UtSettings.testMinimizationStrategyType = TestSelectionStrategyType.COVERAGE_STRATEGY
    }

    val generator = (project.service<Settings>().testCasesGenerator as UtBotTestCaseGenerator).apply {
        init(Paths.get(buildDir), classpath, pluginJarsPath, configureEngine, isCanceled)
    }

    private val settingsState = project.service<Settings>().state

    fun executions(method: UtMethod<*>) = generator.generate(method, mockStrategy).summarize(searchDirectory)

    fun generateForSeveralMethods(methods: List<UtMethod<*>>, timeout:Long = UtSettings.utBotGenerationTimeoutInMillis): List<UtTestCase> {
        logger.info("Tests generating parameters $settingsState")

        return generator
            .generateForSeveralMethods(methods, mockStrategy, chosenClassesToMockAlways, methodsGenerationTimeout = timeout)
            .map { it.summarize(searchDirectory) }
    }
}

fun findMethodsInClassMatchingSelected(clazz: KClass<*>, selectedMethods: List<MemberInfo>): List<UtMethod<*>> {
    val selectedSignatures = selectedMethods.map { it.signature() }
    return clazz.functions
        .sortedWith(compareBy { selectedSignatures.indexOf(it.signature()) })
        .filter { it.signature().normalized() in selectedSignatures }
        .map { UtMethod(it, clazz) }
}

fun findMethodParams(clazz: KClass<*>, methods: List<MemberInfo>): Map<UtMethod<*>, List<String>> {
    val bySignature = methods.associate { it.signature() to it.paramNames() }
    return clazz.functions.mapNotNull { method ->
        bySignature[method.signature()]?.let { params ->
            UtMethod(method, clazz) to params
        }
    }.toMap()
}

private fun MemberInfo.signature(): Signature =
    (this.member as PsiMethod).signature()

private fun MemberInfo.paramNames(): List<String> =
    (this.member as PsiMethod).parameterList.parameters.map { it.name }

private fun PsiMethod.signature() =
    Signature(this.name, this.parameterList.parameters.map {
        it.type.canonicalText
            .replace("...", "[]") //for PsiEllipsisType
            .replace(",", ", ") // to fix cases like Pair<String,String> -> Pair<String, String>
    })

private fun KFunction<*>.signature() =
    Signature(this.name, this.parameters.filter { it.kind == Kind.VALUE }.map { it.type.javaType.typeName })

data class Signature(val name: String, val parameterTypes: List<String?>) {

    fun normalized() = this.copy(
        parameterTypes = parameterTypes.map {
            it?.replace("$", ".") // normalize names of nested classes
        }
    )
}