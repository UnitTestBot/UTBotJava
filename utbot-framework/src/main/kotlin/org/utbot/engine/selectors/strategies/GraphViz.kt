package org.utbot.engine.selectors.strategies

import mu.KotlinLogging
import org.utbot.common.FileUtil.createNewFileWithParentDirectories
import org.utbot.engine.CALL_DECISION_NUM
import org.utbot.engine.Edge
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.isLibraryNonOverriddenClass
import org.utbot.engine.isReturn
import org.utbot.engine.selectors.PathSelector
import org.utbot.engine.stmts
import org.utbot.framework.UtSettings.copyVisualizationPathToClipboard
import org.utbot.framework.UtSettings.showLibraryClassesInVisualization
import soot.jimple.Stmt
import soot.toolkits.graph.ExceptionalUnitGraph
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}


class GraphViz(
    private val globalGraph: InterProceduralUnitGraph,
    private val pathSelector: PathSelector
) : TraverseGraphStatistics(globalGraph) {

    // Files
    private val graphVisDirectory = Files.createTempDirectory("Graph-vis")
    private val graphVisPathString = graphVisDirectory.toString()
    private val graphJs = Paths.get(graphVisPathString, "graph.js").toFile()

    // Subgraph data
    private val stmtToSubgraph = mutableMapOf<Stmt, String>()
    private val subgraphVisibility = mutableMapOf<String, String>()     // public, private and etc
    private val subgraphEdges = mutableMapOf<String, MutableSet<Edge>>()
    private val libraryGraphs = mutableSetOf<String>()
    private val exceptionalEdges = mutableMapOf<Edge, String>()


    init {
        // Prepare workspace
        val requiredFileNames = arrayOf(
            "full.render.js", "graph.js", "legend.png", "private_method.png",
            "public_method.png", "render_graph.js", "UseVisJs.html", "viz.js"
        )

        val classLoader = GraphViz::class.java.classLoader

        for (file in requiredFileNames) {
            classLoader.getResourceAsStream("html/$file").use { inputStream ->
                val path = Paths.get(graphVisDirectory.toString(), file)
                val targetFile = path.toFile()
                targetFile.createNewFileWithParentDirectories()

                targetFile.outputStream().use { targetOutputStream ->
                    inputStream?.copyTo(targetOutputStream) ?: logger.error {
                        "Could not start a visualization because of missing resource html/$file"
                    }
                }
            }
        }
        FileWriter(graphJs).use {
            it.write(
                "var fullStack=[]\nvar fullStackDepth=[]\n" +
                        "var fullStackVisibility=[]\nvar uncompletedStack=[]\n" +
                        "var uncompletedStackDepth=[]\nvar uncompletedStackVisibility=[]\nvar globalGraph=``"
            )
        }

        update()

        val path = Paths.get(graphVisPathString, "UseVisJs.html")

        logger.debug { "Debug visualization: $path" }

        if (copyVisualizationPathToClipboard) {
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(path.toString()), null)
            }
        }
    }

    override fun onVisit(executionState: ExecutionState) {
        var src: Stmt = executionState.path.firstOrNull() ?: return

        update()

        val subgraph = stmtToSubgraph[src] ?: error("No subgraph found for the $src statement")

        val uncompletedStack = mutableListOf(DotGraph(subgraph, 0)) // Only uncompleted methods execution
        val fullStack = mutableListOf(DotGraph(subgraph, 0))
        val dotGlobalGraph = DotGraph("GlobalGraph", 0)

        // Add edges to dot graph
        subgraphEdges[subgraph]?.forEach {
            uncompletedStack.last().addDotEdge(it)
            fullStack.last().addDotEdge(it)
        }

        graph.allEdges.forEach { edge ->
            val (edgeSrc, edgeDst, _) = edge

            val srcInLibraryMethod = stmtToSubgraph[edgeSrc] in libraryGraphs
            val dstInLibraryMethod = stmtToSubgraph[edgeDst] in libraryGraphs
            val edgeIsRelatedToLibraryMethod = srcInLibraryMethod || dstInLibraryMethod

            if (!edgeIsRelatedToLibraryMethod || showLibraryClassesInVisualization) {
                dotGlobalGraph.addDotEdge(edge)
            }
        }

        // Split execution stack
        val allStatements = executionState.path + executionState.stmt

        for (dst in allStatements) {
            val edge = executionState.edges.findLast { it.src == src && it.dst == dst } ?: executionState.lastEdge

            if (edge == null || edge.src != src || edge.dst != dst) {
                continue
            }

            val graphName = stmtToSubgraph[dst] ?: error("No subgraph found for the $dst statement")


            uncompletedStack.last().markAsVisited(src, dst, edge)
            fullStack.last().markAsVisited(src, dst, edge)
            dotGlobalGraph.markAsVisited(src, dst, edge)

            // Check if we returned to previous method
            if (uncompletedStack.last().name != graphName) {
                if (uncompletedStack.size > 1 && uncompletedStack[uncompletedStack.size - 2].name == graphName) {
                    uncompletedStack.removeLast()
                } else {
                    uncompletedStack.addGraphWithEdges(graphName, uncompletedStack.size)
                }
            }

            // Check if we change execution method
            if (graphName != stmtToSubgraph[src]) {
                fullStack.addGraphWithEdges(graphName, uncompletedStack.size - 1)
            }

            src = dst
        }

        // Filter library methods
        if (!showLibraryClassesInVisualization) {
            uncompletedStack.removeIf { it.name in libraryGraphs }
            fullStack.removeIf { it.name in libraryGraphs }
        }

        // Update nodes and edges properties
        dotGlobalGraph.updateProperties(executionState)
        uncompletedStack.forEach { it.updateProperties(executionState) }
        fullStack.forEach { it.updateProperties(executionState) }

        // Write result
        FileWriter(graphJs).use { writer ->
            writer.write(
                "var fullStack=[${fullStack.joinToString(",\n") { "`$it`" }}]\n" +
                        "var fullStackDepth=[${fullStack.joinToString(",") { it.depth.toString() }}]\n" +
                        "var fullStackVisibility=[${fullStack.joinToString(",") { "\"${subgraphVisibility[it.name]}\"" }}]\n" +
                        "var uncompletedStack=[${uncompletedStack.joinToString(",\n") { "`$it`" }}]\n" +
                        "var uncompletedStackDepth=[${uncompletedStack.joinToString(",") { it.depth.toString() }}]\n" +
                        "var uncompletedStackVisibility=[${uncompletedStack.joinToString(",") { "\"${subgraphVisibility[it.name]}\"" }}]\n" +
                        "var globalGraph=`$dotGlobalGraph`"
            )
        }
    }

    private fun MutableList<DotGraph>.addGraphWithEdges(graphName: String, depth: Int) {
        this += DotGraph(graphName, depth)
        subgraphEdges[graphName]?.forEach {
            last().addDotEdge(it)
        }
    }

    private fun DotGraph.markAsVisited(src: Stmt, dst: Stmt, edge: Edge) {
        dotNode(src)?.visited = true
        dotNode(dst)?.visited = true
        dotEdge(edge)?.isVisited = true
    }

    // Note that we do not use `shouldRegister` here, because visualization
    // does not depend on the fact of registration. Otherwise, we'd
    // lose overridden classes here and don't join them at the visualization.
    override fun onJoin(stmt: Stmt, graph: ExceptionalUnitGraph, shouldRegister: Boolean) {
        val method = graph.body?.method
        val declaringClass = method?.declaringClass

        if (declaringClass?.isLibraryNonOverriddenClass == true) {
            libraryGraphs += declaringClass.shortName + "." + method.name
        }

        update()
    }

    override fun onTraversed(executionState: ExecutionState) {
        if (executionState.lastEdge == null) {
            return
        }

        if (executionState.exception != null) {
            exceptionalEdges[executionState.lastEdge] = executionState.exception.concrete.toString()
        }
    }

    private fun update() {
        graph.graphs.forEachIndexed { _, graph ->
            val method = graph.body?.method
            val declaringClass = if (method?.isDeclared == true) method.declaringClass?.shortName else "UnknownClass"
            val methodName = declaringClass + "." + method?.name
            val visibility = if (method?.isPrivate == true) "private" else "public"

            subgraphVisibility.putIfAbsent(methodName, visibility)
            subgraphEdges.putIfAbsent(methodName, mutableSetOf())

            graph.stmts.forEach { stmt ->
                stmtToSubgraph[stmt] = methodName
            }
        }

        for (edge in graph.allEdges) {
            val subgraph = stmtToSubgraph[edge.src] ?: error("No subgraph found for the ${edge.src} statement")
            val edges = subgraphEdges[subgraph] ?: error("No subgraph edges found for the $subgraph")
            edges += edge
        }
    }

    private fun DotGraph.updateProperties(executionState: ExecutionState) {
        // Node property: Last execution state
        dotNode(executionState.stmt)?.isLast = true

        // Node property: covered
        globalGraph.stmts.forEach { dotNode(it)?.covered = globalGraph.isCoveredIgnoringRegistration(it) }

        val queue = pathSelector.queue()

        // Node property: In queue
        queue.forEach { (state, _) -> dotNode(state.stmt)?.inQueue = true }

        // Node property: Head of queue
        pathSelector.peek()?.let { dotNode(it.stmt)?.headQueue = true }

        // Edge property: covered
        globalGraph.allEdges.forEach {
            dotEdge(it)?.isCovered = globalGraph.isCovered(it)
        }

        // Edge property: Edge to queue stmt property
        queue.filter { (state, _) -> state.lastEdge != null && state.lastEdge !in globalGraph.implicitEdges }
            .forEach { (state, weight) ->
                dotEdge(state.lastEdge!!)?.apply {
                    isToQueueStmt = true
                    label = "%.2f".format(weight)
                }
            }

        // Edge property: implicit edge to exception
        exceptionalEdges.keys.forEach {
            dotEdge(it)?.apply {
                isExceptional = true
                toException = exceptionalEdges.getValue(it)
            }
        }
    }
}

