package edu.isi.bmkeg.lapdf.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import edu.isi.bmkeg.lapdf.extraction.Extractor;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTModelFactory;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;
import edu.isi.bmkeg.utils.FrequencyCounter;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;
import org.apache.log4j.Logger;

/**
 * @author Stefan Plehn (denkbares GmbH)
 * @created 11.07.17
 */
public class SpiralBlockParser implements Parser {

	private static final Logger logger = Logger.getLogger(RuleBasedParser.class);
	private final ArrayList<PageBlock> pageList;
	//	private JPedalExtractor pageExtractor;
	//private PDFBoxExtractor pageExtractor;
	private final Extractor pageExtractor;
	private final IntegerFrequencyCounter avgHeightFrequencyCounter;
	private final FrequencyCounter fontFrequencyCounter;
	private final boolean quickly = true;
	private final boolean debugImages = false;
	protected AbstractModelFactory modelFactory;
	private int idGenerator;
	private int northSouthSpacing;
	private int eastWestSpacing;
	private ParserStrategy parserStrategy;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public SpiralBlockParser(Extractor extractor) {
		modelFactory = new RTModelFactory();
		pageList = new ArrayList<PageBlock>();
		idGenerator = 1;
		this.avgHeightFrequencyCounter = new IntegerFrequencyCounter(1);
		this.fontFrequencyCounter = new FrequencyCounter();
		this.pageExtractor = extractor;
	}

	private void init(File file) throws Exception {

		pageExtractor.init(file);
		idGenerator = 1;

		this.avgHeightFrequencyCounter.reset();
		this.fontFrequencyCounter.reset();

		pageList.clear();
	}

	@Override
	public LapdfDocument parse(File file) throws Exception {
		if (file.getName().endsWith(".pdf")) {
			return this.parsePdf(file);
		}
		else {
			throw new Exception("File type of " + file.getName() + " not *.pdf or *_lapdf.xml");
		}
	}

	@SuppressWarnings("Duplicates")
	public LapdfDocument parsePdf(File file)
			throws Exception {

		LapdfDocument document = null;

		init(file);
		List<WordBlock> pageWordBlockList = null;
		PageBlock pageBlock = null;
		int pageCounter = 1;

		document = new LapdfDocument(file);
		document.setjPedalDecodeFailed(true);

		String pth = file.getPath();
		pth = pth.substring(0, pth.lastIndexOf(".pdf"));
		File imgDir = new File(pth);
//		if (isDebugImages()) {
//			imgDir.mkdir();
//		}


		//
		// Calling 'hasNext()' get the text from the extractor.
		//
		while (pageExtractor.hasNext()) {

			document.setjPedalDecodeFailed(false);
			pageWordBlockList = pageExtractor.next();

			pageBlock = modelFactory.createPageBlock(
					pageCounter,
					pageExtractor.getCurrentPageBoxWidth(),
					pageExtractor.getCurrentPageBoxHeight(),
					document);

			pageList.add(pageBlock);

			idGenerator = pageBlock.initialize(pageWordBlockList, idGenerator);

			if (!pageWordBlockList.isEmpty()) {
				this.eastWestSpacing = (pageBlock.getMostPopularWordHeightPage()) / 2
						+ pageBlock.getMostPopularHorizontalSpaceBetweenWordsPage();

				this.northSouthSpacing = (pageBlock.getMostPopularWordHeightPage()) / 2
						+ pageBlock.getMostPopularVerticalSpaceBetweenWordsPage();

				buildChunkBlocksByFanningOut(pageWordBlockList, pageBlock);

//				mergeHighlyOverlappedChunkBlocks(pageBlock);

			}

//			if (isDebugImages()) {
//				PageImageOutlineRenderer.dumpChunkTypePageImageToFile(
//						pageBlock,
//						new File(pth + "/_01_afterBuildBlocks" + pageBlock.getPageNumber() + ".png"),
//						file.getName() + "afterBuildBlocks"
//								+ pageBlock.getPageNumber() + ".png");
//			}
			pageCounter++;
		}

		document.addPages(pageList);

		document.calculateBodyTextFrame();
		document.calculateMostPopularFontStyles();

		return document;
	}

