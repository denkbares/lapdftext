package com.denkbares.lapdf.tekno.studio.dashboard.rules;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;

/**
 * Created by Maximilian Schirm on 02.09.2015.
 */
public class FeatureVector {
/*
    //Static according to the ChunkFeatures. State of 1.9.2015 (#31 Features)
    String[] vectors = new String[31];
    String[] featureNames = {
            "Alignment",
            "Is Aligned with Column Boundaries?",
            "Is Column Centered?",
            "Is Outlier?",
            "Is in Top Half?",
            "Number of Line",
            "Is Header or Footer",
            "X-Left",
            "X-Right",
            "Y-Top",
            "Y-Bottom",
            "Height",
            "Width",
            "Font Name",
            "Most Popular Font Size",
            "Is all Capitals?",
            "Is most popular font modifier bold?",
            "Is most popular font modifier italic?",
            "Is most popular font in document?",
            "Is next most popular font in document?",
            "Chunk Text Length",
            "Density",
            "Height Difference between Chunk Word and Document Word",
            "Contains first Line of Page?",
            "Contains last Line of Page?",
            "Chunk Text",
            "Page Number",
            "Is Table? (confidence)",
            "Is Ordered List? (confidence)",
            "Is Unordered List? (confidence)",
            "Last Classification"
    };
    //Valid Answers for Choices
    String[] yn = {'Yes', 'No'};
    String[] alignmentOC = {"Left", "Right", "Midline"};

    String[] validAnswerType = {
            "alignmentOC",
            "yn",
            "yn",
            "yn",
            "yn",
            "num",
            "yn",
            "num",
            "num",
            "num",
            "num",
            "num",
            "num",
            "text",
            "num",
            "yn",
            "yn",
            "yn",
            "yn",
            "yn",
            "num",
            "num",
            "num",
            "yn",
            "yn",
            "text",
            "num",
            "num",
            "num",
            "num",
            "text"
    };

    protected boolean checkIsValid(){
        for(int i = 0; i < vectors.length; i++){
            String vectorValue= vectors[i];
            switch(validAnswerType[i]){
                case"alignmentOC":
                    if(!(vectorValue.equalsIgnoreCase(alignmentOC[0]) || vectorValue.equalsIgnoreCase(alignmentOC[1]) || vectorValue.equalsIgnoreCase(alignmentOC[2])))
                        return false;
                    break;
                case"num":
                    if(!vectorValue.matches("\d+"))
                        return false;
                    break;
                case"yn":
                    if(!(vectorValue.equalsIgnoreCase(yn[0]) || vectorValue.equalsIgnoreCase(yn[1])))
                        return false;
                    break;
            }
        }
        return true;
    }

    public FeatureVector(ChunkBlock b){
        //Creates a feature Vector from b
        vectors[0] = b.readLeftRightMidLine();
        vectors[1] = b.;
        vectors[2] = b.;
        vectors[3] = b.;
        vectors[4] = ""+b.readNumberOfLine();
        vectors[5] = b.isHeaderOrFooter() ? "Yes" : "No";
        vectors[6] = ""+b.getX1();
        vectors[7] = ""+b.getX2();
        vectors[8] = ""+b.getY2();
        vectors[9] = ""+b.getY1();
        vectors[10] = ""+b.getHeight();
        vectors[11] = ""+b.getWidth();
        vectors[12] = b.getMostPopularWordFont();
        vectors[13] = ""+b.getMostPopularWordHeight();
        vectors[14] = b.;
        vectors[15] = b.;
        vectors[16] = b.;
        vectors[17] = b.;
        vectors[18] = b.;
        vectors[19] = ""+b.readChunkText().length();
        vectors[20] = ""+b.readDensity();
        vectors[21] = b.;
        vectors[22] = b.;
        vectors[23] = b.;
        vectors[24] = b.;
        vectors[25] = b.;
        vectors[26] = b.;
        vectors[27] = b.;
        vectors[28] = b.;
        vectors[29] = b.;
        vectors[30] = b.;






    }


**/

    //Domains are : density:double; headerOrFooter,wasClassified:boolean; wordSpace,line,wordHeight,width,height,page,x1,x2,y1,y2:int; # of rotated Words (int from List)
    double density;
    boolean headerOrFooter, wasClassified;
    int wordSpace, line, wordHeight, width, height, page, x1, x2, y1, y2, noRotWord;
    boolean[] state = {true, true, true, true, true, true, true, true, true, true, true, true, true, true};

