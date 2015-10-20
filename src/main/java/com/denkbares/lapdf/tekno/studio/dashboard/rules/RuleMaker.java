package com.denkbares.lapdf.tekno.studio.dashboard.rules;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian Schirm on 02.09.2015.
 */
public class RuleMaker {

    public Rule makeRule(List<ChunkBlock> input){
        ArrayList<FeatureVector> featureVectors = new ArrayList<FeatureVector>();
        Rule output = new Rule("","");
        for(ChunkBlock c : input){
            featureVectors.add(new FeatureVector(c));
        }
        //Compare ChunkBlocks on all domains. If sufficient amount (= k) of ChunkBlocks agree on a property, add property to rule.
        //Domains are : density:double; headerOrFooter,wasClassified:boolean; wordSpace,line,wordHeight,width,height,page,x1,x2,y1,y2:int; # of rotated Words (int from List)
        ChunkBlock pivot = input.get(0);
        FeatureVector ruleVector = new FeatureVector(pivot);
        for(int i = 1; i < featureVectors.size(); i++){
            FeatureVector loopVector = featureVectors.get(i);

            if(ruleVector.state[0] && !loopVector.matchesDensity(pivot))
                ruleVector.setDensityState(false);

            if(ruleVector.state[1] && !loopVector.matchesHeaderOrFooter(pivot))
                ruleVector.setHeaderOrFooterState(false);

            if(ruleVector.state[2] && !loopVector.matchesWasClassified(pivot))
                ruleVector.setWasClassifiedState(false);

            if(ruleVector.state[3] && !loopVector.matchesWordSpace(pivot))
                ruleVector.setWordSpaceState(false);

            if(ruleVector.state[4] && !loopVector.matchesLine(pivot))
                ruleVector.setLineState(false);

            if(ruleVector.state[5] && !loopVector.matchesWordHeight(pivot))
                ruleVector.setWordHeightState(false);

            if(ruleVector.state[6] && !loopVector.matchesWidth(pivot))
                ruleVector.setWidthState(false);

            if(ruleVector.state[7] && !loopVector.matchesHeight(pivot))
                ruleVector.setHeightState(false);

            if(ruleVector.state[8] && !loopVector.matchesPage(pivot))
                ruleVector.setPageState(false);

            if(ruleVector.state[9] && !loopVector.matchesX1(pivot))
                ruleVector.setX1State(false);

            if(ruleVector.state[10] && !loopVector.matchesX2(pivot))
                ruleVector.setX2State(false);

            if(ruleVector.state[11] && !loopVector.matchesY1(pivot))
                ruleVector.setY1State(false);

            if(ruleVector.state[12] && !loopVector.matchesY2(pivot))
                ruleVector.setY2State(false);

            if(ruleVector.state[13] && !loopVector.matchesNoRotWord(pivot))
                ruleVector.setNoRotWordState(false);

            //Phew.
        }

        //RuleVector.state is now true where all ChunkBlocks are the same
        //Generate Rule TODO Dummy Rule Generation, move to proper Rule making
        String ruleText = "";

        for(int i = 0; i < ruleVector.state.length; i++){
            if(ruleVector.state[i]){
                switch (i){
                    case 0:
                        ruleText += "Density = " + pivot.readDensity() +",\n";
                        break;
                    case 1:
                        ruleText += "Is Header or Footer = " + pivot.isHeaderOrFooter() +",\n";
                        break;
                    case 2:
                        ruleText += "WasClassified = " + pivot.getWasClassified() +",\n";
                        break;
                    case 3:
                        ruleText += "WordSpace = " + pivot.getMostPopularWordSpaceWidth() +",\n";
                        break;
                    case 4:
                        ruleText += "Number of Line = " + pivot.readNumberOfLine() +",\n";
                        break;
                    case 5:
                        ruleText += "WordHeight = " + pivot.getMostPopularWordHeight() +",\n";
                        break;
                    case 6:
                        ruleText += "Width = " + pivot.getWidth() +",\n";
                        break;
                    case 7:
                        ruleText += "Height = " + pivot.getHeight() +",\n";
                        break;
                    case 8:
                        ruleText += "Page Number = " + pivot.getPage().getPageNumber() +",\n";
                        break;
                    case 9:
                        ruleText += "X-Left = " + pivot.getX1() +",\n";
                        break;
                    case 10:
                        ruleText += "X-Right = " + pivot.getX2() +",\n";
                        break;
                    case 11:
                        ruleText += "Y-Top = " + pivot.getY1() +",\n";
                        break;
                    case 12:
                        ruleText += "Y-Bottom = " + pivot.getY2() +",\n";
                        break;
                    case 13:
                        ruleText += "NoRotWord = " + pivot.getRotatedWords().size() +",\n";
                        break;
                }
            }

        }
        System.out.println("New Rule text was : " + ruleText);
        return new Rule(ruleText,"");
    }

}