package de.htwg_konstanz.ebus.wholesaler.main;

import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.htwg_konstanz.ebus.framework.wholesaler.api.bo.BOProduct;
import de.htwg_konstanz.ebus.framework.wholesaler.api.bo.BOSalesPrice;
import de.htwg_konstanz.ebus.framework.wholesaler.api.boa.ProductBOA;

/**
 * @author Marco
 *
 */
public class ExportXMLUtil {

	private HttpServletRequest request;
	private HttpServletResponse response;
	ServletContext context;
	private ArrayList<String> errorList;

	/**
	 * @param request get the request from the servlet
	 * @param response to answer the request get the Response of the servlet
	 * @param errorList 
	 */
	public ExportXMLUtil(HttpServletRequest request, HttpServletResponse response, ArrayList<String> errorList) {
		this.request = request;
		this.response = response;
		this.errorList = errorList;
		this.context = request.getSession().getServletContext();
	}

	/**
	 * @return get the errorlist
	 */
	public ArrayList<String> getErrorList() {
		return errorList;
	}

	/**
	 * @return returns as new empty document instance
	 * @throws ParserConfigurationException
	 */
	public Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document doc = documentBuilder.newDocument();
		return doc;
	}

	/** Selection of the choosen export type, extracted from the request, plus the searchtherm and starts the exact method
	 * @param userId get the Extracted userid out of the Servlet
	 * @return the Filepath to the created File
	 */
	public String exportFile(String userId) {
		String exportType = this.request.getParameter("exportType");
		String search = this.request.getParameter("searchTherm");
		String tmpPath = null;
		String filePath = null;
		
		switch (exportType) {
		case "xml":
			filePath = this.writeDomIntoFile(userId, search);
			break;
		case "xhtml":
			tmpPath = this.writeDomIntoFile(userId, search);
			filePath = this.catalogToXHTML(tmpPath, userId);
			break;
		case "xmlDownload":
			filePath = this.writeDomIntoFile(userId, search);
			this.createFileForExport(filePath);
			break;
		case "xhtmlDownload":
			tmpPath = this.writeDomIntoFile(userId, search);
			filePath = this.catalogToXHTML(tmpPath, userId);
			this.createFileForExport(filePath);
			break;
		default:
			break;
		}
		return filePath;
	}

	/** Chooses with or without searchtherm search in the DB
	 * @param searchTherm needed for the search in the shortdescription
	 * @return document with searched products
	 */
	public Document exportCatalog(String searchTherm) {
		Document doc = null;
		List<BOProduct> productList = ProductBOA.getInstance().findAll();
		
		// Search with Searchtherm
		if (!searchTherm.isEmpty() && searchTherm != null){
			
			String checkString;
			Boolean valueExist = false;
			
			try {
				doc = this.getDocument();
				this.createDom(doc);
			} catch (ParserConfigurationException e) {
				this.errorList.add("Parser Exception: createProductCatalog(searchTherm): " + e.getMessage());
				e.printStackTrace();
			}
			
			for (BOProduct i : productList) {
				checkString = i.product.getShortdescription();
				if (searchTherm != null && !searchTherm.isEmpty() && checkString.toLowerCase().contains(searchTherm.toLowerCase())) {
					valueExist = true;
					this.appendArticles(doc, i);
				}
			}
			if (!valueExist)
				errorList.add("No article with the short description '" + searchTherm + "' found.");

			return doc;	
			
		} else { //Search the whole Catalog without searchtherm
			try {
				doc = this.getDocument();
				this.createDom(doc);
			} catch (ParserConfigurationException e) {
				this.errorList.add("Parser Exception: createProductCatalog: " + e.getMessage());
				e.printStackTrace();
			}
			for (BOProduct i : productList) {
				this.appendArticles(doc, i);
			}
			return doc;			
		}
	}


	/** Creates an XML file out of an specific DOM (with or without searchtherm)
	 * @param userId used for the Filename
	 * @param searchTherm to hand the search to the other Method
	 * @return the filename to the file
	 */
	public String writeDomIntoFile(String userId, String searchTherm) {
		Document doc = this.exportCatalog(searchTherm);
		String fileName = "Procuct_catalog_export_" + userId + ".xml";
		File file = null;
		
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			//Create DOM from document
			DOMSource source = new DOMSource(doc);
			
			file = new File(this.context.getRealPath(fileName));
			StreamResult result = new StreamResult(file);
			
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			this.errorList.add("Transform config Error writeDomIntoFile(): " + e.getMessage());
			e.printStackTrace();
		} catch (TransformerException e) {
			this.errorList.add("Transform Error writeDomIntoFile(): " + e.getMessage());
			e.printStackTrace();
		}
		return fileName;
	}

	/** Creates with the xslt file an xhtml file
	 * @param filePath
	 * @param userId
	 * @return
	 */
	public String catalogToXHTML(String filePath, String userId) {
		String fileName = "Product_catalog_export_" + userId + ".XHTML";
		File file = new File(this.context.getRealPath(fileName));
		String xmlPath = this.context.getRealPath(filePath);
		String xsltPath = this.context.getRealPath("/wsdl/xhtml.xslt");
		
		try {
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer;
			StreamSource xmlSource = new StreamSource(xmlPath);
			StreamSource xsltSource = new StreamSource(xsltPath);
			
			transformer = factory.newTransformer(xsltSource);
			transformer.transform(xmlSource, new StreamResult(file));
		} catch (TransformerConfigurationException e) {
			this.errorList.add("Transform config Error catalogToXHTML():  " + e.getMessage());
			e.printStackTrace();
		} catch (TransformerException e) {
			this.errorList.add("Transform Error catalogToXHTML(): " + e.getMessage());
			e.printStackTrace();
		}
		return fileName;
	}
	
	/** Start download of the file from the filepath parameter
	 * @param filePath of the file to downlod
	 */
	public void createFileForExport(String filePath) {
		try {
			this.response.setContentType("text/xml");
			this.response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
	
			File file = new File(this.context.getRealPath(filePath));
			FileInputStream fileIn = new FileInputStream(file);
			ServletOutputStream out = this.response.getOutputStream();

			int bytesRead;
			byte[] bytes = new byte[4096];
			while ((bytesRead = fileIn.read(bytes)) != -1) {
				out.write(bytes, 0, bytesRead);
			}
			fileIn.close();
			out.flush();
			out.close();
		} catch (IOException e) {
			this.errorList.add(" Create File Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/** Creates the rootelemets with the Header Elements in XML in bmecat style
	 * @param doc empty doc element
	 * @throws ParserConfigurationException
	 */
	public void createDom(Document doc) throws ParserConfigurationException {
		
		// ROOT
		Element root = doc.createElement("BMECAT");
		root.setAttribute("version", "1.2");
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
		// CHILDELEMENTS HEADER
		Element header = doc.createElement("HEADER");
		Element catalog = doc.createElement("CATALOG");
		Element language = doc.createElement("LANGUAGE");
		Element catalog_id = doc.createElement("CATALOG_ID");
		Element catalog_version = doc.createElement("CATALOG_VERSION");
		Element catalog_name = doc.createElement("CATALOG_NAME");
		Element supplier = doc.createElement("SUPPLIER");
		Element supplier_name = doc.createElement("SUPPLIER_NAME");
		Element t_new_catalog = doc.createElement("T_NEW_CATALOG");
		
		// APPENDCHILDS
		doc.appendChild(root); 
		root.appendChild(header); 
		header.appendChild(catalog);
		catalog.appendChild(language);
		language.insertBefore(doc.createTextNode("deu"), language.getLastChild());
		catalog.appendChild(catalog_id);
		catalog_id.insertBefore(doc.createTextNode("HTWG-EBUS-SS17"), catalog_id.getLastChild());
		catalog.appendChild(catalog_version);
		catalog_version.insertBefore(doc.createTextNode("1.0"), catalog_version.getLastChild());
		catalog.appendChild(catalog_name);
		catalog_name.insertBefore(doc.createTextNode("exported productcatalog"), catalog_name.getLastChild());
		header.appendChild(supplier);
		supplier.appendChild(supplier_name);
		supplier_name.insertBefore(doc.createTextNode("KN MEDIA STORE"), supplier_name.getLastChild());
		root.appendChild(t_new_catalog);
	}
	

	/** Creates for each article element an article
	 * @param doc the actual document to edit
	 * @param product one product out of the productlist
	 */
	public void appendArticles(Document doc, BOProduct product) {
		
		// ROOT
		Node t_new_catalog = doc.getElementsByTagName("T_NEW_CATALOG").item(0);
		
		// CHILDELEMENTS
		Element article = doc.createElement("ARTICLE");
		Element supplier_aid = doc.createElement("SUPPLIER_AID");
		Element article_details = doc.createElement("ARTICLE_DETAILS");
		Element description_short = doc.createElement("DESCRIPTION_SHORT");
		Element description_long = doc.createElement("DESCRIPTION_LONG");
		Element article_order_details = doc.createElement("ARTICLE_ORDER_DETAILS");
		Element order_unit = doc.createElement("ORDER_UNIT");
		Element content_unit = doc.createElement("CONTENT_UNIT");
		Element no_cu_per_ou = doc.createElement("NO_CU_PER_OU");
		Element article_price_details = doc.createElement("ARTICLE_PRICE_DETAILS");
		
		// APPEND CHILDS
		t_new_catalog.appendChild(article);
		supplier_aid.insertBefore(doc.createTextNode(product.getOrderNumberSupplier()),supplier_aid.getLastChild());
		article.appendChild(supplier_aid);
		article.appendChild(article_details);
		description_short.insertBefore(doc.createTextNode(product.getShortDescription()),description_short.getLastChild());
		article_details.appendChild(description_short);
		description_long.insertBefore(doc.createTextNode(product.getLongDescription()),description_long.getLastChild());
		article_details.appendChild(description_long);
		article.appendChild(article_order_details);
		order_unit.insertBefore(doc.createTextNode("PK"), order_unit.getLastChild());
		article_order_details.appendChild(order_unit);
		content_unit.insertBefore(doc.createTextNode("C62"), content_unit.getLastChild());
		article_order_details.appendChild(content_unit);
		no_cu_per_ou.insertBefore(doc.createTextNode("10"), no_cu_per_ou.getLastChild());
		article_order_details.appendChild(no_cu_per_ou);
		article.appendChild(article_price_details);
		this.appendPrice(doc, article_price_details, product);
	}

	/** Creates the price elements in every article element 
	 * @param document the actual document to edit
	 * @param element whole article price details elements
	 * @param product the actual product
	 */
	public void appendPrice(Document document, Element element, BOProduct product) {
		
		Element article_price_details = element;
		List<BOSalesPrice> salesPrices = product.getSalesPrices();
		
		for (BOSalesPrice salePrice : salesPrices) {
			
			// CHILDELEMENTS
			Element article_price = document.createElement("ARTICLE_PRICE");
			Element price_amount = document.createElement("PRICE_AMOUNT");
			Element price_currency = document.createElement("PRICE_CURRENCY");
			Element tax = document.createElement("TAX");
			Element territory = document.createElement("TERRITORY");
			
			// APPREND CHILDS
			article_price.setAttribute("price_type", salePrice.getPricetype());
			article_price_details.appendChild(article_price);
			article_price.appendChild(price_amount);
			price_amount.insertBefore(document.createTextNode(salePrice.getAmount().toString()),price_amount.getLastChild());
			article_price.appendChild(price_currency);
			price_currency.insertBefore(document.createTextNode(salePrice.getCountry().getCurrency().getCode()),price_currency.getLastChild());
			article_price.appendChild(tax);
			tax.insertBefore(document.createTextNode(salePrice.getTaxrate().toString()), tax.getLastChild());
			article_price.appendChild(territory);
			territory.insertBefore(document.createTextNode(salePrice.getCountry().getIsocode()),territory.getLastChild());
		}
	}

}