/**
 * Represents graph node in dot format
 */
data class DotNode(val id: Int, val label: String) {
    var inQueue: Boolean = false
    var headQueue: Boolean = false
    var covered: Boolean = false
    var returned: Boolean = false
    var visited: Boolean = false
    var invoke: Boolean = false
    var isLast: Boolean = false
    private var toolTip: String = ""

    private val fillColor: String
        get() {
            val colors = mutableListOf<String>()
            if (inQueue) colors += if (headQueue) "lightsalmon" else "khaki1"
            if (covered) colors += "azure3"

            return mixtureColors(colors, "white")
        }

    private val color: String
        get() = if (visited) "red" else "black"

    private val shape: String
        get() = when {
            returned -> "circle"
            invoke -> "cds"
            else -> "rectangle"
        }

    private val width: String
        get() = when {
            isLast -> "5.0"
            invoke -> "2.0"
            else -> "1.0"
        }

    override fun toString() = "\"$id\" [label=$label,shape=$shape,tooltip=\"$toolTip\",penwidth=$width," +
            "color=$color,style=filled,fillcolor=\"$fillColor\"];\n"
}


/**
 * Represents graph edge in dot format
 */
data class DotEdge(val id: Int, val srcId: Int, val dstId: Int, var label: String = "") {
    var isCovered = false
    var isExceptional = false
    var isToQueueStmt = false
    var isVisited = false
    var isCaller = false
    var toException = ""

