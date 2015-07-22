package com.restReview.dataCollection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModelBuilder {

	public static void parseXmlFile(String baseFolderPath)
			throws ParserConfigurationException, SAXException, IOException {
		// get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		DocumentBuilder db = dbf.newDocumentBuilder();
		// parse using builder to get DOM representation of the XML file
		Document dom = db.parse(baseFolderPath + File.separator
				+ "Restaurants_Train.xml");
		FileWriter fileWriter = new FileWriter(new File(baseFolderPath
				+ File.separator + "DataModel.txt"));
		// get the root elememt
		Element rootElement = dom.getDocumentElement();
		NodeList nodeList = rootElement.getElementsByTagName("sentence");
		if (nodeList != null && nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				String textVal = "";
				Element element = (Element) nodeList.item(i);
				NodeList textList = element.getElementsByTagName("text");
				if (textList != null && textList.getLength() > 0) {
					Element e = (Element) textList.item(0);
					textVal = e.getFirstChild().getNodeValue();
					textVal = textVal.replaceAll("[^A-Za-z0-9]", " ");
					textVal = textVal.replace("\\s+", " ");
				}

				NodeList aspectCategories = element
						.getElementsByTagName("aspectCategories");
				if (aspectCategories != null
						&& aspectCategories.getLength() > 0) {
					Element aspectCategoriesElement = (Element) aspectCategories
							.item(0);
					NodeList aspectCategoryList = aspectCategoriesElement
							.getElementsByTagName("aspectCategory");
					if (aspectCategoryList != null
							&& aspectCategoryList.getLength() > 0) {
						for (int j = 0; j < aspectCategoryList.getLength(); j++) {
							Element categoryElement = (Element) aspectCategoryList
									.item(j);
							String category = categoryElement
									.getAttribute("category");
							fileWriter.append(category + " " + textVal+"\n");
						}
					}
				}
			}
		}
		fileWriter.close();
	}
	
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException{
		parseXmlFile("/Users/kumaran_rajendiran/Desktop/NLP/ModelBuilder");
	}
}
