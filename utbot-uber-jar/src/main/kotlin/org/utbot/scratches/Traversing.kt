package org.utbot.scratches

import org.utbot.engine.EngineController
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.MockStrategy
import org.utbot.engine.UtBotSymbolicEngine
import org.utbot.engine.head
import org.utbot.framework.PathSelectorType
import org.utbot.framework.UtSettings.enableClinitSectionsAnalysis
import org.utbot.framework.UtSettings.pathSelectorStepsLimit
import org.utbot.framework.UtSettings.pathSelectorType
import org.utbot.framework.UtSettings.useConcreteExecution
import org.utbot.framework.plugin.api.ApplicationContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.framework.plugin.services.JdkInfoDefaultProvider
import org.utbot.framework.util.SootUtils
import org.utbot.framework.util.sootOptionConfiguration
import soot.jimple.Stmt
import soot.options.Options
import soot.toolkits.graph.ExceptionalUnitGraph
import java.io.File
import java.nio.file.Paths
import java.util.function.Consumer

fun main(args: Array<String>) {
    val classPath = args.firstOrNull() ?: error("Specify class path")
    val dependencyPath = ";$classPath"

    sootOptionConfiguration = Consumer { options: Options ->
        options.setPhaseOption("jb.tr", "ignore-nullpointer-dereferences:true")
        options.setPhaseOption("jb.dtr", "enabled:false")
        options.setPhaseOption("jb.ese", "enabled:false")
        options.setPhaseOption("jb.a", "enabled:false")
        options.setPhaseOption("jb.ule", "enabled:false")
        options.setPhaseOption("jb.dae", "enabled:false")
        options.setPhaseOption("jb.sils", "enabled:false")
        options.setPhaseOption("jb.ne", "enabled:false")
        options.setPhaseOption("jb.uce", "enabled:false")
        options.setPhaseOption("cg", "types-for-invoke:true")
        options.setPhaseOption("cg", "all-reachable:on")
        options.setPhaseOption("jop", "enabled:false")
        options.setPhaseOption("jap.npc", "enabled:false")
        options.setPhaseOption("jop", "enabled:false")
    }

    useConcreteExecution = false
    pathSelectorType = PathSelectorType.BFS_SELECTOR
    pathSelectorStepsLimit = 1000
    enableClinitSectionsAnalysis = false

    SootUtils.runSoot(
        buildDirPaths = classPath.split(File.pathSeparatorChar).map { Paths.get(it) }.toList(),
        classPath,
        true,
        JdkInfoDefaultProvider().info
    )

    withUtContext(UtContext(Thread.currentThread().contextClassLoader)) {
        val engine = UtBotSymbolicEngine(
            controller = EngineController(),
            methodUnderTest = MethodId(
                classId = ClassId("abc.Samples"),
                name = "foo",
                parameters = listOf(byteClassId),
                returnType = voidClassId
            ),
            classpath = classPath,
            dependencyPaths = dependencyPath,
            mockStrategy = MockStrategy.NO_MOCKS,
            chosenClassesToMockAlways = emptySet(),
            applicationContext = ApplicationContext(),
            solverTimeoutInMillis = 500
        )

        val head = engine.graph.head
        val stmts = mutableListOf<Stmt>()
        visit(head, engine.globalGraph) {
            stmts += it
            listOfNotNull(engine.globalGraph.succStmts(it).firstOrNull())
        }
        val state = engine.buildExecutionState(stmts) ?: return
        require(state.stmt == stmts.last())
        require(state.path == stmts.take(stmts.size - 1))
        println(state.stmt)
    }
}

fun visit(stmt: Stmt, graph: InterProceduralUnitGraph, generate: (stmt: Stmt) -> List<Stmt>) {
    try {
        val unitGraph = ExceptionalUnitGraph(stmt.invokeExpr.method.retrieveActiveBody())
        generate(stmt) // should fire to be included in the path, but generally it is ignored
        graph.join(stmt, unitGraph, true)
        visit(unitGraph.head, graph, generate)
    } catch (_: Throwable) {  }
    generate(stmt).forEach {
        visit(it, graph, generate)
    }
}