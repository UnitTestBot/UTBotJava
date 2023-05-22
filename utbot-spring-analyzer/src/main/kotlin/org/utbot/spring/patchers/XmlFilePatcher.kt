package org.utbot.spring.patchers

import org.utbot.spring.utils.PathsUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XmlFilePatcher(val fileStorage: Array<String>) {
    fun patchXmlConfigurationFile(userXmlConfigurationFilePath: String) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

        val xmlPaths = PathsUtils.getPathsByPattern(fileStorage[0], userXmlConfigurationFilePath)
        for(xmlPath in xmlPaths) {
            val doc = builder.parse(xmlPath)

            patchPropertyPlaceholders(doc)
            patchImportResources(doc)
            writeXmlFile(doc, xmlPath)
        }
    }

    private fun patchPropertyPlaceholders(doc: Document) {
        val elements = doc.getElementsByTagName("context:property-placeholder")
        val elementsCount = elements.length

        // Xml file may contain several property placeholders:
        // see https://stackoverflow.com/questions/26618400/how-to-use-multiple-property-placeholder-in-a-spring-xml-file
        for (i in 0 until elementsCount) {
            val element = elements.item(i) as Element
            val path = element.attributes.item(0)

            path.nodeValue = PathsUtils.patchPath(fileStorage[0], path.nodeValue)
        }

        doc.normalize()
    }

    private fun patchImportResources(doc: Document) {
        val elements = doc.getElementsByTagName("import")
        val elementsCount = elements.length

        for (i in 0 until elementsCount) {
            val element = elements.item(i) as Element
            val path = element.attributes.item(0)

            path.nodeValue = PathsUtils.patchPath(fileStorage[0], path.nodeValue)

            // recursive xml file processing call
            patchXmlConfigurationFile(path.nodeValue)

            //TODO(Check for infinite recursion)
        }

        doc.normalize()
    }

    private fun writeXmlFile(doc: Document, userXmlConfigurationFilePath: String) {
        val tFormer = TransformerFactory.newInstance().newTransformer()
        val source = DOMSource(doc)
        val destination = StreamResult(userXmlConfigurationFilePath)

        tFormer.transform(source, destination)
    }
}