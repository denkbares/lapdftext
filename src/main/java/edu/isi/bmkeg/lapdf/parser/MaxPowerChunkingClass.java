package edu.isi.bmkeg.lapdf.parser;

import edu.isi.bmkeg.lapdf.extraction.Extractor;
import edu.isi.bmkeg.lapdf.extraction.JPedalExtractor;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTModelFactory;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.LineBasedChunkBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;
import edu.isi.bmkeg.lapdf.utils.ChunkBlockOperations;
import edu.isi.bmkeg.lapdf.utils.LineBasedOperations;
import edu.isi.bmkeg.lapdf.utils.PageOperations;
import edu.isi.bmkeg.lapdf.xml.model.*;
import edu.isi.bmkeg.utils.FrequencyCounter;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;
import edu.isi.bmkeg.utils.xml.XmlBindingTools;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Maximilian Schirm (denkbares GmbH), 9.3.2016
 *         <p>
 *         This class offers methods to replace the method "buildChunkBlock"
 *         of the "RuleBasedParser" class. It should offer better results than the
 *         legacy method.
 */
public class MaxPowerChunkingClass implements Parser {

    protected String path;

    private static int idGenerator = 0;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * This method takes the Page's WordBlocks, sorts them by their coordinates from top to bottom and passes them to
     * the build ChunkBlocks(ArrayList<WordBlock>) method for further operations.
     *
     * @param page The page we want to chunk
     * @return A list of ChunkBlocks, created by the complementary method.
     */
    public static ArrayList<ChunkBlock> buildChunkBlocks(PageBlock page) {
        ArrayList<WordBlock> blocksOfPage = (ArrayList<WordBlock>) page.getAllWordBlocks(SpatialOrdering.MIXED_MODE);
        //Sort by coordinates (descending)
        blocksOfPage.sort(new Comparator<WordBlock>() {
            @Override
            public int compare(WordBlock o1, WordBlock o2) {
                if (o1.getY1() < o2.getY1())
                    return -1;
                else if (o1.getY1() == o2.getY1())
                    return 0;
                else
                    return 1;
            }
        });

        return buildChunkBlocks(blocksOfPage, page);
    }

    /**
     * This method does most of the processing in the ChunkBlock generation process.
     *
     * @param wordBlocks The List of WordBlocks from which we will create ChunkBlocks
     * @return A list of ChunkBlocks.
     */
    public static ArrayList<ChunkBlock> buildChunkBlocks(ArrayList<WordBlock> wordBlocks, PageBlock parent) {
        ArrayList<ArrayList<Line>> chunkCandidates = createChunkCandidates(PageOperations.createLinesOfPage(wordBlocks));
        ArrayList<ChunkBlock> returner = new ArrayList<>();
        ArrayList<ArrayList<Line>> splitResults = new ArrayList<>();
        //Further process chunkCandidates by :
        // - in-line features (Font Style)
        // - over-line features (How homogeneous WordBlocks in a ChCan are, possible vertical separators)

        for (ArrayList<Line> currentCandidate : chunkCandidates) {
            if (LineBasedOperations.verticalSplitCandidate(currentCandidate, parent)) {
                ArrayList<Line>[] result = LineBasedOperations.splitLineBlockCandidateDownTheMiddle(currentCandidate);
                ChunkBlock leftHalf = createChunkFromLines(result[0]);
                ChunkBlock rightHalf = createChunkFromLines(result[1]);
                returner.add(leftHalf);
                returner.add(rightHalf);
            } else {
                returner.add(createChunkFromLines(currentCandidate));
            }
        }

        //Experimental post processing TODO Debug and Test
        return new ArrayList<>(ChunkBlockOperations.mergeContainedChunkBlocks(returner));
    }

