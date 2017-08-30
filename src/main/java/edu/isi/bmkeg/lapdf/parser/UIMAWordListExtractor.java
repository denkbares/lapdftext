package edu.isi.bmkeg.lapdf.parser;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import edu.isi.bmkeg.lapdf.model.RTree.RTWordBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.denkbares.utils.Log;

/**
 * @author Stefan Plehn (denkbares GmbH)
 * @created 18.07.17
 */
public class UIMAWordListExtractor {

	private final Document dom;
	private final File xmlFile;

	public UIMAWordListExtractor(File xmlFile) {
		dom = getDom(xmlFile);
		this.xmlFile = xmlFile;
	}

	public List<WordBlock> getWordBlockList(int pageNo) {

		List<WordBlock> resultList = new LinkedList<>();
		Node page = dom.getElementsByTagName("PAGE").item(pageNo - 1);

		if (page.getNodeType() == Node.ELEMENT_NODE) {
			Element elementPage = (Element) page;
			NodeList tokens = elementPage.getElementsByTagName("TOKEN");
			resultList = getWordBlocksFromTokens(tokens);
		}
		return resultList;
	}

	private List<WordBlock> getWordBlocksFromTokens(NodeList tokens) {
		List<WordBlock> resultList = new LinkedList<>();
		for (int i = 0; i < tokens.getLength(); i++) {
			Node token = tokens.item(i);
			WordBlock wb = getWordBlock(token, i);
			resultList.add(wb);
		}
		return resultList;
	}

	private WordBlock getWordBlock(Node token, int order) {
		RTWordBlock rtWordBlock = null;
		if (token.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) token;
			int x1 = getIntAttribute(element, "x");
			int x2 = x1 + getIntAttribute(element, "width");
			int y1 = getIntAttribute(element, "y");
			int y2 = y1 + getIntAttribute(element, "height");
			int spaceWidth = 0;
			if (element.getTextContent().length() != 0) {
				spaceWidth = (x2 - x1) / element.getTextContent().length();
			}
			String font = element.getAttribute("font-name");
			String fontSize = element.getAttribute("font-size");
			String style = "";
			String bold = element.getAttribute("bold");
			String italic = element.getAttribute("italic");
			if (italic.equals("yes")) {
				style = "Italic";
			}
			if (bold.equals("yes")) {
				style = "Bold";
			}
			rtWordBlock = new RTWordBlock(x1, y1, x2, y2, spaceWidth, font, style, element.getTextContent(), order);
		}
		return rtWordBlock;
	}

	private int getIntAttribute(Element element, String name) {
		return (int) Double.parseDouble(element.getAttribute(name));
	}

	private Document getDom(File xmlFile) {
		//get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		//Using factory get an instance of document builder
		DocumentBuilder db;
		Document dom = null;
		try {
			db = dbf.newDocumentBuilder();
			//parse using builder to get DOM representation of the XML file
			dom = db.parse(xmlFile);
			dom.getDocumentElement().normalize();
		}
		catch (ParserConfigurationException e) {
			Log.severe("Error in parser configuration.", e);
		}
		catch (SAXException e) {
			Log.severe("Error within SAX's xml parser or writer.", e);
		}
		catch (IOException e) {
			Log.severe("Input/Output error.", e);
		}
		return dom;
	}
}
