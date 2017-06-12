package de.htwg_konstanz.ebus.wholesaler.main;

import de.htwg_konstanz.ebus.framework.wholesaler.api.bo.*;
import de.htwg_konstanz.ebus.framework.wholesaler.api.boa.*;
import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Marco
 *
 */
public class ImportXMLSaveToDB {

	private static XPathFactory xPathFactory = XPathFactory.newInstance();
	private static XPath xpath = xPathFactory.newXPath();
	private static Document doc;
	private static boolean imported = true;
	private static ArrayList<String> errorList;
	private static String supplierName;

	/**
	 * @param doc Valid dom document
	 * @param errorList
	 */
	@SuppressWarnings("static-access")
	public ImportXMLSaveToDB(Document doc, ArrayList<String> errorList) {
		this.errorList = errorList;
		this.doc = doc;
	}

	/** calls the method which checks existing suppliers
	 * calls all methods to save data into the DB
	 * @return returns if the save was successful or not
	 */
	
	public boolean importArticles() {
		NodeList supplierAids = getSupplierArticleIDs();
		
		if (supplierExists(doc, xpath)) {
			
			importProducts(doc, xpath);
			importProductPrices(supplierAids);
			
			if (imported)
				errorList.add("XML successfully imported");
			return true;
		}
		return false;
	}