    /**
     * Creates a ChunkBlock from the list of lines.
     *
     * @param lines A list of lines.
     * @return A new ChunkBlock
     */
    private static ChunkBlock createChunkFromLines(ArrayList<Line> lines) {
        // vv Create Block vv
        //This is a proof-of-concept implementation, closely built like the original buildChunkBlocks() method.
        PageBlock page = null;
        IntegerFrequencyCounter lineHeightFrequencyCounter = new IntegerFrequencyCounter(1);
        IntegerFrequencyCounter spaceFrequencyCounter = new IntegerFrequencyCounter(0);
        FrequencyCounter fontFrequencyCounter = new FrequencyCounter();
        FrequencyCounter styleFrequencyCounter = new FrequencyCounter();


        for (Line l : lines) {
            lineHeightFrequencyCounter.add(l.getHeight());

            for (WordBlock b : l.getWordBlocks()) {
                if (b.getFont() != null) {
                    fontFrequencyCounter.add(b.getFont());
                } else {
                    fontFrequencyCounter.add("");
                }
                if (b.getFont() != null) {
                    styleFrequencyCounter.add(b.getFontStyle());
                } else {
                    styleFrequencyCounter.add("");
                }
                page = b.getPage();
            }
        }

        ChunkBlock newBlock = LineBasedChunkBlock.buildLineBasedChunkBlock(lines);

        newBlock.setMostPopularWordFont(
                (String) fontFrequencyCounter.getMostPopular()
        );

        newBlock.setMostPopularWordStyle(
                (String) styleFrequencyCounter.getMostPopular()
        );

        newBlock.setMostPopularWordHeight(
                lineHeightFrequencyCounter.getMostPopular()
        );

        //NOTE: SpaceWidths are never initialized!
        newBlock.setMostPopularWordSpaceWidth(
                spaceFrequencyCounter.getMostPopular()
        );

        //Set containers
        for (WordBlock b : newBlock.getWordBlocks())
            b.setContainer(newBlock);

        newBlock.setContainer(page);
        newBlock.setPage(page);

        return newBlock;
    }

    /**
     * Groups lines that are likely to become chunks
     *
     * @param lines The lines which are to be grouped
     * @return Groups of likely Chunk - Line clusters
     */
    private static ArrayList<ArrayList<Line>> createChunkCandidates(ArrayList<Line> lines) {

        ArrayList<ArrayList<Line>> potentialChunks = new ArrayList<ArrayList<Line>>();
        double avgLineDist = PageOperations.getAverageVerticalDistanceOfLines(lines);

        for (Line line : lines) {
            //Two checks :
            // 1. Does this line have a line above it, with the distance to that line being greater than avg?
            // 2. Does this line have a line below it, with the distance to that line being greater than avg?
            Line lineAbove = PageOperations.getLineInDirOf("UP", line, lines);
            Line lineBelow = PageOperations.getLineInDirOf("DOWN", line, lines);

            double distAbove, distBelow, originalDist = 0;
            Line topmostLine = lineAbove;
            boolean topmostLineFound = false;

            if (lineAbove != null) {
                distAbove = line.distanceTo(lineAbove, "UP");
                originalDist = distAbove;
                //TODO : Change avgLineDist formula  to improve coverage if necessary
                if (distAbove < avgLineDist / 2) {
                    //We might have a chunk candidate here.
                    //Proceed checking upwards whether there are more lines with that exact distance.
                    while (lineAbove != null && distAbove == originalDist) {
                        topmostLine = lineAbove;
                        lineAbove = PageOperations.getLineInDirOf("UP", lineAbove, lines);
                        distAbove = topmostLine.distanceTo(lineAbove, "UP");
                    }
                    //topmostLine is now the highest line of the potential chunk.
                    topmostLineFound = true;
                }
            }
            if (lineBelow != null && !topmostLineFound) {
                distBelow = line.distanceTo(lineBelow, "DOWN");
                originalDist = distBelow;
                if (distBelow < avgLineDist) {
                    //We might have a chunk candidate here.
                    //Since we had no line above this one, assume this is the topmost line in the chunk.
                    topmostLine = line;
                    topmostLineFound = true;
                }
            }

            //If we have found the topmost line of our chunk, collect lines with our original distance downwards.
            if (topmostLineFound) {
                ArrayList<Line> tableCandidate = new ArrayList<>();
                tableCandidate.add(topmostLine);
                lineBelow = PageOperations.getLineInDirOf("DOWN", topmostLine, lines);
                distBelow = topmostLine.distanceTo(lineBelow, "DOWN");
                while (lineBelow != null && distBelow == originalDist) {
                    tableCandidate.add(lineBelow);
                    Line oldLineBelow = lineBelow;
                    lineBelow = PageOperations.getLineInDirOf("DOWN", lineBelow, lines);
                    distBelow = oldLineBelow.distanceTo(lineBelow, "DOWN");
                }
                //chunkCandidate now contains all our suspected table lines downwards. TODO Does it add the next one that was originally referenced?
                potentialChunks.add(tableCandidate);
            }
        }
        //Remove any duplicates
        ArrayList<ArrayList<Line>> duplicateFree = new ArrayList<>();
        for (ArrayList<Line> candidate : potentialChunks) {
            if (!duplicateFree.contains(candidate)) {
                duplicateFree.add(candidate);
            }
        }
        return duplicateFree;
    }