    private val color: String
        get() {
            val colors = mutableListOf<String>()
            if (isVisited) colors += "red"
            if (isToQueueStmt) colors += "lightsalmon"
            if (isCovered) colors += "azure3"
            return mixtureColors(colors, "black")
        }

    private val style: String
        get() = when {
            isCaller -> "dotted"
            isExceptional -> "dashed"
            else -> ""
        }

    override fun toString(): String {
        return "\"$srcId\" -> \"$dstId\" [label=\"$label\",style=\"$style\",color=\"$color\"];\n"
    }
}


/**
 * Represents graph in dot format
 */
class DotGraph(val name: String, val depth: Int) {
    private var nodeId = 0
    private var edgeId = 0
    private val nodes = mutableMapOf<Stmt, DotNode>()
    val edges = mutableMapOf<Edge, DotEdge>()

    fun dotNode(stmt: Stmt): DotNode? = nodes[stmt]

    fun dotEdge(edge: Edge): DotEdge? = edges[edge]

    fun addDotEdge(edge: Edge): DotEdge = with(edge) {
        val srcNode = nodes.getOrPut(src) { DotNode(nodeId++, src.toDotName()) }
        val dstNode = nodes.getOrPut(dst) { DotNode(nodeId++, dst.toDotName()) }

        srcNode.returned = src.isReturn
        srcNode.invoke = src.containsInvokeExpr()

        dstNode.returned = dst.isReturn
        dstNode.invoke = dst.containsInvokeExpr()

        val dotEdge = edges.getOrPut(edge) { DotEdge(edgeId++, srcNode.id, dstNode.id) }
        dotEdge.isCaller = decisionNum == CALL_DECISION_NUM

        dotEdge
    }

    // Remove special characters in dot format
    private fun Stmt.toDotName() = "\"" + this.toString()
        .replace("\"", "\\\"")
        .replace("'", "\\'")
        .replace("-", "")
        .replace("\\", "\\\\") + "\""

    override fun toString() = buildString {
        // Add header
        append(
            "digraph DotGraph {\nstyle=filled;\ncolor=lightgrey;\nnode [shape=rectangle];\nlabel=\"$name\";\n" +
                    "fontsize=30;\nlabelloc=t;\n"
        )

        // Add node info
        nodes.values.forEach { append(it.toString()) }

        // Add edge info
        edges.filter { !it.value.isExceptional }.forEach { append(it.value.toString()) }

        // Add implicit edge and node (exception case)
        edges.values
            .filter { it.isExceptional }
            .forEach {
                val implicitId = nodeId++
                val color = if (it.isCovered) "azure3" else "black"
                val shortName = it.toException.split(":")[0].filter { it2 -> it2.isUpperCase() }

                if (it.isCovered) {
                    append(
                        "\"$implicitId\" [label=\"${shortName}\",tooltip=\"${it.toException}\"," +
                                "shape=circle,style=\"filled,dashed\",fillcolor=azure3];\n"
                    )
                } else {
                    append(
                        "\"$implicitId\" [label=\"${shortName}\", tooltip=\"${it.toException}\"," +
                                "shape=circle, style=dashed];\n"
                    )
                }

                append("\"${it.srcId}\" -> \"$implicitId\" [style=dashed,color=\"$color\"];\n")
            }

        // Close dot graph
        append("}")
    }
}

/**
 * Mixture of the first two colors in the list.
 */
fun mixtureColors(colors: List<String>, default: String) =
    if (colors.isEmpty()) default else colors.take(2).joinToString(";0.5:")