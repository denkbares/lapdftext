package edu.isi.bmkeg.lapdf.parser;

import edu.isi.bmkeg.lapdf.model.*;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.utils.PageOperations;
import edu.isi.bmkeg.utils.FrequencyCounter;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * @author Maximilian Schirm (denkbares GmbH), 9.3.2016
 *
 * This class offers methods to replace the method "buildChunkBlock"
 * of the "RuleBasedParser" class. It should offer better results than the
 * legacy method.
 */
public class MaxPowerChunkingClass {

    public static ArrayList<ChunkBlock> buildChunkBlocks(PageBlock page){

        ArrayList<WordBlock> blocksOfPage = (ArrayList<WordBlock>) page.getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);
        //Sort by coordinates (descending)
        blocksOfPage.sort(new Comparator<WordBlock>() {
            @Override
            public int compare(WordBlock o1, WordBlock o2) {
                if (o1.getY1() < o2.getY1())
                    return 1;
                else if (o1.getY1() == o2.getY1())
                    return 0;
                else
                    return -1;
            }
        });

        return buildChunkBlocks(blocksOfPage);
    }

    public static ArrayList<ChunkBlock> buildChunkBlocks(ArrayList<WordBlock> wordBlocks){
        ArrayList<ArrayList<Line>> chunkCandidates = createChunkCandidates(createLinesOfPage(wordBlocks));
        ArrayList<ChunkBlock> returner = new ArrayList<>();
        //Further process chunkCandidates by :
        // - in-line features (Gaps between WordBlocks, Font Style)
        // - over-line features (How homogeneous WordBlocks in a ChCan are, possible vertical separators)
        //Do not forget to properly create ChunkBlocks by setting features like in RuleBasedParser!

        for(ArrayList<Line> currentCandidate : chunkCandidates) {
            //TODO : Implement above "Further Processing" steps!
            //This is a proof-of-concept implementation, closely built like the original buildChunkBlocks() method.
            PageBlock page = null;
            IntegerFrequencyCounter lineHeightFrequencyCounter = new IntegerFrequencyCounter(1);
            IntegerFrequencyCounter spaceFrequencyCounter = new IntegerFrequencyCounter(0);
            FrequencyCounter fontFrequencyCounter = new FrequencyCounter();
            FrequencyCounter styleFrequencyCounter = new FrequencyCounter();

            for(Line l : currentCandidate){
                lineHeightFrequencyCounter.add(l.getHeight());

                for(WordBlock b : l.getWordBlocks()){
                    if( b.getFont() != null ) {
                        fontFrequencyCounter.add(b.getFont());
                    } else {
                        fontFrequencyCounter.add("");
                    }
                    if( b.getFont() != null ) {
                        styleFrequencyCounter.add(b.getFontStyle());
                    } else {
                        styleFrequencyCounter.add("");
                    }
                    page = b.getPage();
                }
            }

            ChunkBlock newBlock = new LineBasedChunkBlock(currentCandidate);

            newBlock.setMostPopularWordFont(
                    (String) fontFrequencyCounter.getMostPopular()
            );

            newBlock.setMostPopularWordStyle(
                    (String) styleFrequencyCounter.getMostPopular()
            );

            newBlock.setMostPopularWordHeight(
                    lineHeightFrequencyCounter.getMostPopular()
            );

            //NOTE: Completely redundant! SpaceWidths are never initialized!
            newBlock.setMostPopularWordSpaceWidth(
                    spaceFrequencyCounter.getMostPopular()
            );

            newBlock.setContainer(page);

            returner.add(newBlock);
        }
        return returner;
    }

    /**
     * Groups lines that are likely to become chunks
     * @param lines The lines which are to be grouped
     * @return Groups of likely Chunk - Line clusters
     */
    private static ArrayList<ArrayList<Line>> createChunkCandidates(ArrayList<Line> lines){
        ArrayList<ArrayList<Line>> potentialChunks=  new ArrayList<ArrayList<Line>>();
        double avgLineDist = PageOperations.getAverageVerticalDistanceOfLines(lines);

        for(Line line : lines){
            //Two checks :
            // 1. Does this line have a line above it, with the distance to that line being greater than avg?
            // 2. Does this line have a line below it, with the distance to that line being greater than avg?
            Line lineAbove = PageOperations.getLineInDirOf("UP", line, lines);
            Line lineBelow = PageOperations.getLineInDirOf("DOWN", line, lines);

            double distAbove, distBelow, originalDist = 0;
            Line topmostLine = lineAbove;
            boolean topmostLineFound = false;

            if(lineAbove != null){
                distAbove = line.distanceTo(lineAbove, "UP");
                originalDist = distAbove;
                if(distAbove < avgLineDist){
                    //We might have a table candidate here.
                    //Proceed checking upwards whether there are more lines with that exact distance.
                    while (lineAbove != null && distAbove == originalDist){
                        topmostLine = lineAbove;
                        lineAbove = PageOperations.getLineInDirOf("UP", lineAbove, lines);
                        distAbove = topmostLine.distanceTo(lineAbove, "UP");
                    }
                    //topmostLine is now the highest line of the potential table.
                    topmostLineFound = true;
                }
            }
            if(lineBelow != null && !topmostLineFound){
                distBelow = line.distanceTo(lineBelow, "DOWN");
                originalDist = distBelow;
                if(distBelow < avgLineDist){
                    //We might have a table candidate here.
                    //Since we had no line above this one, assume this is the topmost line in the table.
                    topmostLine = line;
                    topmostLineFound = true;
                }
            }

            //If we found the topmost line of our possible table, collect lines with our original distance downwards.
            if(topmostLineFound) {
                ArrayList<Line> tableCandidate = new ArrayList<>();
                tableCandidate.add(topmostLine);
                lineBelow = PageOperations.getLineInDirOf("DOWN", topmostLine, lines);
                distBelow = topmostLine.distanceTo(lineBelow, "DOWN");
                while(lineBelow != null && distBelow == originalDist){
                    tableCandidate.add(lineBelow);
                    Line oldLineBelow = lineBelow;
                    lineBelow = PageOperations.getLineInDirOf("DOWN", lineBelow, lines);
                    distBelow = oldLineBelow.distanceTo(lineBelow, "DOWN");
                }
                //tableCandidate now contains all our suspected table lines downwards. TODO Doesn't add next one that was originally referenced!
                //TODO (2) : Check whether above TODO statement still holds!-
                potentialChunks.add(tableCandidate);
            }
        }
        //Remove any duplicates
        ArrayList<ArrayList<Line>> duplicateFree = new ArrayList<>();
        for(ArrayList<Line> candidate : potentialChunks){
            if(!duplicateFree.contains(candidate)){
                duplicateFree.add(candidate);
            }
        }
        potentialChunks = new ArrayList<>(duplicateFree);
        return potentialChunks;
    }

    /**
     * This method generates Lines from the page. Lines are used for all the further processing steps.
     * @param wordBlocksOfPage The WordBlocks of which to create lines. Usually all Blocks of a page
     * @return The lines which have been created by this method.
     */
    private static ArrayList<Line> createLinesOfPage(ArrayList<WordBlock> wordBlocksOfPage) {
        ArrayList<WordBlock> mixedWords = new ArrayList<>(wordBlocksOfPage);
        final ArrayList<WordBlock> originalWords = new ArrayList<>(wordBlocksOfPage);
        ArrayList<Line> lines = new ArrayList<>();
        for(WordBlock w : originalWords){
            if(mixedWords.contains(w)){
                //Find leftmost WordBlock in line
                WordBlock lineStart = w;
                while(PageOperations.getWordBlockInDirOf("LEFT", lineStart, mixedWords) != null){
                    lineStart = PageOperations.getWordBlockInDirOf("LEFT", lineStart, mixedWords);
                }
                //lineStart is now the leftmost WordBlock in line
                //Go through all blocks to the right now and add them progressively to a new line
                //Remove blocks that are in a line from mixedWords, so that any WordBlock is always on just one line
                ArrayList<WordBlock> tempLineWordBlocks = new ArrayList<>();
                tempLineWordBlocks.add(lineStart);
                while(PageOperations.getWordBlockInDirOf("RIGHT", lineStart, mixedWords) != null){
                    lineStart = PageOperations.getWordBlockInDirOf("RIGHT", lineStart, mixedWords);
                    tempLineWordBlocks.add(lineStart);
                    mixedWords.remove(lineStart);
                }

                //Build a new line and save it
                lines.add(new Line(tempLineWordBlocks));
            }
        }
        return lines;
    }
}