    /**
     * The Parse method as specified by the Interface.
     *
     * @param file
     * @return
     * @throws Exception
     */
    public LapdfDocument parse(File file) throws Exception {

        if (file.getName().endsWith(".pdf")) {
            return this.parsePdf(file);
        } else if (file.getName().endsWith("_lapdf.xml")) {
            return this.parseXml(file);
        } else {
            throw new Exception("File type of " + file.getName() + " not *.pdf or *_lapdf.xml");
        }

    }

    /**
     * Creates a new LapdfDocument based on the given PDF File. The Chunks are created using the LineBasedModel. (Default for MaxPCC)
     *
     * @param file Input PDF file to be processed
     * @return The LapdfDocument for the file.
     * @throws Exception
     */
    public LapdfDocument parsePdf(File file) throws Exception {

        //Create a new Extractor
        AbstractModelFactory modelFactory = new RTModelFactory();
        Extractor pageExtractor = new JPedalExtractor(modelFactory);
        pageExtractor.init(file);

        //For our PageBlocks and WordBlocks
        ArrayList<PageBlock> pageList = new ArrayList<>();
        List<WordBlock> pageWordBlockList = null;
        PageBlock pageBlock = null;

        //Initialize the Frequency Counters
        IntegerFrequencyCounter avgHeightFrequencyCounter = new IntegerFrequencyCounter(1);
        FrequencyCounter fontFrequencyCounter = new FrequencyCounter();

        //Create Integers for processing
        int pageCounter = 1;
        idGenerator = 1;

        //Initialize the LapdfDocument
        LapdfDocument document = null;
        document = new LapdfDocument(file);
        document.setjPedalDecodeFailed(true);

        //Initialize the directory
        String pth = file.getPath();
        pth = pth.substring(0, pth.lastIndexOf(".pdf"));
        File imgDir = new File(pth);


        //
        // Calling 'hasNext()' get the text from the extractor.
        //
        while (pageExtractor.hasNext()) {

            document.setjPedalDecodeFailed(false);

            pageBlock = modelFactory.createPageBlock(
                    pageCounter++,
                    pageExtractor.getCurrentPageBoxWidth(),
                    pageExtractor.getCurrentPageBoxHeight(),
                    document);

            pageWordBlockList = pageExtractor.next();

            idGenerator = pageBlock.initialize(pageWordBlockList, idGenerator);

            if (!pageWordBlockList.isEmpty()) {

                idGenerator = pageBlock.addAll(
                        new ArrayList<SpatialEntity>(buildChunkBlocks(pageBlock)),
                        idGenerator
                );

                //Update Average Font Size and Frequency counters
                for (WordBlock word : pageBlock.getAllWordBlocks(SpatialOrdering.MIXED_MODE)) {
                    pageBlock.getDocument().getAvgHeightFrequencyCounter().add(
                            word.getHeight());
                    pageBlock.getDocument().getFontFrequencyCounter().add(
                            word.getFont() + ";" + word.getFontStyle());
                }

                /*
                //TODO Retrieve Method from RuleBasedParser OR check whether it can't be improved upon.
                mergeHighlyOverlappedChunkBlocks(pageBlock);
                **/

            }

            pageList.add(pageBlock);
        }

        if (!document.hasjPedalDecodeFailed()) {

            // initial parse is commplete.
            String s = file.getName().replaceAll("\\.pdf", "");
            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(file.getName());
            if (m.find()) {
                s = m.group(1);
            }

            document.addPages(pageList);

            document.calculateBodyTextFrame();
            document.calculateMostPopularFontStyles();

        }

        return document;
    }

