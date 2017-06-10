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

public class ExportXMLUtil {

	private HttpServletRequest request;
	private HttpServletResponse response;
	ServletContext context;
	private ArrayList<String> errorList;

	public ExportXMLUtil(HttpServletRequest request, HttpServletResponse response, ArrayList<String> errorList) {
		this.request = request;
		this.response = response;
		this.errorList = errorList;
		this.context = request.getSession().getServletContext();
	}

	public ArrayList<String> getErrorList() {
		return errorList;
	}

	public Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		Document doc = documentBuilder.newDocument();
		return doc;
	}

	public String exportFile(String userId) {
		String exportType = this.request.getParameter("exportType");
		String search = this.request.getParameter("searchTherm");
		String tmpPath = null;
		String filePath = null;
		System.out.println(exportType);
		
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

	public Document exportCatalog(String searchTherm) {
		Document doc = null;
		
		if (!searchTherm.isEmpty() && searchTherm != null)
			doc = this.createProductCatalog(searchTherm);
		else
			doc = this.createProductCatalog();
		return doc;
	}

	public Document createProductCatalog() {
		Document doc = null;		
		List<BOProduct> productList = ProductBOA.getInstance().findAll();
		
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

	public Document createProductCatalog(String search) {
		Document doc = null;
		String checkString;
		Boolean valueExist = false;
		List<BOProduct> productList = ProductBOA.getInstance().findAll();
		
		try {
			doc = this.getDocument();
			this.createDom(doc);
		} catch (ParserConfigurationException e) {
			this.errorList.add("Parser Exception: createProductCatalog(searchTherm): " + e.getMessage());
			e.printStackTrace();
		}
		
		for (BOProduct i : productList) {
			checkString = i.product.getShortdescription();
			if (search != null && !search.isEmpty() && checkString.toLowerCase().contains(search.toLowerCase())) {
				valueExist = true;
				this.appendArticles(doc, i);
			}
		}
		if (!valueExist)
			errorList.add("No article with the short description '" + search + "' found.");

		return doc;
	}

	public String writeDomIntoFile(String userId, String searchTherm) {
		Document document = this.exportCatalog(searchTherm);
		String path = "Procuct_catalog_export_" + userId + ".xml";
		File file = null;
		
		try {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(document);
			file = new File(this.context.getRealPath(path));
			StreamResult result = new StreamResult(file);
			transformer.transform(source, result);
		} catch (TransformerConfigurationException e) {
			this.errorList.add("Transform config Error writeDomIntoFile(): " + e.getMessage());
			e.printStackTrace();
		} catch (TransformerException e) {
			this.errorList.add("Transform Error writeDomIntoFile(): " + e.getMessage());
			e.printStackTrace();
		}
		return path;
	}

	public String catalogToXHTML(String pathToTransform, String userId) {
		String path = "Product_catalog_export_" + userId + ".XHTML";
		File file = new File(this.context.getRealPath(path));
		String xmlPath = this.context.getRealPath(pathToTransform);
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
		return path;
	}
	
	public void createFileForExport(String path) {
		try {
			this.response.setContentType("text/xml");
			this.response.setHeader("Content-Disposition", "attachment;filename=" + path);
	
			File file = new File(this.context.getRealPath(path));
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

	public void createDom(Document doc) throws ParserConfigurationException {
		
		// ROOT
		Element root = doc.createElement("BMECAT");
		root.setAttribute("version", "1.2");
		root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
		// CHILDELEMENTS
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
