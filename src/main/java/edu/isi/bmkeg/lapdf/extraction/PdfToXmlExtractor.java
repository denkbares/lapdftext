package edu.isi.bmkeg.lapdf.extraction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.utils.FrequencyCounter;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;
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
public class PdfToXmlExtractor extends AbstractExtractor {

	private final File xmlFile;
	private Document dom;
	private Block cropBlock;
	private static final int maxSize = 1;

	private static LinkedHashMap<File, Document> fileToDocument = new LinkedHashMap<File, Document>(maxSize, 0.75f, false) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<File, Document> eldest) {
			return size() > maxSize;
		}
	};

	public PdfToXmlExtractor(File xmlFile, int startPage, int endPage) {
		//normalize to zero based pages
		super(startPage - 1, endPage - 1);
		this.xmlFile = xmlFile;

	}

	public PdfToXmlExtractor(File xmlFile) {
		this(xmlFile, 0, -1);
	}

	public List<WordBlock> getWordBlockList(int pageNo) {

		List<WordBlock> resultList = new LinkedList<>();
		Node page = dom.getElementsByTagName("PAGE").item(pageNo);
		if (page == null) {
			return null;
		}
		if (page.getNodeType() == Node.ELEMENT_NODE) {
			Element elementPage = (Element) page;
			NodeList tokens = elementPage.getElementsByTagName("TOKEN");
			cropBlock = getBoxBlock("CROPBOX", currentPage);
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
		WordBlock wordBlock = null;
		if (token.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) token;
			Block elementBlock = new Block(element).invokeToken();
			int x1 = elementBlock.getX1();
			int x2 = elementBlock.getX2();
			int y1 = elementBlock.getY1();
			int y2 = elementBlock.getY2();
			//normalize
			x1 = x1 - cropBlock.getX1();
			x2 = x2 - cropBlock.getX1();
			y1 = y1 - cropBlock.getY1();
			y2 = y2 - cropBlock.getY1();

			avgHeightFrequencyCounter.add(y2 - y1);
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
			wordBlock = modelFactory.createWordBlock(x1, y1, x2, y2, spaceWidth, font, style, element.getTextContent(), order);
		}
		return wordBlock;
	}

	private static synchronized Document getDom(File xmlFile) {
		if (fileToDocument.containsKey(xmlFile)) {
			return fileToDocument.get(xmlFile);
		}
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
		fileToDocument.put(xmlFile, dom);
		return dom;
	}

	@Override
	public int getCurrentPageBoxHeight() {
		Block block = getBoxBlock("MEDIABOX", currentPage - 1);
		return block.getHeight();
	}

	@Override
	public int getCurrentPageBoxWidth() {
		Block block = getBoxBlock("MEDIABOX", currentPage - 1);
		return block.getWidth();
	}

	@Override
	public IntegerFrequencyCounter getAvgHeightFrequencyCounter() {
		return avgHeightFrequencyCounter;
	}

	@Override
	public FrequencyCounter getFontFrequencyCounter() {
		return null;
	}

	@Override
	public IntegerFrequencyCounter getSpaceFrequencyCounter(int height) {
		return null;
	}

	@Override
	public void init(File file) throws Exception {
		this.dom = getDom(xmlFile);
		this.pdfFile = file;
	}

	@Override
	public boolean hasNext() {
		if (endPage > 0) {
			return getWordBlockList(currentPage) != null && currentPage <= endPage;
		}
		else {
			return getWordBlockList(currentPage) != null;
		}
	}

	@Override
	public List<WordBlock> next() {
		List<WordBlock> wordBlockList = getWordBlockList(currentPage);
		currentPage++;
		return new ArrayList<>(wordBlockList);
	}

	private Block getBoxBlock(String boxName, int pageNo) {
		Node page = dom.getElementsByTagName("PAGE").item(pageNo);
		Element pageElement = getElement(page);
		Node mediabox = null;
		if (pageElement != null) {
			mediabox = pageElement.getElementsByTagName(boxName).item(0);
		}
		Element mediaBoxElement = getElement(mediabox);
		return new Block(mediaBoxElement).invokeBox();
	}

	private Element getElement(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			return (Element) node;
		}
		return null;
	}

	private static class Block {
		private final Element element;
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

		public int getHeight() {
			return y2 - y1;
		}

		public int getWidth() {
			return x2 - x1;
		}

		public Block invokeToken() {
			x1 = getIntAttribute(element, "x");
			x2 = x1 + getIntAttribute(element, "width");
			y1 = getIntAttribute(element, "y");
			y2 = y1 + getIntAttribute(element, "height");
			return this;
		}

		public Block invokeBox() {
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
