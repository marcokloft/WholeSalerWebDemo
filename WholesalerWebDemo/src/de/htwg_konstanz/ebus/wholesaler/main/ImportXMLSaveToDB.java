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

	@SuppressWarnings("static-access")
	public ImportXMLSaveToDB(Document doc, ArrayList<String> errorList) {
		this.errorList = errorList;
		this.doc = doc;
	}

	public boolean importArticles() {
		NodeList supplierAids = getSuppliers();
		importSupplier(doc, xpath);
		if (supplierExists) {
			importProducts(doc, xpath);
			importProductPrices(supplierAids);
			if (imported)
				errorList.add("XML successfully imported");
			return true;
		}
		return false;
	}

	public static NodeList getSuppliers() {
		String articleXpath = "/BMECAT/T_NEW_CATALOG/ARTICLE/SUPPLIER_AID/text()";
		try {
			return (NodeList) xpath.compile(articleXpath).evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean importSupplier(Document document, XPath xpath) {
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

	public static void importProducts(Document document, XPath xpath) {
		final String[] desciptionXpaths = { "/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_DETAILS/DESCRIPTION_LONG",
				"/BMECAT/T_NEW_CATALOG/ARTICLE/ARTICLE_DETAILS/DESCRIPTION_SHORT" };
		final String orderNumberSupplier = "/BMECAT/T_NEW_CATALOG/ARTICLE/SUPPLIER_AID/text()";
		String shortDescriptionValue;
		String longDescriptionValue;
		String suplierAidValue;
		try {
			NodeList suplierAid = (NodeList) xpath.compile(orderNumberSupplier).evaluate(document,XPathConstants.NODESET);
			NodeList shortDescriptions = (NodeList) xpath.compile(desciptionXpaths[0]).evaluate(document,XPathConstants.NODESET);
			NodeList longDescriptions = (NodeList) xpath.compile(desciptionXpaths[1]).evaluate(document,XPathConstants.NODESET);

			for (int i = 0; i < shortDescriptions.getLength(); i++) {
				suplierAidValue = suplierAid.item(i).getNodeValue();

				String supplierNew = SupplierBOA.getInstance().findByCompanyName(supplierName).get(0).getSupplierNumber() + suplierAidValue;
				if (ProductBOA.getInstance().findByOrderNumberSupplier(supplierNew) == null) {
					System.out.println(supplierNew);
					BOProduct boProduct = new BOProduct();
					ProductBOA.getInstance().findByOrderNumberSupplier(suplierAid.item(i).getNodeValue());
					if (!((SupplierBOA.getInstance().findByCompanyName(supplierName).get(0).getSupplierNumber()
							+ suplierAid).equals(supplierNew))) {
						if (ProductBOA.getInstance().findByOrderNumberSupplier(supplierNew) == null) {
							shortDescriptionValue = shortDescriptions.item(i).getFirstChild().getNodeValue();
							longDescriptionValue = longDescriptions.item(i).getFirstChild().getNodeValue();
							boProduct.setOrderNumberSupplier(supplierNew);
							boProduct.setOrderNumberCustomer(supplierNew);
							boProduct.setShortDescription(shortDescriptionValue);
							boProduct.setLongDescription(longDescriptionValue);
							boProduct.setInventoryAmount(1000);
							boProduct.setSupplier(SupplierBOA.getInstance().findByCompanyName(supplierName).iterator().next());
							errorList.add("Insert successfully PN: " + supplierNew);
							imported = true;
							ProductBOA.getInstance().saveOrUpdate(boProduct);
						}
					}
				} else {
					imported = false;
					errorList.add("Product import error: " + supplierNew + " exists in DB");
				}
			}
			commit();
		} catch (XPathExpressionException e) {
			_BaseBOA.getInstance().rollback();
			imported = false;
			e.printStackTrace();
		}
	}

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
						
						//imports Purchase PRICE
						importPurchasePrice(ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue),
								new BigDecimal(priceAmounts.item(j).getFirstChild().getNodeValue()),
								priceTypes.item(j).getFirstChild().getNodeValue(),
								new BigDecimal(priceTaxes.item(j).getFirstChild().getNodeValue()), 1,
								CountryBOA.getInstance()
										.findCountry(priceTerritories.item(k).getFirstChild().getNodeValue()));
						
						//imports Sales PRICE
						importSalesPrice(ProductBOA.getInstance().findByOrderNumberSupplier(supplierAidValue),
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

	public static void commit() {
		_BaseBOA.getInstance().commit();
		_BaseBOA.getInstance().getSession().close();
	}

}
