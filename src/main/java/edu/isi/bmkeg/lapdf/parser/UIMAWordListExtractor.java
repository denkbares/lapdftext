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
	//if this is true, alls blocks will be normalized to the coordinates of top-left coordinates of the page
	//this prevents differences for same document category but different DPIs
	private boolean normalize;
	private Block cropBlock;

	public UIMAWordListExtractor(File xmlFile) {
		this(xmlFile, false);
	}

	public UIMAWordListExtractor(File xmlFile, boolean normalize) {
		dom = getDom(xmlFile);
		this.xmlFile = xmlFile;
		this.normalize = normalize;
	}

	public List<WordBlock> getWordBlockList(int pageNo) {

		List<WordBlock> resultList = new LinkedList<>();
		Node page = dom.getElementsByTagName("PAGE").item(pageNo - 1);

		if (page.getNodeType() == Node.ELEMENT_NODE) {
			Element elementPage = (Element) page;
			NodeList tokens = elementPage.getElementsByTagName("TOKEN");
			if (normalize) {
				Node cropbox = elementPage.getElementsByTagName("CROPBOX").item(0);
				if (cropbox.getNodeType() == Node.ELEMENT_NODE) {
					Element cropboxElement = (Element) cropbox;
					cropBlock = new Block(cropboxElement).invokeCropbox();
				}
			}
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
			Block elementBlock = new Block(element).invokeToken();
			int x1 = elementBlock.getX1();
			int x2 = elementBlock.getX2();
			int y1 = elementBlock.getY1();
			int y2 = elementBlock.getY2();
			if (normalize) {
				x1 = x1 - cropBlock.getX1();
				x2 = x2 - cropBlock.getX1();
				y1 = y1 - cropBlock.getY1();
				y2 = y2 - cropBlock.getY1();
			}
			int spaceWidth = 0;
			if (!element.getTextContent().isEmpty()) {
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

	private class Block {
		private Element element;
		private int x1;
		private int x2;
		private int y1;
		private int y2;

		public Block(Element element) {
			this.element = element;
		}

		public int getX1() {
			return x1;
		}

		public int getX2() {
			return x2;
		}

		public int getY1() {
			return y1;
		}

		public int getY2() {
			return y2;
		}

		public Block invokeToken() {
			x1 = getIntAttribute(element, "x");
			x2 = x1 + getIntAttribute(element, "width");
			y1 = getIntAttribute(element, "y");
			y2 = y1 + getIntAttribute(element, "height");
			return this;
		}

		public Block invokeCropbox() {
			x1 = getIntAttribute(element, "x1");
			x2 = getIntAttribute(element, "x2");
			y1 = getIntAttribute(element, "y1");
			y2 = getIntAttribute(element, "x2");
			return this;
		}

		private int getIntAttribute(Element element, String name) {
			return (int) Double.parseDouble(element.getAttribute(name));
		}
	}
}
