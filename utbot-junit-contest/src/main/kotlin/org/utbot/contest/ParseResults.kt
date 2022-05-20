package org.utbot.contest

import java.io.File

/**
 * Parse folder with contests results, collected by contest organizers.
 * Example of zipped folders with results: [https://drive.google.com/drive/folders/1dvBqkjqMxY0hFJ5nBbot7iv89ZlSQPro]
 */
fun main(args: Array<String>) {
    val folderPath = if (args.isNotEmpty()) args[0] else "C:\\Users\\d00555580\\Downloads\\results_utbot-concrete_30"
    val folder = File(folderPath)
    require(folder.exists() && folder.isDirectory) { "Folder $folderPath isn't exist" }

    val runs = mutableListOf<BenchmarkRun>()

    val regex = Regex("(\\p{Alpha}+)-(\\d+)_(\\d+)")
    for (dir in folder.listFiles()) {
        if (!dir.isDirectory) continue
        val matchResult = regex.matchEntire(dir.name) ?: continue
        val (projectName, classId, attemptN) = matchResult.destructured

//        println("$projectName $classId $attemptN")

        val csv = File(dir, "transcript.csv")

        if (!csv.exists())
            println("File ${csv.absolutePath} doesn't exist")
        else {
            for (line in csv.readLines()
                .filter { it.startsWith("utbot") }
            ) {
                val run = BenchmarkRun(line.split(","))
                runs.add(run)
//                println(run)
            }
        }

    }

    val allCuts = runs.groupBy { it.classFQN }.map { (k,v) -> CutInBenchmark(k, v) }

    println("Total runs: ${runs.size}")
    println("Non-compilable runs: ${runs.count {!it.compilable}}")

    println("Total classes: ${allCuts.size}")
    val neverCompilable = allCuts.filter { (_, runs) -> runs.all { !it.compilable }}.toSet()

    println("Never compilable classes: ${neverCompilable.size}")
    println("  "+neverCompilable.joinToString("\n  "))


    val partlyCompilable = (allCuts - neverCompilable).filter { (_, runs) -> runs.any {!it.compilable} }.toSet()
    println("Classes non-compilable in several runs: ${partlyCompilable.size}")
    println("  "+partlyCompilable.joinToString("\n  ") { cut ->
        "$cut non-compilable runs: " + cut.runs.filter { r -> !r.compilable}.joinToString { run -> run.runS }
    })

    val compilable = allCuts - neverCompilable - partlyCompilable

    println()
    println("Compilable classes sorted from lowest line coverage ratio to highest: ${compilable.count()}")
    for (cut in compilable.sortedBy { it.averageCoverage }) {
        println(" $cut, avg=${"%.2f".format(cut.averageCoverage)}, runs: ${cut.runs.joinToString { "%.2f".format(it.lineCoverage) }}")
    }

}

data class CutInBenchmark(val fqn: String, val runs: List<BenchmarkRun>) {

    val averageCoverage = runs.sumByDouble { it.lineCoverage } / runs.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CutInBenchmark

        if (fqn != other.fqn) return false

        return true
    }

    override fun hashCode(): Int {
        return fqn.hashCode()
    }

    override fun toString(): String = "$fqn (${runs.map { it.benchmarkName }.distinct().joinToString()})"
}

//tool	benchmark	class	run	preparationTime	generationTime	executionTime	testcaseNumber	uncompilableNumber	brokenTests	failTests	linesTotal	linesCovered	linesCoverageRatio	conditionsTotal	conditionsCovered	conditionsCoverageRatio	mutantsTotal	mutantsCovered	mutantsCoverageRatio	mutantsKilled	mutantsKillRatio	mutantsAlive	timeBudget	totalTestClasses
//utbot-concrete	GUAVA-45	com.google.common.primitives.Shorts	6	12505	18927	531	176	0	1	0	103	97	94.17475	68	59	86.7647	133	128	96.2406	128	96.2406	5	30	1
class BenchmarkRun(
    val toolName: String,                   //utbot-concrete
    val benchmarkName: String,              //GUAVA-45
    val classFQN: String,                   //com.google.common.primitives.Shorts
    val runS: String,                        //6
    val preparationTimeS: String,            //12505
    val generationTimeS: String,             //18927
    val executionTimeS: String,                 //531
    val testCaseNumberS: String,             //176
    val uncompilableNumberS: String,         //0
    val brokenTestsS: String,                //1
    val failTestsS: String,                  //0
    val linesTotalS: String,                 //103
    val linesCoveredS: String,               //97
    val linesCoverageRatioS : String,        //94.17475
    val conditionsTotalS: String,            //68
    val conditionsCoveredS: String,          //59
    val conditionsCoverageRatioS: String,    //86.7647
    val mutantsTotalS: String,               //133
    val mutantsCoveredS: String,             //128
    val mutantsCoverageRatioS: String,       //96.2406
    val mutantsKilledS: String,              //128
    val mutantsKillRatioS: String,           //96.2406
    val mutantsAliveS: String,               //5
    val timeBudgetS: String,                 //30
    val totalTestClassesS: String,           //1
) {
    val compilable = uncompilableNumberS == "0"
    val lineCoverage = linesCoverageRatioS.toDouble()


    constructor(a: List<String>) : this(
        a[0],
        a[1],
        a[2],
        a[3],
        a[4],
        a[5],
        a[6],
        a[7],
        a[8],
        a[9],
        a[10],
        a[11],
        a[12],
        a[13],
        a[14],
        a[15],
        a[16],
        a[17],
        a[18],
        a[19],
        a[20],
        a[21],
        a[22],
        a[23],
        a[24]
    )
}