	private void buildChunkBlocksByFanningOut(List<WordBlock> wordBlocksLeftInPage,
											  PageBlock page) {

		LinkedBlockingQueue<WordBlock> wordBlocksLeftToCheckInChunk =
				new LinkedBlockingQueue<WordBlock>();

		List<WordBlock> chunkWords = new ArrayList<WordBlock>();
		List<WordBlock> rotatedWords = new ArrayList<WordBlock>();
		int counter;
		List<ChunkBlock> chunkBlockList1 = new ArrayList<ChunkBlock>();

		while (wordBlocksLeftInPage.size() > 0) {

			int minX = 1000, maxX = 0, minY = 1000, maxY = 0;
			wordBlocksLeftToCheckInChunk.clear();

			// Start off with this word block
			wordBlocksLeftToCheckInChunk.add(wordBlocksLeftInPage.get(0));

			counter = 0;
			int extra;

			// Here are all the words we've come across in this run
			chunkWords.clear();
			int chunkTextHeight = -1;

			// Build a single Chunk here based on overlapping words
			// keep going while there are still words to work through
			while (wordBlocksLeftToCheckInChunk.size() != 0) {

				//
				// get the top of the stack of words in the queue,
				// note that we have not yet looked at this word
				//
				// look at the top word in the queue
				WordBlock word = wordBlocksLeftToCheckInChunk.peek();

				if (chunkTextHeight == -1) {
					chunkTextHeight = word.getHeight();
				}

				// add this word's height and font to the counts.
				page.getDocument().getAvgHeightFrequencyCounter().add(
						word.getHeight());
				page.getDocument().getFontFrequencyCounter().add(
						word.getFont() + ";" + word.getFontStyle());

				// remove this word from the global search
				wordBlocksLeftInPage.remove(word);

				// heuristic to correct missing blocking errors for large fonts
				int eastWest = (int) Math.ceil(word.getSpaceWidth() * 1.2);
				int northSouth = (int) Math.ceil(word.getHeight() * 0.7);

				// what other words on the page are close to this word
				// and are still in the block?
				List<WordBlock> wordsToAddThisIteration = addWordsToThisIteration(word, eastWest, northSouth);

				// TODO how to add more precise word features without
				//word.writeFlushArray(wordsToAddThisIteration);

				wordsToAddThisIteration.retainAll(wordBlocksLeftInPage);

				// remove the words we've already looked at
				wordsToAddThisIteration.removeAll(wordBlocksLeftToCheckInChunk);

				// or they've already been seen
				wordsToAddThisIteration.removeAll(chunkWords);

				//
				// TODO Add criteria here to improve blocking by dropping newly found words that should be excluded.
				//
				List<WordBlock> wordsToKill = new ArrayList<WordBlock>();
				for (int i = 0; i < wordsToAddThisIteration.size(); i++) {
					WordBlock w = wordsToAddThisIteration.get(i);

					// They are a different height from the height
					// of the first word in this chunk +/- 1px
					// (and outside the current line for the chunk)
					if ((w.getHeight() > chunkTextHeight + 3 ||
							w.getHeight() < chunkTextHeight - 3) && (w.getY1() < minY || w.getY2() > maxY)
						// TODO sometimes it is the same chunk even if font style is not matching
						//|| !w.getFontStyle().equals(word.getFontStyle())
							) {
						wordsToKill.add(w);
					}
					//we start with i > 0, because only if chapter numbering gets captured by expaning from words we
					//want to kill it and not if it is the first word
//						if (i > 0) {
					String s = w.getWord();
					if (s.matches("\\d{3}\\.\\d{3}\\.\\d+")) {
						wordsToKill.add(w);
					}

//						}

				}
				wordsToAddThisIteration.removeAll(wordsToKill);

				// At this point, these words will be added to this chunk.
				wordBlocksLeftToCheckInChunk.addAll(wordsToAddThisIteration);

				// get this word from the queue and add it.
				WordBlock wb = wordBlocksLeftToCheckInChunk.poll();
				chunkWords.add(wb);
				wb.setOrderAddedToChunk(chunkWords.size());

				if (wb.getX1() < minX) minX = wb.getX1();
				if (wb.getX2() > maxX) maxX = wb.getX2();
				if (wb.getY1() < minY) minY = wb.getY1();
				if (wb.getY2() > maxY) maxY = wb.getY2();
			}

			wordBlocksLeftInPage.removeAll(chunkWords);

			ChunkBlock cb1 = buildChunkBlock(chunkWords, page);
			chunkBlockList1.add(cb1);
		}

		idGenerator = page.addAll(new ArrayList<SpatialEntity>(
				chunkBlockList1), idGenerator);
	}

