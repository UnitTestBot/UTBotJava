package analyzers

import org.w3c.dom.Document
import org.w3c.dom.Element
import utils.ResourceNames
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.Result
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class XmlConfigurationAnalyzer(private val userXmlFilePath: String) {
    private val fakeXmlFilePath = this.javaClass.classLoader.getResource(ResourceNames.fakeApplicationXmlFileName)?.path
        ?: error("The path must exist")

    @Throws(Exception::class)
    fun fillFakeApplicationXml() {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = builder.parse(userXmlFilePath)

        // Property placeholders may contain file names relative to user project,
        // they will not be found in ours. We import all properties using another approach.
        deletePropertyPlaceholders(doc)
        writeXmlFile(doc)
    }

    private fun deletePropertyPlaceholders(doc: Document) {
        val elements = doc.getElementsByTagName("context:property-placeholder")
        val elementsCount = elements.length

        // Xml file may contain several property placeholders:
        // see https://stackoverflow.com/questions/26618400/how-to-use-multiple-property-placeholder-in-a-spring-xml-file
        for (i in 0 until elementsCount) {
            val element = elements.item(i) as Element
            element.parentNode.removeChild(element)
        }

        doc.normalize()
    }

    @Throws(TransformerException::class)
    private fun writeXmlFile(doc: Document) {
        val tFormer = TransformerFactory.newInstance().newTransformer()
        val source: Source = DOMSource(doc)
        val dest: Result = StreamResult(fakeXmlFilePath)

        tFormer.transform(source, dest)
    }
}