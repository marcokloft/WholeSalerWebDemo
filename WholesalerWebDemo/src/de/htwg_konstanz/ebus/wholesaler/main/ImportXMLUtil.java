package de.htwg_konstanz.ebus.wholesaler.main;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportXMLUtil {

	HttpServletRequest request;
	ArrayList<String> errorList;

	private static boolean isXMLFile = false;
	private static boolean isNoFile = false;
	private static boolean isValid = false;

	/*
	 * public Document getDocument(){ return this.doc; }
	 */

	public ImportXMLUtil(HttpServletRequest request, ArrayList<String> errorList) {
		this.request = request;
		this.errorList = errorList;
	}

	public Document createFile(HttpServletRequest myRequest) {
		Document doc = null;
		List<FileItem> fileItem = null;

		DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
		ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);

		try {
			fileItem = servletFileUpload.parseRequest(myRequest);
		} catch (FileUploadException e) {
			System.out.println("CATCH FILE UPLOAD EXCEPTION");
			errorList.add("Parse Error: "+e);
			e.printStackTrace();
			return null;
		}
		doc = importXMLFile(fileItem, this.errorList);

		return doc;
	}

	public Document importXMLFile(List<FileItem> fileItems, ArrayList<String> errorList) {

		Document doc = null;
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

		isNoFile = fileItems.iterator().next().getName().isEmpty();
		isXMLFile = fileItems.iterator().next().getName().toLowerCase().endsWith(".xml");
		
		System.out.println("NoFile : "+isNoFile+"  XMLFile : "+isXMLFile);

		if (isNoFile) {
			errorList.add("Import error: No file imported");
			return null;
		}
		if (!isXMLFile) {
			errorList.add("Import error: Imported File is not an XML File");
			return null;
		}
		if (!isXMLFile && !isNoFile) {
			errorList.add("Import error: Wrong document type");
			return null;
		}

		for (FileItem fileItem : fileItems) {

			InputStream inputStream;
			DocumentBuilder documentBuilder;

			try {
				inputStream = fileItem.getInputStream();
				documentBuilder = documentBuilderFactory.newDocumentBuilder();
				doc = documentBuilder.parse(inputStream);

			} catch (SAXException e) {
				errorList.add("File upload error SAXException: " + e.getMessage());
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				errorList.add("Import error:" + e.getMessage());
				e.printStackTrace();
				return null;
			} catch (ParserConfigurationException e) {
				errorList.add("Import error:" + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		// XSD Validator
		isValid = validateXSD(doc, errorList);

		if (!isValid) {
			errorList.add("Validator error: Not XSD valid");
			return null;
		}
		
		return doc;
	}

	public boolean validateXSD(Document doc, ArrayList<String> errorlist) {

		File schemaFile = new File("bmecat_new_catalog_1_2_simple_without_NS.xsd");
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			Schema schema = schemaFactory.newSchema(schemaFile);
			javax.xml.validation.Validator validator = schema.newValidator();
			validator.validate(new DOMSource(doc));
		} catch (SAXException e) {
			errorList.add("Validator error: is NOT valid reason: " + e.getMessage());
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			errorList.add("Validator error: File import error: " + e.getMessage());
			return false;
		}

		return true;
	}

}
