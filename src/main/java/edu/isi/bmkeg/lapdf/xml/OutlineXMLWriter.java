package edu.isi.bmkeg.lapdf.xml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OutlineXMLWriter {

	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String ENCODING = "UTF-8";
	private static final String ELEMENT_TOCITEMS = "TOCITEMS";
	private static final String ELEMENT_TOCITEMLIST = "TOCITEMLIST";
	private static final String ELEMENT_ITEM = "ITEM";
	private static final String ELEMENT_STRING = "STRING";
	private static final String ELEMENT_LINK = "LINK";
	private static final String TOCITEMS_ATTRIBUTE_NBPAGES = "nbPages";
	private static final String TOCITEMLIST_ATTRIBUTE_LEVEL = "level";
	private static final String TOCITEMLIST_ATTRIBUTE_IDITEMPARENT = "idItemParent";
	private static final String ITEM_ATTRIBUTE_ID = "id";
	private static final String LINK_ATTRIBUTE_PAGE = "page";
	private static final String LINK_ATTRIBUTE_TOP = "top";
	private static final String LINK_ATTRIBUTE_BOTTOM = "bottom";
	private static final String LINK_ATTRIBUTE_LEFT = "left";
	private static final String LINK_ATTRIBUTE_RIGHT = "right";

	public void write(LapdfDocument document, String outputFilename) throws IOException {

		try {

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// root element (TOCITEMS)
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement(ELEMENT_TOCITEMS);
			rootElement.setAttribute(TOCITEMS_ATTRIBUTE_NBPAGES, Integer.toString(document.getTotalNumberOfPages()));
			doc.appendChild(rootElement);

			// root TOCITEMLIST (level 0)
			Map<Integer, Element> itemListsByLevel = new HashMap<>();
			Map<Integer, Element> lastElementByLevel = new HashMap<>();
			Element rootTocItemList = doc.createElement(ELEMENT_TOCITEMLIST);
			rootTocItemList.setAttribute(TOCITEMLIST_ATTRIBUTE_LEVEL, Integer.toString(0));
			itemListsByLevel.put(0, rootTocItemList);
			rootElement.appendChild(rootTocItemList);
			int lastLevel = 0;

			// EXPORT ITEMS
			int counter = 0;
			for (int page = 1; page <= document.getTotalNumberOfPages(); page++) {
				PageBlock pageBlock = document.getPage(page);
				List<ChunkBlock> chunks = pageBlock.getAllChunkBlocks(SpatialOrdering.VERTICAL_MODE);
				for (ChunkBlock chunk : chunks) {

					// get correct TOCITEMLIST element
					int level;
					switch (chunk.getType()) {
						case "Heading 1":
							level = 0;
							break;
						case "Heading 2":
							level = 1;
							break;
						case "Heading 3":
							level = 2;
							break;
						default:
							continue;
					}

					// prepare parent (open new toc item list for current level)
					if (level > 0 && level < lastLevel) {

						// create actual element (TOCITEMLIST)
						Element tocItemList = doc.createElement(ELEMENT_TOCITEMLIST);
						tocItemList.setAttribute(TOCITEMLIST_ATTRIBUTE_LEVEL, Integer.toString(level));
						doc.appendChild(tocItemList);

						// set correct parent
						Element parent = lastElementByLevel.get(level - 1);
						tocItemList.setAttribute(TOCITEMLIST_ATTRIBUTE_IDITEMPARENT, parent.getAttribute(ITEM_ATTRIBUTE_ID));
						itemListsByLevel.put(level, tocItemList);
					}

					// ITEM
					Element item = doc.createElement(ELEMENT_ITEM);
					item.setAttribute(ITEM_ATTRIBUTE_ID, Integer.toString(counter++));
					itemListsByLevel.get(level).appendChild(item);

					// STRING
					Element string = doc.createElement(ELEMENT_STRING);
					string.setTextContent(chunk.readChunkText());
					item.appendChild(string);

					// LINK
					Element link = doc.createElement(ELEMENT_LINK);
					link.setAttribute(LINK_ATTRIBUTE_PAGE, Integer.toString(page));
					link.setAttribute(LINK_ATTRIBUTE_TOP, Integer.toString(chunk.getY1()));
					link.setAttribute(LINK_ATTRIBUTE_BOTTOM, Integer.toString(chunk.getY2()));
					link.setAttribute(LINK_ATTRIBUTE_LEFT, Integer.toString(chunk.getX1()));
					link.setAttribute(LINK_ATTRIBUTE_RIGHT, Integer.toString(chunk.getX2()));
					item.appendChild(link);

					// references...
					lastElementByLevel.put(level, item);
					lastLevel = level;
				}
			}

			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(outputFilename));
			transformer.transform(source, result);

		}
		catch (ParserConfigurationException | TransformerException e) {
			throw new IOException(e);
		}
	}
}
