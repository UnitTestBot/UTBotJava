package org.utbot.engine.selectors.strategies

import org.utbot.engine.CALL_DECISION_NUM
import org.utbot.engine.Edge
import org.utbot.engine.ExecutionState
import org.utbot.engine.InterProceduralUnitGraph
import org.utbot.engine.isReturn
import org.utbot.engine.selectors.PathSelector
import org.utbot.engine.stmts
import org.utbot.framework.UtSettings.copyVisualizationPathToClipboard
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import org.utbot.engine.isOverridden
import soot.jimple.Stmt
import soot.toolkits.graph.ExceptionalUnitGraph

private val logger = KotlinLogging.logger {}


class GraphViz(
    private val globalGraph: InterProceduralUnitGraph,
    private val pathSelector: PathSelector
) : TraverseGraphStatistics(globalGraph) {

    // Files
    private val path = Files.createTempDirectory("Graph-vis")
    private val graphJs = Paths.get(path.toString(), "graph.js").toFile()

    // Subgraph data
    private val stmtToSubgraph = mutableMapOf<Stmt, String>()
    private val subgraphVisibility = mutableMapOf<String, String>()     // public, private and etc
    private val subgraphEdges = mutableMapOf<String, MutableSet<Edge>>()
    private val libraryGraphs = mutableSetOf<String>()
    private val exceptionalEdges = mutableMapOf<Edge, String>()


    init {
        // Prepare workspace
        for (file in arrayOf(
            "full.render.js", "graph.js", "legend.png", "private_method.png",
            "public_method.png", "render_graph.js", "UseVisJs.html", "viz.js"
        )) {
            FileUtils.copyInputStreamToFile(
                GraphViz::class.java.classLoader.getResourceAsStream("html/$file"),
                Paths.get(path.toString(), file).toFile()
            )
        }
        FileWriter(graphJs).use {
            it.write(
                "var fullStack=[]\nvar fullStackDepth=[]\n" +
                        "var fullStackVisibility=[]\nvar uncompletedStack=[]\n" +
                        "var uncompletedStackDepth=[]\nvar uncompletedStackVisibility=[]\nvar globalGraph=``"
            )
        }

        update()
        val path = Paths.get(path.toString(), "UseVisJs.html")
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

        val uncompletedStack = mutableListOf(DotGraph(stmtToSubgraph[src]!!, 0)) // Only uncompleted methods execution
        val fullStack = mutableListOf(DotGraph(stmtToSubgraph[src]!!, 0))
        val dotGlobalGraph = DotGraph("GlobalGraph", 0)

        // Add edges to dot graph
        subgraphEdges[stmtToSubgraph[src]!!]?.forEach {
            uncompletedStack.last().addDotEdge(it)
            fullStack.last().addDotEdge(it)
        }
        (graph.allEdges + graph.implicitEdges).forEach {
            if (!libraryGraphs.contains(stmtToSubgraph[it.src]) && !libraryGraphs.contains(stmtToSubgraph[it.dst])) {
                dotGlobalGraph.addDotEdge(it)
            }
        }

        // Split execution stack
        for (dst in executionState.path + executionState.stmt) {
            val edge = executionState.edges.findLast { it.src == src && it.dst == dst } ?: executionState.lastEdge
            if (edge == null || edge.src != src || edge.dst != dst) continue
            val graph = stmtToSubgraph[dst]!!

            uncompletedStack.last().dotNode(src)?.visited = true
            uncompletedStack.last().dotNode(dst)?.visited = true
            uncompletedStack.last().dotEdge(edge)?.isVisited = true

            fullStack.last().dotNode(src)?.visited = true
            fullStack.last().dotNode(dst)?.visited = true
            fullStack.last().dotEdge(edge)?.isVisited = true

            dotGlobalGraph.dotNode(src)?.visited = true
            dotGlobalGraph.dotNode(dst)?.visited = true
            dotGlobalGraph.dotEdge(edge)?.isVisited = true

            // Check if we returned to previous method
            if (uncompletedStack.last().name != graph) {
                if (uncompletedStack.size > 1 && uncompletedStack[uncompletedStack.size - 2].name == graph) {
                    uncompletedStack.removeLast()
                } else {
                    uncompletedStack += DotGraph(graph, uncompletedStack.size)
                    subgraphEdges[graph]?.forEach {
                        uncompletedStack.last().addDotEdge(it)
                    }
                }
            }

            // Check if we change execution method
            if (graph != stmtToSubgraph[src]) {
                fullStack += DotGraph(graph, uncompletedStack.size - 1)
                subgraphEdges[graph]?.forEach {
                    fullStack.last().addDotEdge(it)
                }
            }

            src = dst
        }

        // Filter library methods
        uncompletedStack.removeIf { libraryGraphs.contains(it.name) }
        fullStack.removeIf { libraryGraphs.contains(it.name) }

        // Update nodes and edges properties
        updateProperties(dotGlobalGraph, executionState)
        uncompletedStack.forEach { updateProperties(it, executionState) }
        fullStack.forEach { updateProperties(it, executionState) }

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

    override fun onJoin(stmt: Stmt, graph: ExceptionalUnitGraph, shouldRegister: Boolean) {
        val declaringClass = graph.body?.method?.declaringClass
        if (declaringClass?.isLibraryClass == true && !declaringClass.isOverridden) {
            libraryGraphs.add(graph.body?.method?.declaringClass?.shortName + "." + graph.body?.method?.name)
        }
        update()
    }

    override fun onTraversed(executionState: ExecutionState) {
        if (executionState.exception != null)
            exceptionalEdges[executionState.lastEdge ?: return] = executionState.exception.concrete.toString()
    }

    private fun update() {
        graph.graphs.forEachIndexed { _, graph ->
            val declaringClassName =
                if (graph.body.method.isDeclared) graph.body?.method?.declaringClass?.shortName else "UnknownClass"
            val name = declaringClassName + "." + graph.body?.method?.name
            subgraphVisibility.putIfAbsent(name, if (graph.body?.method?.isPrivate == true) "private" else "public")
            subgraphEdges.putIfAbsent(name, mutableSetOf())
            graph.stmts.forEach { stmt ->
                stmtToSubgraph[stmt] = name
            }
        }

        for (edge in graph.allEdges + graph.implicitEdges) {
            subgraphEdges[stmtToSubgraph[edge.src]!!]!!.add(edge)
        }
    }

    private fun updateProperties(graph: DotGraph, executionState: ExecutionState) {
        // Node property: Last execution state
        graph.dotNode(executionState.stmt)?.isLast = true

        // Node property: covered
        globalGraph.stmts.forEach { graph.dotNode(it)?.covered = globalGraph.isCoveredIgnoringRegistration(it) }

        // Node property: In queue
        pathSelector.queue().forEach { graph.dotNode(it.first.stmt)?.inQueue = true }

        // Node property: Head of queue
        if (!pathSelector.isEmpty()) graph.dotNode(pathSelector.peek()!!.stmt)?.headQueue = true

        // Edge property: covered
        (globalGraph.allEdges + globalGraph.implicitEdges).forEach {
            graph.dotEdge(it)?.isCovered = globalGraph.isCovered(it)
        }

        // Edge property: Edge to queue stmt property
        pathSelector.queue().filter { it.first.lastEdge != null && it.first.lastEdge !in globalGraph.implicitEdges }
            .forEach { (state, weight) ->
                graph.dotEdge(state.lastEdge!!)?.apply {
                    isToQueueStmt = true
                    label = "%.2f".format(weight)
                }
            }

        // Edge property: implicit edge to exception
        exceptionalEdges.keys.forEach {
            graph.dotEdge(it)?.apply {
                isExceptional = true
                toException = exceptionalEdges[it]!!
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
    var isImplicit: Boolean = false
    private var toolTip: String = ""

    private val fillColor: String
        get() {
            val colors = mutableListOf<String>()
            if (inQueue) colors += if (headQueue) "lightsalmon" else "khaki1"
            if (covered) colors += "azure3"

            return mixtureColors(colors, "white")
        }

    val color: String
        get() = if (visited) "red" else "black"

    val shape: String
        get() = when {
            returned -> "circle"
            invoke -> "cds"
            else -> "rectangle"
        }

    val width: String
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

    val color: String
        get() {
            val colors = mutableListOf<String>()
            if (isVisited) colors += "red"
            if (isToQueueStmt) colors += "lightsalmon"
            if (isCovered) colors += "azure3"
            return mixtureColors(colors, "black")
        }

    val style: String
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
    var nodeId = 0
    var edgeId = 0
    val nodes = mutableMapOf<Stmt, DotNode>()
    val edges = mutableMapOf<Edge, DotEdge>()

    fun dotNode(stmt: Stmt): DotNode? = nodes[stmt]

    fun dotEdge(edge: Edge): DotEdge? {
        return edges[edge]
    }

    fun addDotEdge(edge: Edge): DotEdge {
        val srcNode = nodes.getOrPut(edge.src) { DotNode(nodeId++, edge.src.toDotName()) }
        val dstNode = nodes.getOrPut(edge.dst) { DotNode(nodeId++, edge.dst.toDotName()) }

        srcNode.returned = edge.src.isReturn
        srcNode.invoke = edge.src.containsInvokeExpr()
        dstNode.returned = edge.dst.isReturn
        dstNode.invoke = edge.dst.containsInvokeExpr()

        val dotEdge = edges.getOrPut(edge) { DotEdge(edgeId++, srcNode.id, dstNode.id) }
        dotEdge.isCaller = edge.decisionNum == CALL_DECISION_NUM

        return dotEdge
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
        edges.values.filter { it.isExceptional }.forEach {
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