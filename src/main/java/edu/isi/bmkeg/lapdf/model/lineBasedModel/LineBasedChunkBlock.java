package edu.isi.bmkeg.lapdf.model.lineBasedModel;

import edu.isi.bmkeg.lapdf.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.isi.bmkeg.lapdf.model.RTree.RTSpatialEntity;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;

/**
 * Created by Maximilian on 11.03.2016.
 */
public class LineBasedChunkBlock extends RTSpatialEntity implements ChunkBlock {

    int x1,x2,y1,y2;
    ArrayList<Line> lines;
    ArrayList<WordBlock> wordBlocks = new ArrayList<>();


    public static LineBasedChunkBlock buildLineBasedChunkBlock(ArrayList<Line> lines){
        int[] bounds = createBounds(lines);
        int x1,x2,y1,y2;
        x1 = bounds[0];
        x2 = bounds[1];
        y1 = bounds[2];
        y2 = bounds[3];
        LineBasedChunkBlock block = new LineBasedChunkBlock(x1,y1,x2,y2,0);
        block.setLines(lines);
        return block;
    }

    public LineBasedChunkBlock(int x1, int y1, int x2, int y2, int order){
        super(x1, y1, x2, y2, order);
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    }

    public void setLines(ArrayList<Line> lines){
        this.lines = lines;
        refreshWordBlocks();
    }

    private static int[] createBounds(ArrayList<Line> lines){
        int xMin = -1, xMax = -1, yMin = -1, yMax = -1;
        for(Line l : lines){
            if(l.getX1() < xMin || xMin == -1)
                xMin = l.getX1();
            if(l.getX2() > xMax || xMax == -1)
                xMax = l.getX2();
            if(l.getY1() < yMin || yMin == -1)
                yMin = l.getY1();
            if(l.getY2() > yMax || yMax == -1)
                yMax = l.getY2();
        }
        int[] returner = {xMin,xMax,yMin,yMax};
        return returner;
    }

