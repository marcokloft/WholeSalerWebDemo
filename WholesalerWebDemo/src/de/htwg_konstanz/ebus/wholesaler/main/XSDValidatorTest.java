package de.htwg_konstanz.ebus.wholesaler.main;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.xml.sax.SAXException;

public class XSDValidatorTest {

	public static void main(String[] args) {

		File schemaFile = new File("bmecat_new_catalog_1_2_simple_without_NS.xsd");
		Source xmlFile = new StreamSource(new File("xmltest.xml"));
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		System.out.println("asd");
		try {
			Schema schema = schemaFactory.newSchema(schemaFile);
		  
		  javax.xml.validation.Validator validator = schema.newValidator();
		  validator.validate(xmlFile);
		  System.out.println(xmlFile.getSystemId() + " is valid");
		  
		} catch (SAXException e) {
		  System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
		} catch (IOException e) {}

		DiskFileItemFactory asd = new DiskFileItemFactory();
		ServletFileUpload sFU = new ServletFileUpload(asd);
		
		sFU.
	}

}
