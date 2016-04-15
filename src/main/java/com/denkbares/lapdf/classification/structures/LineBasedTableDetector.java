/*
 * Copyright (C) 2015 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package com.denkbares.lapdf.classification.structures;

import edu.isi.bmkeg.lapdf.features.ChunkFeatures;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Gap;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 * @author Sebastian Furth (denkbares GmbH), Maximilian Schirm
 * @created 19.08.15
 */
public class LineBasedTableDetector implements StructureDetector {

	//set int method to preferred detection algorithm
	int method = 3;

	@Override
	public double classify(ChunkBlock block, ChunkFeatures features) {

		switch (method){
			case 0 :
				return method1(block, features);
			case 1:
				return method2(block, features);
			default:
				return method3(block, features);
		}

	}

	public void setMethod(int i){
		method = i;
	}

	//TODO Replace with spaceOverlap approach!
	protected double method1(ChunkBlock block, ChunkFeatures features) {
		ArrayList<WordBlock> blocksOfPage = (ArrayList<WordBlock>) block.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);

		blocksOfPage.sort(new Comparator<WordBlock>() {
			@Override
			public int compare(WordBlock o1, WordBlock o2) {
				if (o1.getY1() < o2.getY1())
					return 1;
				else if (o1.getY1() == o2.getY1())
					return 0;
				else
					return -1;
			}
		});

