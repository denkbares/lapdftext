package com.denkbares.lapdf.tekno.studio.dashboard.rules;

import edu.isi.bmkeg.lapdf.model.ChunkBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian Schirm on 02.09.2015.
 */
public class RuleMaker {

    public String makeRule(List<ChunkBlock> input){

        FeatureVector[] vectors = new FeatureVector[input.size()];
        ArrayList<ChunkBlock> arrayListInput = new ArrayList<ChunkBlock>(input);
        //Setting all FeatureVectors
        for(int i = 0; i < arrayListInput.size(); i++){
            vectors[i] = new FeatureVector();
        }

        //TODO
        return null;
    }

}
