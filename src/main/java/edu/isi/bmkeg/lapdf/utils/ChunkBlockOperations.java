package edu.isi.bmkeg.lapdf.utils;

import com.denkbares.utils.Log;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTChunkBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTModelFactory;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.LineBasedChunkBlock;
import edu.isi.bmkeg.lapdf.parser.MaxPowerChunker;
import edu.isi.bmkeg.lapdf.parser.RuleBasedParser;

import java.util.*;

/**
 * @author Maximilian Schirm (denkbares GmbH)
 * @created 23.06.2016
 */

public class ChunkBlockOperations {

    /**
     * This method can be passed a collection of ChunkBlocks and will proceed to merge smaller ChunkBlocks
     * which are contained inside the perimeter of a bigger one into it.
     *
     * @param candidates
     * @return
     */
    public static Collection<ChunkBlock> mergeContainedChunkBlocks(Collection<ChunkBlock> candidates) {
        boolean newMerges = shouldBeMerged(candidates);
        ArrayList<ChunkBlock> returner = new ArrayList<>(candidates);

        //Repeat merging as long as there are still possible merges
        while (newMerges) {
            Map<ChunkBlock, ArrayList<ChunkBlock>> containsMap = new HashMap<>();

            for (ChunkBlock b1 : candidates) {
                for (ChunkBlock b2 : candidates) {
                    //We lookup if b1 is contained in any blocks
                    if (checkIsContainedIn(b1, b2) && !b1.equals(b2)) {

                        ArrayList<ChunkBlock> temp;
                        if (containsMap.containsKey(b2)) {
                            temp = containsMap.get(b2);
                        } else {
                            temp = new ArrayList<>();
                        }
                        temp.add(b1);
                        containsMap.put(b2, temp);
                    }
                }
            }

            //--Build the output list--
            returner = new ArrayList<>();
            ArrayList<ChunkBlock> checkedChunks = new ArrayList<>();

            //Conduct merges and add to candidates
            for (ChunkBlock parentChunk : containsMap.keySet()) {
                ArrayList<ChunkBlock> combinedParentAndChildren = containsMap.get(parentChunk);
                combinedParentAndChildren.add(parentChunk);
                ChunkBlock combinedContained = forceCombineMultipleChunkBlocks(combinedParentAndChildren);
                //To avoid checking them again
                candidates.removeAll(combinedParentAndChildren);
                checkedChunks.addAll(combinedParentAndChildren);

                returner.add(combinedContained);
            }

            //Include blocks from candidates that were not merged into any other blocks
            for (ChunkBlock block : candidates) {
                if (!checkedChunks.contains(block))
                    returner.add(block);
            }

            candidates = returner;
            newMerges = shouldBeMerged(candidates);
        }

        return returner;
    }


    /**
     * Returns true if there are definitely possible merges between the blocks of the input collection.
     *
     * @param candidates
     * @return
     */
    private static boolean shouldBeMerged(Collection<ChunkBlock> candidates) {
        for (ChunkBlock b1 : candidates) {
            for (ChunkBlock b2 : candidates) {
                if (!b1.equals(b2) && checkIsContainedIn(b1, b2))
                    return true;
            }
        }

        return false;
    }

    /**
     * Creates a ChunkBlock by merging blocks no matter their overlapping.
     *
     * @param blocks
     * @return
     */
    public static ChunkBlock forceCombineMultipleChunkBlocks(Collection<ChunkBlock> blocks) {
        boolean oldMode = false;
        if(oldMode)
            return forceCombineMultipleChunkBlocksOLDMETHOD(blocks);
        else
            return forceCombineMultipleChunkBlocksNEWMETHOD(blocks);
    }

    private static ChunkBlock forceCombineMultipleChunkBlocksNEWMETHOD(Collection<ChunkBlock> blocks) {
        ArrayList<WordBlock> allWordBlocks = new ArrayList<>();
        for(ChunkBlock chunkBlock : blocks){
            for(WordBlock wordBlock : chunkBlock.getWordBlocks()){
                if(!allWordBlocks.contains(wordBlock))
                    allWordBlocks.add(wordBlock);
            }
        }

        try {
            RuleBasedParser builder = new RuleBasedParser(new RTModelFactory());
            return builder.buildChunkBlock(allWordBlocks, allWordBlocks.get(0).getPage());
        } catch (Exception e) {
            Log.severe("Failed to merge Chunks by creating new Chunk from their WordBlocks. Returned null.", e);
        }
        return null;
    }

