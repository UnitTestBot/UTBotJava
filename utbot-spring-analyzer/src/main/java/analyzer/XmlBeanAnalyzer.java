package analyzer;

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
import java.nio.file.Path;
import java.nio.file.Paths;


public class XmlBeanAnalyzer {
    //change to auto path detect
    private final String PATH = "utbot-spring-analyzer/src/main/resources";
    private final String xmlFilePath;
    private final String fakeXmlFilePath;


    public XmlBeanAnalyzer(String xmlFilePath) {
        this.xmlFilePath = xmlFilePath;
        this.fakeXmlFilePath = getFakePath(xmlFilePath);
    }

    private String getFakePath(String filePath) {
        Path path = Paths.get(filePath);
        return Paths.get(PATH, "fake" + path.getFileName()).toString();
    }

    public void rewriteXmlFile() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFilePath);

        deletePropertyPlaceholder(doc);
        writeXmlFile(doc);
    }

    private void deletePropertyPlaceholder(Document doc) {
        NodeList elements = doc.getElementsByTagName("context:property-placeholder");
        int elementsCount = elements.getLength();

        //https://stackoverflow.com/questions/26618400/how-to-use-multiple-property-placeholder-in-a-spring-xml-file
        for (int i = 0; i < elementsCount; i++) {
            Element element = (Element) elements.item(i);
            element.getParentNode().removeChild(element);
        }
        doc.normalize();
    }

    private void writeXmlFile(Document doc) throws TransformerException {
        Source source = new DOMSource(doc);
        Result dest = new StreamResult(fakeXmlFilePath);

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer tFormer = tFactory.newTransformer();
        tFormer.transform(source, dest);
    }
}