    /**
     * Reads a lapdf XML file and decodes it on WordBlock level, then builds LineBasedChunkBlocks and adds them to the page.
     *
     * @param file The file to be decoded
     * @return A LapdfDocument
     * @throws Exception
     */
    public LapdfDocument parseXml(File file) throws Exception {
        FileReader reader = new FileReader(file);
        AbstractModelFactory modelFactory = new RTModelFactory();

        LapdftextXMLDocument xmlDoc = XmlBindingTools.parseXML(reader, LapdftextXMLDocument.class);

        List<WordBlock> pageWordBlockList = null;
        int pageCounter = 1;
        int id = 0;
        List<PageBlock> pageList = new ArrayList<PageBlock>();

        LapdfDocument document = new LapdfDocument();

        Map<Integer, String> fsMap = new HashMap<Integer, String>();
        for (LapdftextXMLFontStyle xmlFs : xmlDoc.getFontStyles()) {
            fsMap.put(xmlFs.getId(), xmlFs.getFontStyle());
        }

        for (LapdftextXMLPage xmlPage : xmlDoc.getPages()) {

            PageBlock pageBlock = modelFactory.createPageBlock(pageCounter, xmlPage.getW(), xmlPage.getH(), document);
            pageList.add(pageBlock);

            ArrayList<WordBlock> pageWords = new ArrayList<>();
            List<ChunkBlock> chunkBlockList = new ArrayList<ChunkBlock>();

            for (LapdftextXMLChunk xmlChunk : xmlPage.getChunks()) {
                String font = xmlChunk.getFont();
                List<WordBlock> chunkWords = new ArrayList<WordBlock>();
                pageWords = new ArrayList<>();

                for (LapdftextXMLWord xmlWord : xmlChunk.getWords()) {
                    int x1 = xmlWord.getX();
                    int y1 = xmlWord.getY();
                    int x2 = xmlWord.getX() + xmlWord.getW();
                    int y2 = xmlWord.getY() + xmlWord.getH();

                    WordBlock wordBlock = modelFactory.createWordBlock(x1, y1, x2, y2, 1, font, "", xmlWord.getT(), xmlWord.getI());

                    pageBlock.add(wordBlock, xmlWord.getId());
                    wordBlock.setPage(pageBlock);

                    String f = fsMap.get(xmlWord.getfId());
                    wordBlock.setFont(f);

                    String s = fsMap.get(xmlWord.getsId());
                    wordBlock.setFontStyle(s);

                    // add this word's height and font to the counts.
                    document.getAvgHeightFrequencyCounter().add(xmlWord.getH());
                    document.getFontFrequencyCounter().add(f + ";" + s);

                    pageWords.add(wordBlock);
                }
            }

            //LineBasedModel : Create Lines and ChunkBlocks
            List<SpatialEntity> pageChunks = new ArrayList<>();
            for (ChunkBlock b : buildChunkBlocks(pageBlock))
                pageChunks.add((LineBasedChunkBlock) b);

            pageBlock.addAll(pageChunks, idGenerator);
            pageCounter++;
        }

        document.addPages(pageList);
        document.calculateBodyTextFrame();
        document.calculateMostPopularFontStyles();

        return document;
    }
}