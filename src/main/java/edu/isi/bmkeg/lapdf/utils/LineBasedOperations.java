package edu.isi.bmkeg.lapdf.utils;

import edu.isi.bmkeg.lapdf.model.Block;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Gap;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.LineBasedChunkBlock;

import java.util.ArrayList;

/**
 * This class could be optimized by merging Functionalities of getHorizontalSeperation and getSplitCoord.
 *
 * Created by Maximilian Schirm (denkbares GmbH), 13.04.2016
 */
public class LineBasedOperations {

    private static class MetaGap{
        private int ctr = 0;
        private Gap thisGap;

        private MetaGap(Gap thisGap){
            this.thisGap = thisGap;
        }

        protected Gap getThisGap(){
            return thisGap;
        }

        protected int getCtr(){
            return ctr;
        }

        protected void incrementCtr(){
            ctr++;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if(o.getClass() == Gap.class)
                return thisGap.equals(o);
            if (o == null || getClass() != o.getClass()) return false;

            MetaGap metaGap = (MetaGap) o;

            if (ctr != metaGap.ctr) return false;
            return thisGap.equals(metaGap.thisGap);

        }

        @Override
        public int hashCode() {
            int result = ctr;
            result = 31 * result + thisGap.hashCode();
            return result;
        }
    }

    /**
     * Returns the likelihood that the passed Chunk candidate can be further separated
     * Might be improved to simultaneously return the position of the split
     */
    public static double getHorizontalSeparation(ArrayList<Line> chunk){
        double sep = 0.0;
        int countLines = chunk.size();
        int currentCountOfOverlap = 0;
        ArrayList<Integer> overlapCounters = new ArrayList<>();

        for(Line currLine : chunk){
            for(Gap currGap : currLine.getGaps()) {
                currentCountOfOverlap = 0;
                for(Line compareLine : chunk){
                    if(!currLine.equals(compareLine)){
                        for(Gap compareGap : compareLine.getGaps()){
                            //Now let's see whether they overlap!
                            if(currGap.doesOverlap(compareGap)){
                                currentCountOfOverlap++;
                            }
                        }
                    }
                }
                overlapCounters.add(new Integer(currentCountOfOverlap));
            }
        }

        //Analyze

        int overlappingGaps = 0;
        double meanOverlappingLines = 0;
        int maxOverlappingLines = 0;

        for(Integer count : overlapCounters){
            int x = count.intValue();
            //Our counting method counts the same overlapping axis for every line on which it overlaps
            //This formula resolves the issue and gives us the actual count of overlappers
            double linesOverlappingForThisGap = 0.5*(Math.sqrt(4*x+1)+1);
            overlappingGaps++;

            if(linesOverlappingForThisGap > maxOverlappingLines)
                maxOverlappingLines = (int)linesOverlappingForThisGap;

            meanOverlappingLines += linesOverlappingForThisGap;
        }
        //The higher meanOverlappingLines is, the more likely that this Chunk is a Table
        meanOverlappingLines = meanOverlappingLines/overlappingGaps;

        sep = maxOverlappingLines / countLines;
        return  sep;
    }

    /**
     * Returns the coordinate on which the best split would occur
     * TODO Might be integrated with getHorizontalSeperation for performance gains
     * @param currentCandidate
     * @return
     */
    public static int getSplitCoord(ArrayList<Line> currentCandidate) {
        ArrayList<MetaGap> overlapCounters = new ArrayList<>();
        ArrayList<Gap> allGaps = new ArrayList<>();

        for(Line currLine : currentCandidate){
            for(Gap currGap : currLine.getGaps()) {
                allGaps.add(currGap);


                MetaGap currentMeta = new MetaGap(currGap);

                for(Line compareLine : currentCandidate){
                    if(!currLine.equals(compareLine)){

                        for(Gap compareGap : compareLine.getGaps()){
                            //Now let's see whether they overlap!
                            if(currGap.doesOverlap(compareGap)){
                                currentMeta.incrementCtr();
                            }
                        }

                    }
                }
                overlapCounters.add(currentMeta);
            }
        }

        //Analyze

        MetaGap maxOverlappingGap = null;
        int maxOverlappingLines = 0;

        for(MetaGap currentMeta : overlapCounters){
            if(currentMeta.getCtr() > maxOverlappingLines) {
                maxOverlappingLines = currentMeta.getCtr();
                maxOverlappingGap = currentMeta;
            }
        }

        //Find the gaps overlapping our maxOverlappingGap
        int x1 = maxOverlappingGap.getThisGap().getGlobalBeginning(),x2 = maxOverlappingGap.getThisGap().getGlobalEnd();
        for(Gap currGap : allGaps){
            if(currGap.doesOverlap(maxOverlappingGap.getThisGap()) && !maxOverlappingGap.equals(currGap)){
                //Get minimum shared coordinates of selected Gaps
                int currX1 = currGap.getGlobalBeginning();
                int currX2 = currGap.getGlobalEnd();

                if(x1 < currX1)
                    x1 = currX1;
                if(x2 > currX2)
                    x2 = currX2;
            }
        }

        return (x2-x1)/2;
    }