		if (blocksOfPage == null || blocksOfPage.size() == 0)
			return 0;
		else {

			//It would be possible to raise the returned probability with each step, calculating it for each Block separately -
			//- Possibly leading to a more precise result.
			double avgLineDistance = block.getPage().getAvgLineDistance();
			double avgSpacing = block.getPage().getAvgSpacing();

			//
			//Begin STEP 1 : Find all Blocks with a greater than average distance to the next block below
			//

			ArrayList<WordBlock> candidatesA = new ArrayList<>();
			for(int i = 0; i < blocksOfPage.size()-1; i++){
				double distance = blocksOfPage.get(i).getY1()-blocksOfPage.get(i+1).getY2();
				if(distance >= avgLineDistance)
					candidatesA.add(blocksOfPage.get(i));
			}

			//Create a line for each block by getting all colliding blocks on x - axis
			ArrayList<ArrayList<WordBlock>> candidatesB = new ArrayList<ArrayList<WordBlock>>();
			for(WordBlock b : candidatesA){
				ArrayList<WordBlock> line = new ArrayList<>();
				for(WordBlock temp : blocksOfPage){
					//Check collision
					int y1B = b.getY1(), y2B = b.getY2(), y1Temp = temp.getY1(), y2Temp = temp.getY2();
					//Will also add b when encountered. Not a bug. TODO Correct ?!
					if(y2B >= y1Temp && y2Temp >= y1B){
						if(!line.contains(temp))
							line.add(temp);
					}
				}
				candidatesB.add(line);
			}

			//Ensure that no duplicate lines are contained
			for(int i = 0; i < candidatesB.size()-1; i++){
				for(int j = i+1; j < candidatesB.size(); j++){
					if(candidatesB.get(i).equals(candidatesB.get(j)))
						candidatesB.remove(j);
				}
			}

			ArrayList<Line> lines = new ArrayList<>();
			for(ArrayList<WordBlock> temp : candidatesB){
				lines.add(new Line(temp));
			}

			//CandidatesB now contains all lines with irregular vertical distances.
			//Get all adjacent lines and create Blocks composed of those lines.
			ArrayList<ArrayList<Line>> lineClusters = new ArrayList<>();
			//For each line in candidatesB check adjacency
			for(Line line : lines){
				ArrayList<Line> clusterLines = new ArrayList<>();
				clusterLines.add(line);
				for(int i = lines.indexOf(line)+1; i < lines.size(); i++){
					if(i <= lines.size()){
						//TODO CollidesWith needs to be replaced with a move->collision check!
						if(line.collidesWith(lines.get(i)))
							clusterLines.add(lines.get(i));
					}
				}
				//Check whether we found matching lines
				if(clusterLines.size() > 1)
					lineClusters.add(clusterLines);
			}

			//Check whether we actually found lines matching the criteria
			if(lineClusters.size() > 1){
				//further process those clusters, which might turn out to be tables.

				//
				//Begin STEP 2 : Check clusters for vertical seperations
				//


			}
			else{
				//TODO Adjust default value after determining accuracy in tests
				return 0.1;
			}

			return 0;
		}
	}

	//TODO tweak and adjust to work for all documents
	protected double method2(ChunkBlock block, ChunkFeatures features){
		//Method contents were deprecated.
		return 0.0;
	}

	/**
	 * This approach uses the preprocessing done in MaxPowerChunking class to recognize Tables. Further processing will greatly improve results,
	 * but right now this should be the best (and most functional) of all three methods until method 2 is finished - don't forget to remove
	 * the table detection from MPCC once method 2 is finished!
	 * @param block
	 * @param features
	 * @return
	 */
	protected double method3(ChunkBlock block, ChunkFeatures features){
		return block.getTableProbability();
	}

	/**
	 * Creates a boolean array with a cell for every pixel of the TableCandidate
	 * The first index indicates the line of the TC, the second index indicates the pixel
	 * True means that this position is empty (i.e. space char / space between WordBlocks)
	 * False means that this position is filled with a char
	 * @param tableCandidate The (possible) table for which we want to create a map of spaces
	 * @return A 2D boolean array ("map of spaces") where empty coordinates in the input table are "
	 */
	protected boolean[][] createMapOfSpaces(ArrayList<Line> tableCandidate){
		int height = -1, width = -1;
		int topY = -1,bottomY = -1,leftX = -1,rightX = -1;

		//Find maximum dimensions of table candidate
		for(Line l : tableCandidate){
			if(l.getY1() < topY || topY == -1)
				topY = l.getY1();
			if(l.getY2() > bottomY || bottomY == -1)
				bottomY = l.getY2();
			if(l.getX1() < leftX || leftX == -1)
				leftX = l.getX1();
			if(l.getX2() > rightX || rightX == -1)
				rightX = l.getX2();
		}

		//Set height and width and initialize the array
		//Each element in the array represents a pixel in our table candidate rectangle
		height = bottomY-topY;
		width = rightX-leftX;
		boolean[][] map = new boolean[height][width];

		//Set values
		for(Line l : tableCandidate) {
			for (WordBlock w : l.getWordBlocks()) {


				//TODO Implement steps below


				//Transpose coordinates by subtracting the topY and leftX from the respective WordBlock values.
				//This ensures that we always obtain coordinates usable inside of the map[][] array

				//Translate character positions of WordBlocks to Coordinates by
				//multiplying the index with the character width and adding that to the WordBlocks top/left coordinate
				//relative to the table candidate

				//If the character is "space", set the corresponding values in the map[][]

				//Further, look for distances between the word blocks and set all values for the gaps in the map[][]
			}
		}


		return map;
	}

	//Returns a value (0.0-1.0) of how sure we are that the lines are clearly separated.
	//1.0 means maximum seperation, 0 means that the lines are not separated at all
	@Deprecated
	private double getGradeOfSeparation(ArrayList<Line> tableCandidate) {

		for(Line line : tableCandidate) {

			ArrayList<Boolean> lineSpaces = new ArrayList<>();
			ArrayList<Double>  scaleFactor= new ArrayList<>();
			WordBlock prev = null;
			for (WordBlock b : line.getWordBlocks()) {
				if (prev == null) {
					prev = b;
				}
				if (b.getX1() - prev.getX2() > 0) {
					//We need to add a gap of the size of the distance
					line.addGap(new Gap(prev.getX2(), b.getX1()));
				}

				String word = b.getWord();
				for (int i = 0; i < word.length(); i++) {
					if (Character.isSpaceChar(word.charAt(i))) {
						lineSpaces.add(new Boolean(true));
						scaleFactor.add(new Double(b.getWidth()/word.length()));
					} else {
						lineSpaces.add(new Boolean(false));
						scaleFactor.add(new Double(b.getWidth()/word.length()));
					}
				}
			}

			//Find Gaps in the Words of the line and add them to the line
			//We translate the approximate position of chars to coordinates on the page using our scaling factor from before
			double currentCoord = line.getX1();
			//TODO : Adjust Gap creation for Line offset
			for (int i = 0; i < lineSpaces.size(); i++) {
				currentCoord += scaleFactor.get(i).doubleValue();
				if (lineSpaces.get(i).booleanValue()) {
					double beginningCoord = currentCoord;
					while (i < lineSpaces.size() && lineSpaces.get(i)) {
						currentCoord += scaleFactor.get(i).doubleValue();
						i++;
					}

					int beginningCoordInteger = (int) beginningCoord;
					int endCoordInteger = (int) currentCoord;
					line.addGap(new Gap(beginningCoordInteger, endCoordInteger));
				}
			}
		}

		//Let's now see whether the lines happen to have any Gaps in common
		//And yeah, the runtime is horrible
		HashMap<Gap, Integer> overlapCounter = new HashMap<>();
		for(Line line : tableCandidate){
			for(Gap gap : line.getGaps()){
				for(Line otherLine : tableCandidate){
					if(!line.equals(otherLine)){
						for(Gap otherGap : otherLine.getGaps()){
							if(gap.doesOverlap(otherGap)){
								if(overlapCounter.containsKey(gap))
									overlapCounter.put(gap, new Integer(overlapCounter.get(gap) +1));
								else if(overlapCounter.containsKey(otherGap))
									overlapCounter.put(otherGap,new Integer(overlapCounter.get(otherGap) +1));
								else
									overlapCounter.put(gap, new Integer(1));
							}
						}
					}
				}
			}
		}

		double returner = 0;
		for(Gap g : overlapCounter.keySet()){
			//Count how often a Gap occurred.
			int count = overlapCounter.get(g).intValue();
			returner = returner + (1 /(double) overlapCounter.size()) * count;
		}

		//
		//TODO CONTINUE
		//


		return (returner > 1) ? 1 : returner;
	}

}