	/** extract all Supplier AIDs out of the document with xpath
	 * @return
	 */
	public static NodeList getSupplierArticleIDs() {
		String articleXpath = "/BMECAT/T_NEW_CATALOG/ARTICLE/SUPPLIER_AID/text()";
		
		try {
			return (NodeList) xpath.compile(articleXpath).evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** Check if Supplier exists  in DB or not
	 * @param document valid dom document
	 * @param xpath path instance
	 * @return found or not
	 */
	public static boolean supplierExists(Document document, XPath xpath) {
		final String supplierXpath = "/BMECAT/HEADER/SUPPLIER/SUPPLIER_NAME/text()";
		SupplierBOA supplierBOA = SupplierBOA.getInstance();
		
		try {
			XPathExpression exprSupplier = xpath.compile(supplierXpath);
			Node supplier = (Node) exprSupplier.evaluate(document, XPathConstants.NODE);
			
			supplierName = supplier.getNodeValue();
			List<BOSupplier> boSuppliers = supplierBOA.findByCompanyName(supplierName);
			
			if (boSuppliers.isEmpty()) {
				errorList.add("Supplier not found error: Supplier does not exist in DB");
				imported = false;
				return false;
			}
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			imported = false;
			errorList.add("xPath error: " + e.getMessage());
		}
		return true;
	}

	/** Imports the products into the database (Only in the case they don´t exist already)
	 * @param doc valid dom document
	 * @param xpath xpath instance 
	 */
	public static void importProducts(Document doc, XPath xpath) {
		
		//probably failure
		final String longDescriptionXPATH = "/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_DETAILS/DESCRIPTION_LONG";
		final String shortDescriptionXPATH = "/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_DETAILS/DESCRIPTION_SHORT";
		final String orderNumberSupplierXPATH = "/BMECAT/T_NEW_CATALOG/ARTICLE/SUPPLIER_AID/text()";
		String shortDescriptionValue;
		String longDescriptionValue;
		String supplierAidValue;
		System.out.println("Changed : 1");
		try {
			NodeList supplierAid = (NodeList) xpath.compile(orderNumberSupplierXPATH).evaluate(doc,XPathConstants.NODESET);
			NodeList shortDescriptions = (NodeList) xpath.compile(shortDescriptionXPATH).evaluate(doc,XPathConstants.NODESET);
			NodeList longDescriptions = (NodeList) xpath.compile(longDescriptionXPATH).evaluate(doc,XPathConstants.NODESET);

			for (int i = 0; i < shortDescriptions.getLength(); i++) {
				supplierAidValue = supplierAid.item(i).getNodeValue();
				
				if (ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue) == null) {

					BOProduct boProduct = new BOProduct();

					shortDescriptionValue = shortDescriptions.item(i).getFirstChild().getNodeValue();
					longDescriptionValue = longDescriptions.item(i).getFirstChild().getNodeValue();
					
					boProduct.setOrderNumberSupplier(supplierAidValue);
					boProduct.setOrderNumberCustomer(supplierAidValue);
					boProduct.setShortDescription(shortDescriptionValue);
					boProduct.setLongDescription(longDescriptionValue);
					boProduct.setInventoryAmount(10);
					boProduct.setSupplier(SupplierBOA.getInstance().findByCompanyName(supplierName).iterator().next());
					
					errorList.add("Insert successfully Product: " + supplierAidValue);
					imported = true;
					ProductBOA.getInstance().saveOrUpdate(boProduct);

				} else {
					imported = false;
					errorList.add("Product import error: " + supplierAidValue + " exists in DB");
				}
			}
			commit();
		} catch (XPathExpressionException e) {
			errorList.add("xPath error: " +e);
			_BaseBOA.getInstance().rollback();
			imported = false;
			e.printStackTrace();
		}
	}

	/** Import of the Product prices for every single product and calls methods
	 * @param supplierAids
	 */
	public static void importProductPrices(NodeList supplierAids) {
		String articlePriceXpath = "/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_PRICE_DETAILS/ARTICLE_PRICE/";
		String[] priceDetails = { "PRICE_AMOUNT", "PRICE_CURRENCY", "TAX", "TERRITORY", "@price_type" };
		
		try {
			for (int i = 0; i < supplierAids.getLength(); i++) {
				
				String whereSupplierAid = "[../../../SUPPLIER_AID/text()=\"" + supplierAids.item(i).getNodeValue()+ "\"]";
				String supplierAidValue = SupplierBOA.getInstance().findByCompanyName(supplierName).get(0).getSupplierNumber() + supplierAids.item(i).getNodeValue();
				
				NodeList priceAmounts = (NodeList) xpath.compile(articlePriceXpath + priceDetails[0] + whereSupplierAid).evaluate(doc, XPathConstants.NODESET);
				NodeList priceTaxes = (NodeList) xpath.compile(articlePriceXpath + priceDetails[2] + whereSupplierAid).evaluate(doc, XPathConstants.NODESET);
				NodeList priceTerritories = (NodeList) xpath.compile(articlePriceXpath + priceDetails[3] + whereSupplierAid).evaluate(doc, XPathConstants.NODESET);
				NodeList priceTypes = (NodeList) xpath.compile(articlePriceXpath + priceDetails[4] + whereSupplierAid).evaluate(doc, XPathConstants.NODESET);
				
				for (int j = 0; j < priceAmounts.getLength(); j++) {
					for (int k = 0; k < priceTerritories.getLength(); k++) {
						
						//imports Purchase PRICE, method call
						importPurchasePrice(
								ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue),
								new BigDecimal(priceAmounts.item(j).getFirstChild().getNodeValue()),
								priceTypes.item(j).getFirstChild().getNodeValue(),
								new BigDecimal(priceTaxes.item(j).getFirstChild().getNodeValue()),
								1,
								CountryBOA.getInstance().findCountry(priceTerritories.item(k).getFirstChild().getNodeValue())
								);
						
						//imports Sales PRICE, method call
						importSalesPrice(
								ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue),
								new BigDecimal(priceAmounts.item(j).getFirstChild().getNodeValue()),
								priceTypes.item(j).getFirstChild().getNodeValue(),
								new BigDecimal(priceTaxes.item(j).getFirstChild().getNodeValue()),
								1,
								CountryBOA.getInstance().findCountry(priceTerritories.item(k).getFirstChild().getNodeValue())					
								);
					}
					commit();
				}
			}
		} catch (XPathExpressionException e) {
			errorList.add("xPath error: " +e);
			imported = false;
			_BaseBOA.getInstance().rollback();
			e.printStackTrace();
		}
	}

	/** saves the Purchase Prices into DB
	 * @param product 
	 * @param amount
	 * @param priceType
	 * @param taxrate
	 * @param lowerboundScaledprice
	 * @param country
	 */
	public static void importPurchasePrice(BOProduct product, BigDecimal amount, String priceType, BigDecimal taxrate, Integer lowerboundScaledprice, BOCountry country) {
		
		BOPurchasePrice price = new BOPurchasePrice();
		price.setProduct(product);
		price.setAmount(amount);
		price.setPricetype(priceType);
		price.setTaxrate(taxrate);
		price.setLowerboundScaledprice(lowerboundScaledprice);
		price.setCountry(country);
		PriceBOA.getInstance().saveOrUpdate(price);
		commit();
	}

	/** saves the Sales price into DB
	 * @param product
	 * @param amount
	 * @param priceType
	 * @param taxrate
	 * @param lowerboundScaledprice
	 * @param country
	 */
	public static void importSalesPrice(BOProduct product, BigDecimal amount, String priceType, BigDecimal taxrate, Integer lowerboundScaledprice, BOCountry country) {
		
		BOSalesPrice price = new BOSalesPrice();
		price.setProduct(product);
		price.setAmount(amount);
		price.setPricetype(priceType);
		price.setTaxrate(taxrate);
		price.setLowerboundScaledprice(lowerboundScaledprice);
		price.setCountry(country);
		PriceBOA.getInstance().saveOrUpdate(price);
		commit();
	}

	/**Commits the DB
	 * 
	 */
	public static void commit() {
		_BaseBOA.getInstance().commit();
		_BaseBOA.getInstance().getSession().close();
	}

}