    /**
     * Returns a value (0.0 - 1.0) representing the percentage of overlapping between the ChunkBlock and the line
     * @param line
     * @param block
     * @return
     */
    public static double getOverlap(Line line, ChunkBlock block) {
        ArrayList<WordBlock> lineblocks = line.getWordBlocks();
        double overlap = 0;
        for(WordBlock w : block.getWordBlocks()) {
            if(lineblocks.contains(w))
                overlap += 1/block.getWordBlocks().size();
        }
        return overlap;
    }

    /**
     * This method builds a temporary LineBasedChunkBlock to pass to the verticalSplitCandidate(..) method.
     * It is more suitable to be used during ChunkBlock construction than it's counterpart.
     *
     *
     * The evaluation part of this method was taken from the RuleBasedParser.verticalSplitCandidate() method.
     * It was modified to be more performant using the features of the LineBasedModel.
     *
     * @param lineBlock The the LineBasedChunkBlock we want to analyze for splitting.
     * @param parent The PageBlock on which the lineBlock resides
     * @return Whether the lineBlock should be split in half.
     */
    public static boolean verticalSplitCandidate(ArrayList<Line> lineBlock, PageBlock parent){
        LineBasedChunkBlock temp = new LineBasedChunkBlock(lineBlock);
        temp.setPage(parent);
        return verticalSplitCandidate(temp);
    }

    /**
     * This method determines whether it is viable to split a LineBasedChunkBlock in half.
     *
     * The evaluation part of this method was taken from the RuleBasedParser.verticalSplitCandidate() method.
     * It was modified to be more performant using the features of the LineBasedModel.
     *
     * @param lineBlock The the LineBasedChunkBlock we want to analyze for splitting.
     * @return Whether the lineBlock should be split in half.
     */
    public static boolean verticalSplitCandidate(LineBasedChunkBlock lineBlock){
        ArrayList<Integer> spaceList = new ArrayList();
        PageBlock parent = lineBlock.getPage();

        //Find widest Gaps of each line and add their width to the spaceList
        for(Line currentLine : lineBlock.getLines()){
            int currentWidest = 0;
            for(Gap g : currentLine.getGaps()){
                if(g.getWidth() > currentWidest)
                    currentWidest = g.getWidth();
            }
            spaceList.add(new Integer(currentWidest));
        }

        //Below copied from RuleBasedParser:

        // Criterium for whether the widest spaces are properly lined up:
        // At least 20% of them have an x position within that differ with less
        // than 1% to the x position of the previous space.
        // The average x position doesn't matter!
        if (spaceList.size() == 0)
            return false;

        // Find average width of the widest spaces and make sure it's at least
        // as wide as 2.5% of the page width.
        double averageWidth = 0;
        for (int i = 0; i < spaceList.size(); i++)
            averageWidth += spaceList.get(i).intValue();
        averageWidth = averageWidth / spaceList.size();
        // spaceWidthToPageWidth = (float) averageWidth / (float) pageWidth;

        if (averageWidth > parent.getMostPopularHorizontalSpaceBetweenWordsPage())
            return true;
        else
            return false;
    }

