package org.utbot.contest

//import java.nio.charset.Charset
//import java.nio.file.Files
//import java.nio.file.Path
//import java.nio.file.Paths
//import kotlin.streams.asSequence
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Element
//import org.jsoup.nodes.Node
//
//fun main(args: Array<String>) {
//    val originalReportPath: Path
//    val classFiltersFolder: Path
//
//    if (args.isEmpty()) {
//        println("Using default parameters to start filtering")
//        originalReportPath = Paths.get("utbot-junit-contest/build/reports/jacoco/test/html/index.html").toAbsolutePath()
//        classFiltersFolder = Paths.get("utbot-junit-contest/src/main/resources/classes").toAbsolutePath()
//    } else {
//        println("Command line arguments being passed: ${args.toText()}")
//        originalReportPath = Paths.get(args[0])
//        classFiltersFolder = Paths.get(args[1])
//    }
//
//    val originalReport = Jsoup.parse(originalReportPath.toFile(), "UTF-8")
//    val packages = packages(classFiltersFolder.toAbsolutePath())
//    removeIrrelevantPackages(originalReport, packages)
//    val filteredReportPath = Paths.get(originalReportPath.parent.toString(), "index_filtered.html")
//    println("Saving filtered report to ${filteredReportPath.toAbsolutePath()}")
//    Files.write(filteredReportPath, originalReport.html().toByteArray(Charset.defaultCharset()))
//}
//
//fun packages(path: Path) = Files.walk(path)
//    .asSequence()
//    .filter(Files::isRegularFile)
//    .flatMap { Files.readAllLines(it) }
//    .mapTo(mutableSetOf()) { it.substringBeforeLast(".") }
//
//private fun removeIrrelevantPackages(node: Node, packages: Set<String>) {
//    var i = 0
//    while (i < node.childNodes().size) {
//        val child = node.childNode(i)
//        if (child.toBeRemoved(packages)) {
//            child.remove()
//        } else {
//            removeIrrelevantPackages(child, packages)
//            i++
//        }
//    }
//}
//
//fun Node.toBeRemoved(packages: Set<String>): Boolean {
//    if (this.nodeName() == "tr") {
//        val href = (this as Element).select("a[href]").attr("href")
//        if (href.endsWith("index.html")
//            && href.substringBeforeLast("/") !in packages
//        ) {
//            return true
//        }
//    }
//    return false
//}