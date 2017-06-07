package de.htwg_konstanz.ebus.wholesaler.main;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class ValidatorImportTest {
	
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Document doc;
		try {
					File fXmlFile = new File("xmltest.xml");
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder;
					dBuilder = dbFactory.newDocumentBuilder();
					doc = dBuilder.parse(fXmlFile);
					doc.getDocumentElement().normalize();
					System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
					
					SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

				    // load a WXS schema, represented by a Schema instance
				    Source schemaFile = new StreamSource(new File("bmecat_new_catalog_1_2_simple_without_NS.xsd"));
				    Schema schema = factory.newSchema(schemaFile);

				    // create a Validator instance, which can be used to validate an instance document
				    Validator validator = schema.newValidator();

				    // validate the DOM tree
				    try {
				        validator.validate(new DOMSource(doc));
				    } catch (SAXException e) {
				    	System.out.println("Validation exception:" + e);
				        // instance document is invalid!
				    }
				} catch (ParserConfigurationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				//optional, but recommended
				//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
				

				

	}

}