    /**
     * Splits a LineBasedChunkBlock down the middle and removes the old one from the page.
     *
     * @param block
     * @param idGenerator
     */
    public static void splitLineBlockDownTheMiddle(LineBasedChunkBlock block, int idGenerator){
        PageBlock parent = block.getPage();
        int median = parent.getMedian();

        ArrayList<Line> leftLines = new ArrayList<>();
        ArrayList<Line> rightLines = new ArrayList<>();

        for(Line currentLine : block.getLines()){
            ArrayList<WordBlock> leftBlocks = new ArrayList<>();
            ArrayList<WordBlock> rightBlocks = new ArrayList<>();
            ArrayList<WordBlock> currentWords = currentLine.getWordBlocks();

            for(WordBlock currentWordBlock : currentWords){
                String wordBlockLeftRightMidLine = currentWordBlock.readLeftRightMidLine();

                if (wordBlockLeftRightMidLine.equals(Block.LEFT))
                    leftBlocks.add(currentWordBlock);
                else if (wordBlockLeftRightMidLine.equals(Block.RIGHT))
                    rightBlocks.add(currentWordBlock);
                else if (wordBlockLeftRightMidLine.equals(Block.MIDLINE)) {

                    // Assign the current word to the left or right side depending upon
                    // whether most of the word is on the left or right side of the median.
                    if (Math.abs(median - currentWordBlock.getX1()) > Math.abs(currentWordBlock.getX2() - median)) {
                        currentWordBlock.resize(currentWordBlock.getX1(), currentWordBlock.getY1(), median - currentWordBlock.getX1(), currentWordBlock.getHeight());
                        leftBlocks.add(currentWordBlock);
                    } else {
                        currentWordBlock.resize(median, currentWordBlock.getY1(),currentWordBlock.getX2() - median, currentWordBlock.getHeight());
                        rightBlocks.add(currentWordBlock);
                    }
                }
            }

            if(leftBlocks.size() == 0 || rightBlocks.size() == 0){
                //NOTE : If we have one line in which we cannot divide the WordBlocks on two sides we abort splitting
                // Might lead to problems, just test it.
                return;
            }

            //Create new Lines leftPart and rightPart and add them to the respective lists
            Line leftPart = new Line(leftBlocks);
            Line rightPart = new Line(rightBlocks);

            leftLines.add(leftPart);
            rightLines.add(rightPart);

        }

        LineBasedChunkBlock leftBlock = new LineBasedChunkBlock(leftLines);
        LineBasedChunkBlock rightBlock = new LineBasedChunkBlock(rightLines);

        double relativeOverlap = leftBlock.getRelativeOverlap(rightBlock);

        if(relativeOverlap < 0.1){
            parent.delete(block, block.getId());
            parent.add(leftBlock, idGenerator++);
            parent.add(rightBlock, idGenerator++);
        }
    }

    /**
     * Splits a bunch of lines down the middle (median) and returns two Lists with both halves.
     * Index 0 is left, index 1 is right half.
     *
     * @param linesToSplit The lines we want to get cut up
     * @return An array with two ArrayList<Line>. Index 0 is left, 1 is right half.
     */
    public static ArrayList<Line>[] splitLineBlockCandidateDownTheMiddle(ArrayList<Line> linesToSplit){
        int median;

        //Set Median.
        median = linesToSplit.get(0).getWordBlocks().get(0).getPage().getMedian();

        ArrayList<Line> leftLines = new ArrayList<>();
        ArrayList<Line> rightLines = new ArrayList<>();

        for(Line currentLine : linesToSplit){
            ArrayList<WordBlock> leftBlocks = new ArrayList<>();
            ArrayList<WordBlock> rightBlocks = new ArrayList<>();
            ArrayList<WordBlock> currentWords = currentLine.getWordBlocks();

            for(WordBlock currentWordBlock : currentWords){
                String wordBlockLeftRightMidLine = currentWordBlock.readLeftRightMidLine();

                if (wordBlockLeftRightMidLine.equals(Block.LEFT))
                    leftBlocks.add(currentWordBlock);
                else if (wordBlockLeftRightMidLine.equals(Block.RIGHT))
                    rightBlocks.add(currentWordBlock);
                else if (wordBlockLeftRightMidLine.equals(Block.MIDLINE)) {

                    // Assign the current word to the left or right side depending upon
                    // whether most of the word is on the left or right side of the median.
                    if (Math.abs(median - currentWordBlock.getX1()) > Math.abs(currentWordBlock.getX2() - median)) {
                        currentWordBlock.resize(currentWordBlock.getX1(), currentWordBlock.getY1(), median - currentWordBlock.getX1(), currentWordBlock.getHeight());
                        leftBlocks.add(currentWordBlock);
                    } else {
                        currentWordBlock.resize(median, currentWordBlock.getY1(),currentWordBlock.getX2() - median, currentWordBlock.getHeight());
                        rightBlocks.add(currentWordBlock);
                    }
                }
            }

            if(leftBlocks.size() == 0 || rightBlocks.size() == 0){
                //NOTE : If we have one line in which we cannot divide the WordBlocks on two sides we just move all Words to the Left Line
                leftBlocks = currentWords;
            }

            //Create new Lines leftPart and rightPart and add them to the respective lists
            Line leftPart = new Line(leftBlocks);
            Line rightPart = new Line(rightBlocks);

            leftLines.add(leftPart);
            rightLines.add(rightPart);

        }
        ArrayList<Line>[] returner = new ArrayList[2];
        returner[0] = leftLines;
        returner[1] = rightLines;

        return returner;
    }
}