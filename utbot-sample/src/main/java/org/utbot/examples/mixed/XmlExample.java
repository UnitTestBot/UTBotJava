package org.utbot.examples.mixed;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.net.URL;

public class XmlExample {
    public void xmlReader(ContentHandler handler, String url) throws SAXException, IOException {
        XMLReader myReader = XMLReaderFactory.createXMLReader();
        myReader.setContentHandler(handler);
        myReader.parse(new InputSource(new URL(url).openStream()));
    }
}
