package de.htwg_konstanz.ebus.wholesaler.main;

import de.htwg_konstanz.ebus.framework.wholesaler.api.bo.*;
import de.htwg_konstanz.ebus.framework.wholesaler.api.boa.*;
import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImportXMLSaveToDB {

	private static XPathFactory xPathFactory = XPathFactory.newInstance();
	private static XPath xpath = xPathFactory.newXPath();
	private static Document doc;
	private static boolean imported = true;
	private static ArrayList<String> errorList;
	private static boolean supplierExists = true;
	private static String supplierName;

	public ImportXMLSaveToDB(Document doc, ArrayList<String> errorList) {
		this.errorList = errorList;
		this.doc = doc;
	}

	/**
	 * calls functions to save Articles to DB
	 */
	public boolean saveArticles() {
		NodeList supplierAids = getSupplierAids();
		saveSupplier(doc, xpath);
		if (supplierExists) {
			saveProducts(doc, xpath);
			saveProductPrices(supplierAids);
			if (imported)
				errorList.add("XML imported");
			return true;
		}
		return false;
	}

	public static NodeList getSupplierAids() {
		String articleXpath = "/BMECAT/T_NEW_CATALOG/ARTICLE/SUPPLIER_AID/text()";
		try {
			return (NodeList) xpath.compile(articleXpath).evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * saves supplier when supplier with a given company doesn't exists
	 *
	 * @param document
	 * @param xpath
	 */
	public static boolean saveSupplier(Document document, XPath xpath) {
		final String supplierXpath = "/BMECAT/HEADER/SUPPLIER/SUPPLIER_NAME/text()";
		SupplierBOA supplierBOA = SupplierBOA.getInstance();
		try {
			XPathExpression exprSupplier = xpath.compile(supplierXpath);
			Node supplier = (Node) exprSupplier.evaluate(document, XPathConstants.NODE);
			supplierName = supplier.getNodeValue();
			List<BOSupplier> boSuppliers = supplierBOA.findByCompanyName(supplierName);
			if (boSuppliers.isEmpty()) {
				errorList.add("Supplier does not exist in DB");
				supplierExists = false;
				imported = false;
			}
			commit();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			imported = false;
			errorList.add(e.getMessage());
		}
		return true;
	}

	/**
	 * saves products for a given supplierID and upadates products when they
	 * already exsit in DB
	 *
	 * @param document
	 * @param xpath
	 */
	public static void saveProducts(Document document, XPath xpath) {
		final String[] desciptionXpaths = { "/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_DETAILS/DESCRIPTION_LONG",
				"/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_DETAILS/DESCRIPTION_SHORT" };
		final String orderNumberSupplier = "/BMECAT/T_NEW_CATALOG/ARTICLE/SUPPLIER_AID/text()";
		String shortDescriptionValue;
		String longDescriptionValue;
		String suplierAidValue;
		try {
			NodeList suplierAid = (NodeList) xpath.compile(orderNumberSupplier).evaluate(document,
					XPathConstants.NODESET);
			NodeList shortDescriptions = (NodeList) xpath.compile(desciptionXpaths[0]).evaluate(document,
					XPathConstants.NODESET);
			NodeList longDescriptions = (NodeList) xpath.compile(desciptionXpaths[1]).evaluate(document,
					XPathConstants.NODESET);

			for (int i = 0; i < shortDescriptions.getLength(); i++) {
				suplierAidValue = suplierAid.item(i).getNodeValue();

				String supplierAidNew = SupplierBOA.getInstance().findByCompanyName(supplierName).get(0)
						.getSupplierNumber() + suplierAidValue;
				if (ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidNew) == null) {
					System.out.println(supplierAidNew);
					BOProduct boProduct = new BOProduct();
					ProductBOA.getInstance().findByOrderNumberSupplier(suplierAid.item(i).getNodeValue());
					if (!((SupplierBOA.getInstance().findByCompanyName(supplierName).get(0).getSupplierNumber()
							+ suplierAid).equals(supplierAidNew))) {
						if (ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidNew) == null) {
							shortDescriptionValue = shortDescriptions.item(i).getFirstChild().getNodeValue();
							longDescriptionValue = longDescriptions.item(i).getFirstChild().getNodeValue();
							boProduct.setOrderNumberSupplier(supplierAidNew);
							boProduct.setOrderNumberCustomer(supplierAidNew);
							boProduct.setShortDescription(shortDescriptionValue);
							boProduct.setLongDescription(longDescriptionValue);
							boProduct.setInventoryAmount(1000);
							boProduct.setSupplier(
									SupplierBOA.getInstance().findByCompanyName(supplierName).iterator().next());
							errorList.add("inserted product with number: " + supplierAidNew);
							imported = true;
							ProductBOA.getInstance().saveOrUpdate(boProduct);
						}
					}
				} else {
					imported = false;
					errorList.add("product with number " + supplierAidNew + " exists");
				}
			}
			commit();
		} catch (XPathExpressionException e) {
			_BaseBOA.getInstance().rollback();
			imported = false;
			e.printStackTrace();
		}
	}

	/**
	 * saves product prices and references to the right product to a price, this
	 * method also checks the amount of territories for this price and saves the
	 * price referencing to a product one time for each category
	 *
	 * @param supplierAids
	 */
	public static void saveProductPrices(NodeList supplierAids) {
		String articlePriceXpath = "/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_PRICE_DETAILS/ARTICLE_PRICE/";
		String[] priceDetails = { "PRICE_AMOUNT", "PRICE_CURRENCY", "TAX", "TERRITORY", "@price_type" };
		try {
			for (int i = 0; i < supplierAids.getLength(); i++) {
				String whereSupplierAid = "[../../../SUPPLIER_AID/text()=\"" + supplierAids.item(i).getNodeValue()
						+ "\"]";
				String supplierAidValue = SupplierBOA.getInstance().findByCompanyName(supplierName).get(0)
						.getSupplierNumber() + supplierAids.item(i).getNodeValue();
				NodeList priceAmounts = (NodeList) xpath.compile(articlePriceXpath + priceDetails[0] + whereSupplierAid)
						.evaluate(doc, XPathConstants.NODESET);
				NodeList priceTaxes = (NodeList) xpath.compile(articlePriceXpath + priceDetails[2] + whereSupplierAid)
						.evaluate(doc, XPathConstants.NODESET);
				NodeList priceTerritories = (NodeList) xpath
						.compile(articlePriceXpath + priceDetails[3] + whereSupplierAid)
						.evaluate(doc, XPathConstants.NODESET);
				NodeList priceTypes = (NodeList) xpath.compile(articlePriceXpath + priceDetails[4] + whereSupplierAid)
						.evaluate(doc, XPathConstants.NODESET);
				for (int j = 0; j < priceAmounts.getLength(); j++) {
					for (int k = 0; k < priceTerritories.getLength(); k++) {
						savePurchasePrice(ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue),
								new BigDecimal(priceAmounts.item(j).getFirstChild().getNodeValue()),
								priceTypes.item(j).getFirstChild().getNodeValue(),
								new BigDecimal(priceTaxes.item(j).getFirstChild().getNodeValue()), 1,
								CountryBOA.getInstance()
										.findCountry(priceTerritories.item(k).getFirstChild().getNodeValue()));
						saveSalesPrice(ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue),
								new BigDecimal(priceAmounts.item(j).getFirstChild().getNodeValue()),
								priceTypes.item(j).getFirstChild().getNodeValue(),
								new BigDecimal(priceTaxes.item(j).getFirstChild().getNodeValue()), 1,
								CountryBOA.getInstance()
										.findCountry(priceTerritories.item(k).getFirstChild().getNodeValue()));
					}
					commit();
				}
			}
		} catch (XPathExpressionException e) {
			imported = false;
			_BaseBOA.getInstance().rollback();
			e.printStackTrace();
		}
	}

	/**
	 * saves data into PurchasePrice table
	 *
	 * @param product
	 * @param amount
	 * @param priceType
	 * @param taxrate
	 * @param lowerboundScaledprice
	 * @param country
	 */
	public static void savePurchasePrice(BOProduct product, BigDecimal amount, String priceType, BigDecimal taxrate,
			Integer lowerboundScaledprice, BOCountry country) {
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

	/**
	 * saves data into SalesPrice table
	 *
	 * @param product
	 * @param amount
	 * @param priceType
	 * @param taxrate
	 * @param lowerboundScaledprice
	 * @param country
	 */
	public static void saveSalesPrice(BOProduct product, BigDecimal amount, String priceType, BigDecimal taxrate,
		Integer lowerboundScaledprice, BOCountry country) {
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

	/**
	 * commits hibernate statements and closes the session
	 */
	public static void commit() {
		_BaseBOA.getInstance().commit();
		_BaseBOA.getInstance().getSession().close();
	}

}
