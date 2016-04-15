package edu.isi.bmkeg.lapdf.parser;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.LineBasedChunkBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.utils.LineBasedOperations;
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
        ArrayList<ArrayList<Line>> chunkCandidates = createChunkCandidates(PageOperations.createLinesOfPage(wordBlocks));
        ArrayList<ChunkBlock> returner = new ArrayList<>();
        ArrayList<ArrayList<Line>> splitResults = new ArrayList<>();
        //Further process chunkCandidates by :
        // - in-line features (Font Style)
        // - over-line features (How homogeneous WordBlocks in a ChCan are, possible vertical separators)

        for(ArrayList<Line> currentCandidate : chunkCandidates) {
            ArrayList<Line> leftHalf = new ArrayList<>(currentCandidate);

            //Look for horizontal separation and split if possible
            double separation = LineBasedOperations.getHorizontalSeparation(currentCandidate);

            //Play with this value to increase splitting sensitivity
            if(separation > 0.9){
                //TODO set table probability prior to splitting!
                int splitCoord = LineBasedOperations.getSplitCoord(currentCandidate);
                ArrayList<Line> rightHalf = new ArrayList<>();
                for(Line line : currentCandidate){
                    line.setTableProbability(separation);
                    rightHalf.add(line.split(splitCoord)[1]);
                    leftHalf.set(currentCandidate.indexOf(line), line.split(splitCoord)[0]);
                }
            }
            //Finally, add block to returner
            returner.add(createChunkFromLines(leftHalf));
        }

        //Process split halves
        for(ArrayList<Line> currentCandidate : splitResults){
            //Add block to returner
            ChunkBlock newBlock =createChunkFromLines(currentCandidate);
            //Might be improved by using mean table prob, but at the moment get(0).get.. suffices.
            newBlock.setTableProbability(currentCandidate.get(0).getTableProbability());
            returner.add(newBlock);
        }

        return returner;
    }

    /**
     * Creates a ChunkBlock from the list of lines.
     * @param lines A list of lines.
     * @return A new ChunkBlock
     */
    private static ChunkBlock createChunkFromLines(ArrayList<Line> lines){
        // vv Create Block vv
        //This is a proof-of-concept implementation, closely built like the original buildChunkBlocks() method.
        PageBlock page = null;
        IntegerFrequencyCounter lineHeightFrequencyCounter = new IntegerFrequencyCounter(1);
        IntegerFrequencyCounter spaceFrequencyCounter = new IntegerFrequencyCounter(0);
        FrequencyCounter fontFrequencyCounter = new FrequencyCounter();
        FrequencyCounter styleFrequencyCounter = new FrequencyCounter();



        for(Line l : lines){
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

        ChunkBlock newBlock = new LineBasedChunkBlock(lines);

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

        newBlock.setContainer(page);

        return newBlock;
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
                //TODO : Change avgLineDist formula  to improve coverage if necessary
                if(distAbove < avgLineDist/2){
                    //We might have a chunk candidate here.
                    //Proceed checking upwards whether there are more lines with that exact distance.
                    while (lineAbove != null && distAbove == originalDist){
                        topmostLine = lineAbove;
                        lineAbove = PageOperations.getLineInDirOf("UP", lineAbove, lines);
                        distAbove = topmostLine.distanceTo(lineAbove, "UP");
                    }
                    //topmostLine is now the highest line of the potential chunk.
                    topmostLineFound = true;
                }
            }
            if(lineBelow != null && !topmostLineFound){
                distBelow = line.distanceTo(lineBelow, "DOWN");
                originalDist = distBelow;
                if(distBelow < avgLineDist){
                    //We might have a chunk candidate here.
                    //Since we had no line above this one, assume this is the topmost line in the chunk.
                    topmostLine = line;
                    topmostLineFound = true;
                }
            }

            //If we have found the topmost line of our chunk, collect lines with our original distance downwards.
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
                //chunkCandidate now contains all our suspected table lines downwards. TODO Doesn't add next one that was originally referenced?
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
        return duplicateFree;
    }
}