	protected List<WordBlock> addWordsToThisIteration(WordBlock word, int eastWest, int northSouth) {
		if (parserStrategy == null) {
			parserStrategy = new ParserDefaultStrategy();
		}
		return parserStrategy.addWordsToThisIteration(word, eastWest, eastWest, northSouth, northSouth);
//		List<WordBlock> wordsToAddThisIteration;
//		wordsToAddThisIteration = word.readNearbyWords(
//				eastWest, eastWest, northSouth, northSouth);
//		return wordsToAddThisIteration;
	}

	public ChunkBlock buildChunkBlock(List<WordBlock> wordBlockList,
									  PageBlock pageBlock) {

		ChunkBlock chunkBlock = null;

		IntegerFrequencyCounter lineHeightFrequencyCounter = new IntegerFrequencyCounter(1);
		IntegerFrequencyCounter spaceFrequencyCounter = new IntegerFrequencyCounter(0);
		FrequencyCounter fontFrequencyCounter = new FrequencyCounter();
		FrequencyCounter styleFrequencyCounter = new FrequencyCounter();

		String fontStyle = "";
		for (WordBlock wordBlock : wordBlockList) {

			lineHeightFrequencyCounter.add(wordBlock.getHeight());
			spaceFrequencyCounter.add(wordBlock.getSpaceWidth());

			avgHeightFrequencyCounter.add(wordBlock.getHeight());
			if (wordBlock.getFont() != null) {
				fontFrequencyCounter.add(wordBlock.getFont());
			}
			else {
				fontFrequencyCounter.add("");
			}
			if (wordBlock.getFontStyle() != null && !wordBlock.getFontStyle().equals("")) {
				fontStyle = wordBlock.getFontStyle();
			}


			if (chunkBlock == null) {

				chunkBlock = modelFactory
						.createChunkBlock(wordBlock.getX1(), wordBlock.getY1(),
								wordBlock.getX2(), wordBlock.getY2(), wordBlock.getOrder());
			}
			else {

				SpatialEntity spatialEntity = chunkBlock.union(wordBlock);
				chunkBlock.resize(spatialEntity.getX1(), spatialEntity.getY1(),
						spatialEntity.getWidth(), spatialEntity.getHeight());
			}

			wordBlock.setContainer(chunkBlock);
			wordBlock.setPage(pageBlock);
		}

		chunkBlock.setMostPopularWordFont(
				(String) fontFrequencyCounter.getMostPopular()
		);

		chunkBlock.setMostPopularWordStyle(fontStyle);

		chunkBlock.setMostPopularWordHeight(
				lineHeightFrequencyCounter.getMostPopular()
		);

		//NOTE : SpaceWidths are never initialized!
		chunkBlock.setMostPopularWordSpaceWidth(
				spaceFrequencyCounter.getMostPopular()
		);

		chunkBlock.setContainer(pageBlock);

		return chunkBlock;
	}

	public void setParserStrategy(ParserStrategy parserStrategy) {
		this.parserStrategy = parserStrategy;
	}
}