    private static ChunkBlock forceCombineMultipleChunkBlocksOLDMETHOD(Collection<ChunkBlock> blocks){
        //Push all input elements to a stack
        Stack<ChunkBlock> blockStack = new Stack<>();
        for (ChunkBlock b : blocks)
            blockStack.push(b);

        //First block onto which all others are merged
        ChunkBlock returnBlock = blockStack.pop();

        //Combine all chunks from the stack
        while (!blockStack.isEmpty()) {
            returnBlock = combineChunkBlocks(returnBlock, blockStack.pop());
        }
        return returnBlock;
    }

    /**
     * Combines two chunkBlocks, using either the LineBasedModel or the RTModel.
     *
     * @param b1
     * @param b2
     * @return
     */
    public static ChunkBlock combineChunkBlocks(ChunkBlock b1, ChunkBlock b2) {
        if (b1 instanceof LineBasedChunkBlock && b2 instanceof LineBasedChunkBlock) {
            //Use Line Based Model
            ArrayList<Line> linesb1 = ((LineBasedChunkBlock) b1).getLines();
            ArrayList<Line> linesb2 = ((LineBasedChunkBlock) b2).getLines();

            ArrayList<Line> merged = new ArrayList<>();
            merged.addAll(linesb1);
            merged.addAll(linesb2);

            return LineBasedChunkBlock.buildLineBasedChunkBlock(merged);
        } else {
            //Use RT Model
            Collection<WordBlock> wordsb1 = b1.getWordBlocks();
            Collection<WordBlock> wordsb2 = b2.getWordBlocks();

            List<WordBlock> merged = new ArrayList<>();
            merged.addAll(wordsb1);
            merged.addAll(wordsb2);

            PageBlock pb = merged.iterator().next().getPage();
            ChunkBlock returner = null;

            try {
                RuleBasedParser builder = new RuleBasedParser(new RTModelFactory());
                returner = builder.buildChunkBlock(merged, pb);
            } catch (Exception e) {
                Log.severe("Failed to combine the ChunkBlocks b1 :" + b1 + " and b2 :" + b2 + ", could not instantiate a new RuleBasedParser (and returned null)", e);
            }

            return returner;
        }
    }

    /**
     * Checks whether b1 lies within b2.
     *
     * @param b1
     * @param b2
     * @return
     */
    public static boolean checkIsContainedIn(ChunkBlock b1, ChunkBlock b2) {
        int b1X1 = b1.getX1();
        int b1X2 = b1.getX2();
        int b1Y1 = b1.getY1();
        int b1Y2 = b1.getY2();

        int b2X1 = b2.getX1();
        int b2X2 = b2.getX2();
        int b2Y1 = b2.getY1();
        int b2Y2 = b2.getY2();

		boolean xinBounds = (b1X1 >= b2X1 && b1X2 <= b2X2);
		boolean yinBounds = (b1Y1 >= b2Y1 && b1Y2 <= b2Y2);

        return xinBounds && yinBounds;
    }

    public static boolean overlapsXPercent(ChunkBlock b1, ChunkBlock b2, double x){
        if(overlapPercentage(b1,b2) >= x)
            return true;
        else
            return false;
    }

    private static double overlapPercentage(ChunkBlock b1, ChunkBlock b2){
        //b1 coordinates inside b2?
        boolean b1LeftEdgeInsideB2Perimeter = (b1.getX1() >= b2.getX1() && b1.getX1() < b2.getX2());
        boolean b1RightEdgeInsideB2Perimeter = (b1.getX2() > b2.getX1() && b1.getX2() <= b2.getX2());
        boolean b1TopEdgeInsideB2Perimeter = (b1.getY1() >= b2.getY1() && b1.getY1() < b2.getY2());
        boolean b1LowEdgeInsideB2Perimeter = (b1.getY2() > b2.getY1() && b1.getY2() <= b2.getY2());
        //b2 coordinates inside b1
        boolean b2LeftEdgeInsideB1Perimeter = (b2.getX1() >= b1.getX1() && b2.getX1() < b1.getX2());
        boolean b2RightEdgeInsideB1Perimeter = (b2.getX2() > b1.getX1() && b2.getX2() <= b1.getX2());
        boolean b2TopEdgeInsideB1Perimeter = (b2.getY1() >= b1.getY1() && b2.getY1() < b1.getY2());
        boolean b2LowEdgeInsideB1Perimeter = (b2.getY2() > b1.getY1() && b2.getY2() <= b1.getY2());

        //TODO COMPLETE
        return 0;
    }
}
