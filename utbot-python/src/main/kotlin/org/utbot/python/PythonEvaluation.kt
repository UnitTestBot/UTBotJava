package org.utbot.python

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.UtModel
import org.utbot.fuzzer.FuzzedValue
import org.utbot.python.code.MemoryDump
import org.utbot.python.code.PythonCodeGenerator
import org.utbot.python.code.PythonObjectDeserializer
import org.utbot.python.code.toPythonTree
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.utils.TemporaryFileManager
import org.utbot.python.utils.getResult
import org.utbot.python.utils.startProcess
import java.io.File

sealed class PythonEvaluationResult

class PythonEvaluationError(
    val status: Int,
    val message: String,
    val stackTrace: List<String>
) : PythonEvaluationResult()

class PythonEvaluationTimeout(
    val message: String = "Timeout"
) : PythonEvaluationResult()

class PythonEvaluationSuccess(
    val coverage: Coverage,
    val isException: Boolean,
    val stateBefore: MemoryDump,
    val stateAfter: MemoryDump,
    val modelListIds: List<String>,
    val result: OutputData,
) : PythonEvaluationResult()

enum class PythonEvaluationStatus(val value: String) {
    SUCCESS("success"),
    FAIL("fail"),
    ARGUMENTS_FAIL("arguments_fail"),
}

data class OutputData(
    val output: PythonTree.PythonTreeNode,
    val type: PythonClassId,
)

data class EvaluationInput(
    val method: PythonMethod,
    val methodArguments: List<UtModel>,
    val directoriesForSysPath: Set<String>,
    val moduleToImport: String,
    val pythonPath: String,
    val timeoutForRun: Long,
    val thisObject: UtModel?,
    val modelList: List<UtModel>,
    val values: List<FuzzedValue>,
    val additionalModulesToImport: Set<String> = emptySet()
)

data class EvaluationProcess(
    val process: Process,
    val fileWithCode: File,
    val fileForOutput: File
)

fun startEvaluationProcess(input: EvaluationInput): EvaluationProcess {
    val fileForOutput = TemporaryFileManager.assignTemporaryFile(
        tag = "out_" + input.method.name + ".py",
        addToCleaner = false
    )
    val coverageDatabasePath = TemporaryFileManager.assignTemporaryFile(
        tag = "coverage_db_" + input.method.name,
        addToCleaner = false,
    )
    val runCode = PythonCodeGenerator.generateRunFunctionCode(
        input.method,
        input.methodArguments,
        input.directoriesForSysPath,
        input.moduleToImport,
        input.additionalModulesToImport,
        fileForOutput.path.replace("\\", "\\\\"),
        coverageDatabasePath.absolutePath.replace("\\", "\\\\")
    )
    val fileWithCode = TemporaryFileManager.createTemporaryFile(
        runCode,
        tag = "run_" + input.method.name + ".py",
        addToCleaner = false
    )
    return EvaluationProcess(
        startProcess(listOf(input.pythonPath, fileWithCode.path)),
        fileWithCode,
        fileForOutput
    )
}

fun calculateCoverage(statements: List<Int>, missedStatements: List<Int>, input: EvaluationInput): Coverage {
    val covered = statements.filter { it !in missedStatements }
    return Coverage(
        coveredInstructions=covered.map {
            Instruction(
                input.method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                input.method.methodSignature(),
                it,
                it.toLong()
            )
        },
        instructionsCount = statements.size.toLong(),
        missedInstructions = missedStatements.map {
            Instruction(
                input.method.containingPythonClassId?.name ?: pythonAnyClassId.name,
                input.method.methodSignature(),
                it,
                it.toLong()
            )
        }
    )
}

fun getEvaluationResult(input: EvaluationInput, process: EvaluationProcess, timeout: Long): PythonEvaluationResult {
    val result = getResult(process.process, timeout = timeout)
//    process.fileWithCode.delete()

    if (result.terminatedByTimeout)
        return PythonEvaluationTimeout()

    if (result.exitValue != 0)
        return PythonEvaluationError(
            result.exitValue,
            result.stdout,
            result.stderr.split(System.lineSeparator())
        )

    val output = process.fileForOutput.readText().split(System.lineSeparator())
    process.fileForOutput.delete()

    if (output.size != 8)
        return PythonEvaluationError(
            0,
            "Incorrect format of output",
            emptyList()
        )

    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val stmtsAdapter: JsonAdapter<List<Int>> = moshi.adapter(Types.newParameterizedType(List::class.java, Int::class.javaObjectType))
    val argsIdsAdapter: JsonAdapter<List<String>> = moshi.adapter(Types.newParameterizedType(List::class.java, String::class.java))
    val kwargsIdsAdapter: JsonAdapter<List<String>> = moshi.adapter(Types.newParameterizedType(List::class.java, String::class.java))

    return when (val status = output[0]) {
        PythonEvaluationStatus.ARGUMENTS_FAIL.value -> {
            PythonEvaluationError(
                0,
                output[1],
                emptyList(),
            )
        }
        PythonEvaluationStatus.FAIL.value, PythonEvaluationStatus.SUCCESS.value -> {
            val isSuccess = status == PythonEvaluationStatus.SUCCESS.value

            val stmts = stmtsAdapter.fromJson(output[1])!!
            val missed = stmtsAdapter.fromJson(output[2])!!
            val coverage = calculateCoverage(stmts, missed, input)

            val stateBeforeMemory = PythonObjectDeserializer.parseDumpedObjects(output[3])
            val stateAfterMemory = PythonObjectDeserializer.parseDumpedObjects(output[4])

            val argsIds = argsIdsAdapter.fromJson(output[5])!!
            val kwargsIds = kwargsIdsAdapter.fromJson(output[6])!!
            val resultId = output[7]

            val modelListIds = argsIds + kwargsIds

            val resultValue = stateAfterMemory.getById(resultId).toPythonTree(stateAfterMemory)

            PythonEvaluationSuccess(
                coverage,
                !isSuccess,
                stateBeforeMemory,
                stateAfterMemory,
                modelListIds,
                OutputData(resultValue, resultValue.type),
            )
        }
        else -> {
            PythonEvaluationError(
                0,
                "Incorrect format of output",
                emptyList()
            )
        }
    }
}
