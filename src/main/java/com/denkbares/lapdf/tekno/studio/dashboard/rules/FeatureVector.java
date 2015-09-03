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
}