    private void refreshWordBlocks(){
        wordBlocks = new ArrayList<WordBlock>();
        Collections.sort(lines, new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                if (o1.getY1() < o2.getY1())
                    return 1;
                else if (o1.getY1() == o2.getY1())
                    return 0;
                else
                    return -1;
            }
        });

        for(Line l : lines){
            wordBlocks.addAll(l.getWordBlocks());
        }
    }

    @Override
    public String readChunkText(){
        String returner = "";
        for(Line l : lines){
            returner += l.getWord();
        }
        return returner;
    }

    @Override
    public int getX1() {
        return x1;
    }

    @Override
    public int getX2() {
        return x2;
    }

    @Override
    public int getY1() {
        return y1;
    }

    @Override
    public int getY2() {
        return y2;
    }

    public ArrayList<Line> getLines() {
        return lines;
    }

    @Override
    public int getHeight(){
        return getY2()-getY1();
    }

    @Override
    public int getWidth(){
        return getX2()-getX1();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //End of my implementation
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //Taken from RTChunkBlock
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private Block container;
    private int mostPopularWordHeight;
    private int mostPopularWordSpaceWidth;
    private String mostPopularWordFont = "";
    private String mostPopularWordStyle = "";
    private Boolean headerOrFooter=null;
    private List<WordBlock> rotatedWords = new ArrayList<>();
    private String type = ChunkBlock.TYPE_UNCLASSIFIED;
    private boolean wasClassified = false;
    private String alignment = null;

    @Override
    public boolean getWasClassified() {
        return wasClassified;
    }

    @Override
    public void setWasClassified(boolean wasClassified) {
        this.wasClassified = wasClassified;
    }

    @Override
    public String getType() {
        return type;
    }

    double tableProb = 0.0;
    @Override
    public void setTableProbability(double d) {
        tableProb = d;
    }

    @Override
    public double getTableProbability() {
        return tableProb;
    }

    double unorderedListProb = 0.0;
    @Override
    public void setUnorderedListProbability(double d){
        unorderedListProb = d;
    }

    @Override
    public double getUnorderedListProbability(){
        return unorderedListProb;
    }

    double orderedListProbability = 0.0;
    @Override
    public void setOrderedListProbability(double d){
        orderedListProbability = d;
    }

    @Override
    public double getOrderedListProbability(){
        return orderedListProbability;
    }

    @Override
    public List<WordBlock> getWordBlocks() {
        return wordBlocks;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<WordBlock> getRotatedWords() {
        return rotatedWords;
    }

    @Override
    public void setRotatedWords(List<WordBlock> rotatedWords) {
        this.rotatedWords = rotatedWords;
    }

    @Override
    public Boolean isHeaderOrFooter() {
        return headerOrFooter;
    }

    @Override
    public void setHeaderOrFooter(boolean headerOrFooter) {
        this.headerOrFooter = new Boolean(headerOrFooter);
    }

    @Override
    public PageBlock getPage() {
        return (PageBlock) this.container;
    }

    @Override
    public void setPage(PageBlock page){
        this.container = page;
    }

    @Override
    public Block getContainer() {
        return container;
    }

    @Override
    public void setContainer(Block container) {
        this.container = container;
    }

    @Override
    public String readLeftRightMidLine() {
        if (this.alignment != null)
            return this.alignment;

        PageBlock page = (PageBlock) this.getContainer();
        int median = page.getMedian();
        int X1 = this.getX1();
        int width = this.getWidth();

        if ( X1 + width < median ) {

            this.alignment = LEFT;

        } else if (X1 > median) {

            this.alignment = RIGHT;

        } else {
            this.alignment = MIDLINE;
        }
        return this.alignment;
    }

    @Override
    public boolean isFlush(String condition, int value) {
        PageBlock parent = (PageBlock) this.getContainer();
        int median = parent.getMedian();
        String leftRightMidline = this.readLeftRightMidLine();

        int x1 = this.getX1();
        int x2 = this.getX2();
        int marginX1 = parent.getMargin()[0];
        int marginX2 = parent.getMargin()[3];

        if (condition.equals(MIDLINE)) {
            if (leftRightMidline.equals(MIDLINE))
                return false;
            else if (leftRightMidline.equals(LEFT)
                    && Math.abs(x2 - median) < value)
                return true;
            else if (leftRightMidline.equals(RIGHT)
                    && Math.abs(x1 - median) < value)
                return true;
        } else if (condition.equals(LEFT)) {
            if (leftRightMidline.equals(MIDLINE)
                    && Math.abs(x1 - marginX1) < value)
                return true;
            else if (leftRightMidline.equals(LEFT)
                    && Math.abs(x1 - marginX1) < value)
                return true;
            else if (leftRightMidline.equals(RIGHT))
                return false;
        } else if (condition.equals(RIGHT)) {
            if (leftRightMidline.equals(MIDLINE)
                    && Math.abs(x2 - marginX2) < value)
                return true;
            else if (leftRightMidline.equals(LEFT))
                return false;
            else if (leftRightMidline.equals(RIGHT)
                    && Math.abs(x2 - marginX2) < value)
                return true;
        }
        return false;
    }

    @Override
    public int getMostPopularWordHeight() {
        return mostPopularWordHeight;
    }

    @Override
    public void setMostPopularWordHeight(int mostPopularWordHeight) {
        this.mostPopularWordHeight = mostPopularWordHeight;
    }

    @Override
    public int getMostPopularWordSpaceWidth() {
        return mostPopularWordSpaceWidth;
    }

    @Override
    public void setMostPopularWordSpaceWidth(int mostPopularWordSpaceWidth) {
        this.mostPopularWordSpaceWidth = mostPopularWordSpaceWidth;
    }

    @Override
    public String getMostPopularWordFont() {
        return mostPopularWordFont;
    }

    @Override
    public void setMostPopularWordFont(String mostPopularWordFont) {
        this.mostPopularWordFont = mostPopularWordFont;
    }

    @Override
    public String getMostPopularWordStyle() {
        return mostPopularWordStyle;
    }

    @Override
    public void setMostPopularWordStyle(String mostPopularWordStyle) {
        this.mostPopularWordStyle = mostPopularWordStyle;
    }

    //This could be optimized once a LineBasedPageBlock is introduced
    @Override
    public int readNumberOfLine() {
        PageBlock parent = (PageBlock) this.container;
        List<SpatialEntity> wordBlockList = parent.containsByType(this,
                SpatialOrdering.MIXED_MODE, WordBlock.class);
        if (wordBlockList.size() == 0)
            return 0;
        WordBlock block = (WordBlock) wordBlockList.get(0);
        int numberOfLines = 1;
        int lastY = block.getY1() + block.getHeight() / 2;
        int currentY = lastY;
        for (SpatialEntity entity : wordBlockList) {
            lastY = currentY;
            block = (WordBlock) entity;
            currentY = block.getY1() + block.getHeight() / 2;
            if (currentY > lastY + block.getHeight() / 2)
                numberOfLines++;

        }
        return numberOfLines;
    }

    @Override
    public ChunkBlock readLastChunkBlock() {

        List<ChunkBlock> sortedChunkBlockList = ((PageBlock) this
                .getContainer())
                .getAllChunkBlocks(SpatialOrdering.MIXED_MODE);

        int index = Collections.binarySearch(sortedChunkBlockList, this,
                new SpatialOrdering(SpatialOrdering.MIXED_MODE));

        return (index <= 0) ? null : sortedChunkBlockList.get(index - 1);
    }

    @Override
    public boolean isMatchingRegularExpression(String regex) {
        Pattern pattern = Pattern.compile(regex);

        String text = this.readChunkText();
        Matcher matcher = pattern.matcher(text);

        if (matcher.find())
            return true;

        return false;
    }

    //This could be optimized once a LineBasedPageBlock is introduced
    @Override
    public boolean isUnderOneLineFlushNeighboursOfType(String type) {

        List<ChunkBlock> list = getOverlappingNeighbors(LapdfDirection.NORTH,
                (PageBlock) this.getContainer(),
                (ChunkBlock) this);

        double threshold = this.getMostPopularWordHeight() * 2;

        for (ChunkBlock chunky : list) {

            int delta1 = Math.abs(chunky.getX1() - this.getX1());
            int delta2 = Math.abs(chunky.getX2() - this.getX2());

            if( delta1 < threshold
                    && delta2 < threshold
                    && chunky.readNumberOfLine() == 1
                    && chunky.getType().equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;

    }

    //This could be optimized once a LineBasedPageBlock is introduced
    public List<ChunkBlock> getOverlappingNeighbors(
            int nsew,
            PageBlock parent,
            ChunkBlock chunkBlock) {

        int topX = chunkBlock.getX1();
        int topY = chunkBlock.getY1();
        int width = chunkBlock.getWidth();
        int height = chunkBlock.getHeight();

        if (nsew == LapdfDirection.NORTH) {
            height = height / 2;
            topY = topY - height;
        } else if (nsew == LapdfDirection.SOUTH) {
            topY = topY + height;
            height = height / 2;
        } else if (nsew == LapdfDirection.EAST) {
            topX = topX + width;
            width = width / 2;
        } else if (nsew == LapdfDirection.WEST) {
            width = width / 2;
            topX = topX - width;
        } else if (nsew == LapdfDirection.NORTH_SOUTH) {
            topY = topY - height / 2;
            height = height * 2;
        } else if (nsew == LapdfDirection.EAST_WEST) {
            topX = topX - width / 2;
            width = width * 2;

        }

        SpatialEntity entity = new LineBasedChunkBlock(topX, topY, topX
                + width, topY + height, -1);

        List<ChunkBlock> l = new ArrayList<ChunkBlock>();
        Iterator<SpatialEntity> it = parent.intersectsByType(
                entity, null, ChunkBlock.class).iterator();
        while( it.hasNext() ) {
            l.add((ChunkBlock)it.next());
        }

        return l;

    }

    //This could be optimized once a LineBasedPageBlock is introduced
    @Override
    public boolean hasNeighboursOfType(String type, int nsew) {

        List<ChunkBlock> list = getOverlappingNeighbors(nsew,
                (PageBlock) this.getContainer(),
                (ChunkBlock) this);

        for (ChunkBlock chunky : list)
            if (chunky.getType().equalsIgnoreCase(type))
                return true;

        return false;

    }

    @Override
    public double readDensity(){
        double areaBlock = 0.0, wordsArea = 0.0;

        areaBlock = getHeight()*getWidth();

        for(Line l : lines){
            for(WordBlock b : l.getWordBlocks()){
                wordsArea += b.getHeight()*b.getWidth();
            }
        }

        return wordsArea/areaBlock;

    }

    @Override
    public List<WordBlock> getWordBlocks(String ordering){
        return getWordBlocks();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //End content copied from RTChunkBlock
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LineBasedChunkBlock that = (LineBasedChunkBlock) o;

        if (getX1() != that.getX1()) return false;
        if (getX2() != that.getX2()) return false;
        if (getY1() != that.getY1()) return false;
        if (getY2() != that.getY2()) return false;
        if (getMostPopularWordHeight() != that.getMostPopularWordHeight()) return false;
        if (getMostPopularWordSpaceWidth() != that.getMostPopularWordSpaceWidth()) return false;
        if (!getWordBlocks().equals(that.getWordBlocks())) return false;
        if (!getContainer().equals(that.getContainer())) return false;
        if (getMostPopularWordFont() != null ? !getMostPopularWordFont().equals(that.getMostPopularWordFont()) : that.getMostPopularWordFont() != null)
            return false;
        if (getMostPopularWordStyle() != null ? !getMostPopularWordStyle().equals(that.getMostPopularWordStyle()) : that.getMostPopularWordStyle() != null)
            return false;
        if (headerOrFooter != null ? !headerOrFooter.equals(that.headerOrFooter) : that.headerOrFooter != null)
            return false;
        if (getRotatedWords() != null ? !getRotatedWords().equals(that.getRotatedWords()) : that.getRotatedWords() != null)
            return false;
        return !(alignment != null ? !alignment.equals(that.alignment) : that.alignment != null);

    }

    @Override
    public int hashCode() {
        int result = getX1();
        result = 31 * result + getX2();
        result = 31 * result + getY1();
        result = 31 * result + getY2();
        result = 31 * result + getWordBlocks().hashCode();
        result = 31 * result + getContainer().hashCode();
        result = 31 * result + getMostPopularWordHeight();
        result = 31 * result + getMostPopularWordSpaceWidth();
        result = 31 * result + (getMostPopularWordFont() != null ? getMostPopularWordFont().hashCode() : 0);
        result = 31 * result + (getMostPopularWordStyle() != null ? getMostPopularWordStyle().hashCode() : 0);
        result = 31 * result + (headerOrFooter != null ? headerOrFooter.hashCode() : 0);
        result = 31 * result + (getRotatedWords() != null ? getRotatedWords().hashCode() : 0);
        result = 31 * result + (alignment != null ? alignment.hashCode() : 0);
        return result;
    }

}
