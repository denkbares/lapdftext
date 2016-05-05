package edu.isi.bmkeg.lapdf.model.lineBasedModel;

import edu.isi.bmkeg.lapdf.model.WordBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Maximilian Schirm (denkbares Gmbh), 9.3.2016
 * Class for easier management of collisions and other line specific operations.
 */
public class Line{
    int ytop, ybottom, xleft, xright;
    private ArrayList<WordBlock> wordBlocks;
    private ArrayList<Gap> gaps = new ArrayList<>();

    public static final String UP="UP", DOWN="DOWN", LEFT="LEFT", RIGHT="RIGHT";
    private double tableProbability = 0.0;

    public Line(ArrayList<WordBlock> words){
        //Find borders
        int maxY = -1, minY = -1, minX = -1, maxX = -1;
        for(WordBlock w : words){
            if(maxY == -1 || maxY < w.getY2())
                maxY = w.getY2();
            if(minY == -1 || minY > w.getY1())
                minY = w.getY1();
            if(maxX == -1 || maxX < w.getX2())
                maxX = w.getX2();
            if(minX == -1 || minX > w.getX1())
                minX = w.getX1();
        }
        //Assign values
        ytop = minY;
        ybottom = maxY;
        xleft = minX;
        xright = maxX;
        setWordBlocks(words);
        createGaps();
    }

    /**
     * This method is called on init or on changes to the WordBlocks to refresh the gaps in the line.
     */
    public void createGaps(){
        WordBlock prev = null;
        for (WordBlock b : wordBlocks) {
            int bX1 = b.getX1()-getX1();
            if (prev == null) {
                prev = b;
            } else if (b.getX1() - prev.getX2() > 0) {
                //We need to add a gap of the size of the distance between the word blocks
                int prevX2 = prev.getX2()-getX1();

                Gap newGap = new Gap(prevX2,bX1);
                newGap.setGlobalCorrectionFactor(getX1());
                addGap(newGap);
            }

            //TODO Check for Gaps inside of the WordBlock - Complications. How to determine Char width? --> Sebastian?
        }
    }

    public void addGap(Gap gap){
        if(!gaps.contains(gap))
            gaps.add(gap);
    }

    public ArrayList<Gap> getGaps(){
        return gaps;
    }

    public void setWordBlocks(ArrayList<WordBlock> wordBlocks){
        this.wordBlocks = wordBlocks;
    }

    public ArrayList<WordBlock> getWordBlocks(){
        return wordBlocks;
    }

    public boolean collidesWith(Line l){
        if(ytop >= l.ybottom && l.ytop >= ybottom)
            //TODO FIND COLLISION CLAUSE
            if((xright >= l.xleft && xleft <= l.xright )
                    ||(xleft<=l.xright && xright >= l.xleft)
                    ||(xright < l.xright && xleft > l.xleft))
                return true;
        return false;
    }


    /**
     * Splits this line on the given coordinate
     * TODO Solve issue of splitting WordBlocks!
     * @param splitCoord The local coordinate on which to split
     * @return An array of fixed size = 2 with [0] being the left half and [1] the right.
     */
    public Line[] split(int splitCoord) {
        Line[] returner = new Line[2];

        ArrayList<WordBlock> leftBlocks = new ArrayList<>(),
                rightBlocks = new ArrayList<>();

        WordBlock onBlock = isOnWordBlock(splitCoord);
        if(onBlock != null){
            for(WordBlock wb : wordBlocks){
                int wbX1 = wb.getX1()-getX1();
                if(wbX1 < splitCoord)
                    leftBlocks.add(wb);
                else
                    rightBlocks.add(wb);
            }
        }

        returner[0] = new Line(leftBlocks);
        returner[1] = new Line(rightBlocks);
        return returner;
    }

    /**
     * Checks whether a given coordinate is on a WordBlock and returns the colliding block
     * @param splitCoord
     * @return
     */
    private WordBlock isOnWordBlock(int splitCoord) {

        for(WordBlock wb : wordBlocks){
            int wbX1 = wb.getX1()-getX1();
            int wbX2 = wb.getX2()-getX1();
            boolean a = wbX1 <= splitCoord && wbX2 >= splitCoord;
            if(a)
                return wb;
        }

        return null;
    }

    public double distanceTo(Line line){
        return distanceTo(line, "");
    }

    public double distanceTo(Line line, String direction){
        if(line == null)
            return -1;
        switch (direction){
            case UP:
                return ytop - line.ybottom;
            case DOWN:
                return line.ytop - ybottom;
            case LEFT:
                return xleft - line.xright;
            case RIGHT:
                return line.xleft - xright;
            default:
                double centerX = (xright-xleft)/2, centerY = (ybottom-ytop)/2;
                double centerLineX = (line.xright-line.xleft)/2, centerLineY = (line.ytop-ybottom)/2;
                return Math.sqrt(Math.pow(centerX - centerLineX, 2) + Math.pow(centerY - centerLineY, 2));
        }
    }

    public int getX1(){
        return xleft;
    }

    public int getX2(){
        return xright;
    }

    public int getY1(){
        return ytop;
    }

    public int getY2(){
        return ybottom;
    }

    public int getHeight(){
        return getY2() - getY1();
    }

    public int getWidth(){
        return getX2() - getX1();
    }

    public String getWord(){
        ArrayList<WordBlock> wordsSorted = new ArrayList<>(wordBlocks);
        Collections.sort(wordsSorted, new Comparator<WordBlock>() {
            @Override
            public int compare(WordBlock o1, WordBlock o2) {
                if(o1.getX1() < o2.getX1())
                    return -1;
                if(o1.getX1() > o2.getX1())
                    return 1;
                return 0;
            }
        });
        String returner = "";
        for(WordBlock w : wordsSorted)
            returner += w.getWord();
        return returner;
    }

    public String toString(){
        String returner = "";
        for(WordBlock b : wordBlocks)
            returner += b.getWord();
        return returner;
    }

    public void setTableProbability(double d){
        tableProbability = d;
    }

    public double getTableProbability(){
        return tableProbability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Line line = (Line) o;

        if (ytop != line.ytop) return false;
        if (ybottom != line.ybottom) return false;
        if (xleft != line.xleft) return false;
        if (xright != line.xright) return false;
        if (wordBlocks != null ? !wordBlocks.equals(line.wordBlocks) : line.wordBlocks != null) return false;
        return !(gaps != null ? !gaps.equals(line.gaps) : line.gaps != null);

    }

    @Override
    public int hashCode() {
        int result = ytop;
        result = 31 * result + ybottom;
        result = 31 * result + xleft;
        result = 31 * result + xright;
        result = 31 * result + (wordBlocks != null ? wordBlocks.hashCode() : 0);
        result = 31 * result + (gaps != null ? gaps.hashCode() : 0);
        return result;
    }

}