    public FeatureVector(ChunkBlock b){
        try{
            //Assign
            density = b.readDensity();
            //TODO DEBUG WTF IS THE PROBLEM HERE
//            headerOrFooter = b.isHeaderOrFooter().booleanValue();
            wasClassified = b.getWasClassified();
            wordSpace = b.getMostPopularWordSpaceWidth();
            line = b.readNumberOfLine();
            wordHeight = b.getMostPopularWordHeight();
            width = b.getWidth();
            height = b.getHeight();
            page = b.getPage().getPageNumber();
            x1 = b.getX1(); x2 = b.getX2();
            y1 = b.getY1(); y2 = b.getY2();
            noRotWord = b.getRotatedWords().size();
        }
        catch(Exception e){
            //Failed to init. b == null?!
            throw e;
        }
    }

    //Density
    public double getDensity () {
        return density;
    }

    public void setDensityState(boolean b){
        state[0] = b;
    }

    public boolean matchesDensity (ChunkBlock b){
        return density == b.readDensity();
    }

    //HeaderOrFooter
    public boolean getHeaderOrFooter () {
        return headerOrFooter;
    }

    public void setHeaderOrFooterState(boolean b){
        state[1] = b;
    }

    public boolean matchesHeaderOrFooter (ChunkBlock b){
        //TODO Debug this!
        return true;
        //return headerOrFooter == b.isHeaderOrFooter();
    }

    //WasClassified
    public boolean getWasClassified () {
        return wasClassified;
    }

    public void setWasClassifiedState(boolean b){
        state[2] = b;
    }

    public boolean matchesWasClassified (ChunkBlock b){
        return wasClassified = b.getWasClassified();
    }

    //WordSpace
    public int getWordSpace () {
        return wordSpace;
    }

    public void setWordSpaceState(boolean b){
        state[3] = b;
    }

    public boolean matchesWordSpace (ChunkBlock b){
        return wordSpace == b.getMostPopularWordSpaceWidth();
    }

    //Line
    public int getLine () {
        return line;
    }

    public void setLineState(boolean b){
        state[4] = b;
    }

    public boolean matchesLine (ChunkBlock b){
        return line == b.readNumberOfLine();
    }

    //WordHeight
    public int getWordHeight () {
        return wordHeight;
    }

    public void setWordHeightState(boolean b){
        state[5] = b;
    }

    public boolean matchesWordHeight (ChunkBlock b){
        return wordHeight == b.getMostPopularWordHeight();
    }

    //Width
    public int getWidth () {
        return width;
    }

    public void setWidthState(boolean b){
        state[6] = b;
    }

    public boolean matchesWidth (ChunkBlock b){
        return  width == b.getWidth();
    }

    //Height
    public int getHeight () {
        return height;
    }

    public void setHeightState(boolean b){
        state[7] = b;
    }

    public boolean matchesHeight (ChunkBlock b){
        return height == b.getHeight();
    }

    //Page
    public int getPage () {
        return page;
    }

    public void setPageState(boolean b){
        state[8] = b;
    }

    public boolean matchesPage (ChunkBlock b){
        return page == b.getPage().getPageNumber();
    }

    //X1
    public int getX1 () {
        return x1;
    }

    public void setX1State(boolean b){
        state[9] = b;
    }

    public boolean matchesX1 (ChunkBlock b){
        return x1 == b.getX1();
    }

    //X2
    public int getX2 () {
        return x2;
    }

    public void setX2State(boolean b){
        state[10] = b;
    }

    public boolean matchesX2 (ChunkBlock b){
        return x2 == b.getX2();
    }

    //Y1
    public int getY1 () {
        return y1;
    }

    public void setY1State(boolean b){
        state[11] = b;
    }

    public boolean matchesY1 (ChunkBlock b){
        return y1 == b.getY1();
    }

    //Y2
    public int getY2 () {
        return y2;
    }

    public void setY2State(boolean b){
        state[12] = b;
    }

    public boolean matchesY2 (ChunkBlock b){
        return y2 == b.getY2();
    }

    //NoRotWord
    public int getNoRotWord () {
        return noRotWord;
    }

    public void setNoRotWordState(boolean b){
        state[13] = b;
    }

    public boolean matchesNoRotWord (ChunkBlock b){
        return noRotWord == b.getRotatedWords().size();
    }
}