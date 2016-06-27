package edu.isi.bmkeg.lapdf.utils;

import de.d3web.utils.Log;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.RTree.RTModelFactory;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.LineBasedChunkBlock;
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
                    if (checkIsContainedIn(b1, b2)) {

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
                ChunkBlock combinedContained = forceCombineMultipleChunkBlocks(containsMap.get(parentChunk));

                //to check whether we got all blocks after building
                for (ChunkBlock checker : containsMap.get(parentChunk)) {
                    if (!checkedChunks.contains(checker))
                        checkedChunks.add(checker);
                }
                if (!checkedChunks.contains(parentChunk))
                    checkedChunks.add(parentChunk);

                ChunkBlock parentAndChildrenMerged = combineChunkBlocks(parentChunk, combinedContained);
                returner.add(parentAndChildrenMerged);
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
     * Returns true if there are possible merges between the blocks of the input collection.
     *
     * @param candidates
     * @return
     */
    private static boolean shouldBeMerged(Collection<ChunkBlock> candidates) {
        for (ChunkBlock b1 : candidates) {
            for (ChunkBlock b2 : candidates) {
                if (checkIsContainedIn(b1, b2))
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

        boolean xinBounds = (b1X1 >= b2X1 && b1X2 <= b2X2) ? true : false;
        boolean yinBounds = (b1Y1 >= b2Y1 && b1Y2 <= b2Y2) ? true : false;

        return xinBounds && yinBounds;
    }

    //TODO : COMPLETE
    public static boolean overlapsXPercent(ChunkBlock b1, ChunkBlock b2, double x){
        return true;
    }
}
