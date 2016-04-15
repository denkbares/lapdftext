package edu.isi.bmkeg.lapdf.utils;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Gap;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;

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
        int x1 = maxOverlappingGap.getThisGap().getBeginning(),x2 = maxOverlappingGap.getThisGap().getEnd();
        for(Gap currGap : allGaps){
            if(currGap.doesOverlap(maxOverlappingGap.getThisGap()) && !maxOverlappingGap.equals(currGap)){
                //Get minimum shared coordinates of selected Gaps
                int currX1 = currGap.getBeginning();
                int currX2 = currGap.getEnd();

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
}
