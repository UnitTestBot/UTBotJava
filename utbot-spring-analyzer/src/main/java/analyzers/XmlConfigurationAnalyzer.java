package analyzers;

import utils.ResourceNames;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlConfigurationAnalyzer {

    private final String userXmlFilePath;

    public XmlConfigurationAnalyzer(String userXmlFilePath) {
        this.userXmlFilePath = userXmlFilePath;
    }

    private final String fakeXmlFilePath =
            this.getClass().getClassLoader().getResource(ResourceNames.fakeApplicationXmlFileName).getPath();

    public void fillFakeApplicationXml() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(userXmlFilePath);

        // Property placeholders may contain file names relative to user project,
        // they will not be found in ours. We import all properties using another approach.
        deletePropertyPlaceholders(doc);

        writeXmlFile(doc);
    }

    private void deletePropertyPlaceholders(Document doc) {
        NodeList elements = doc.getElementsByTagName("context:property-placeholder");
        int elementsCount = elements.getLength();

        // Xml file may contain several property placeholders:
        // see https://stackoverflow.com/questions/26618400/how-to-use-multiple-property-placeholder-in-a-spring-xml-file
        for (int i = 0; i < elementsCount; i++) {
            Element element = (Element) elements.item(i);
            element.getParentNode().removeChild(element);
        }

        doc.normalize();
    }

    private void writeXmlFile(Document doc) throws TransformerException {
        Transformer tFormer = TransformerFactory.newInstance().newTransformer();
        Source source = new DOMSource(doc);
        Result dest = new StreamResult(fakeXmlFilePath);

        tFormer.transform(source, dest);
    }
}