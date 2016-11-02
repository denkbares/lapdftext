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

import java.util.ArrayList;
import java.util.Comparator;

import edu.isi.bmkeg.lapdf.features.ChunkFeatures;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Gap;
import edu.isi.bmkeg.lapdf.model.lineBasedModel.Line;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.utils.LineBasedOperations;
import edu.isi.bmkeg.lapdf.utils.PageOperations;

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
	public double classify(ChunkBlock block) {

		switch (method) {
			case 0:
				return method1(block);
			case 1:
				return method2(block);
			default:
				return method3(block);
		}

	}

	public void setMethod(int i) {
		method = i;
	}

	//TODO Replace with spaceOverlap approach!
	protected double method1(ChunkBlock block) {
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
			for (int i = 0; i < blocksOfPage.size() - 1; i++) {
				double distance = blocksOfPage.get(i).getY1() - blocksOfPage.get(i + 1).getY2();
				if (distance >= avgLineDistance)
					candidatesA.add(blocksOfPage.get(i));
			}

			//Create a line for each block by getting all colliding blocks on x - axis
			ArrayList<ArrayList<WordBlock>> candidatesB = new ArrayList<ArrayList<WordBlock>>();
			for (WordBlock b : candidatesA) {
				ArrayList<WordBlock> line = new ArrayList<>();
				for (WordBlock temp : blocksOfPage) {
					//Check collision
					int y1B = b.getY1(), y2B = b.getY2(), y1Temp = temp.getY1(), y2Temp = temp.getY2();
					//Will also add b when encountered. Not a bug. TODO Correct ?!
					if (y2B >= y1Temp && y2Temp >= y1B) {
						if (!line.contains(temp))
							line.add(temp);
					}
				}
				candidatesB.add(line);
			}

			//Ensure that no duplicate lines are contained
			for (int i = 0; i < candidatesB.size() - 1; i++) {
				for (int j = i + 1; j < candidatesB.size(); j++) {
					if (candidatesB.get(i).equals(candidatesB.get(j)))
						candidatesB.remove(j);
				}
			}

			ArrayList<Line> lines = new ArrayList<>();
			for (ArrayList<WordBlock> temp : candidatesB) {
				lines.add(new Line(temp));
			}

			//CandidatesB now contains all lines with irregular vertical distances.
			//Get all adjacent lines and create Blocks composed of those lines.
			ArrayList<ArrayList<Line>> lineClusters = new ArrayList<>();
			//For each line in candidatesB check adjacency
			for (Line line : lines) {
				ArrayList<Line> clusterLines = new ArrayList<>();
				clusterLines.add(line);
				for (int i = lines.indexOf(line) + 1; i < lines.size(); i++) {
					if (i <= lines.size()) {
						//TODO CollidesWith needs to be replaced with a move->collision check!
						if (line.collidesWith(lines.get(i)))
							clusterLines.add(lines.get(i));
					}
				}
				//Check whether we found matching lines
				if (clusterLines.size() > 1)
					lineClusters.add(clusterLines);
			}

			//Check whether we actually found lines matching the criteria
			if (lineClusters.size() > 1) {
				//further process those clusters, which might turn out to be tables.

				//
				//Begin STEP 2 : Check clusters for vertical seperations
				//


			} else {
				//TODO Adjust default value after determining accuracy in tests
				return 0.1;
			}

			return 0;
		}
	}

	/**
	 * This approach uses the preprocessing done in MaxPowerChunking class to recognize Tables. Further processing will greatly improve results,
	 * but right now this should be the best (and most functional) of the methods - don't forget to remove
	 * the table detection from MPCC once method 2 is finished!
	 *
	 * @param block    The block for which we want to determine the TableProbability
	 * @return A value 0.0-1.0 how likely this ChunkBlock contains a table or is part of a table
	 */
	protected double method2(ChunkBlock block) {
		return block.getTableProbability();
	}

	/**
	 * Far more simple approach to finding the table probability.
	 * 1. Creates Lines for each WordBlock
	 * 2. Finds overlapping gaps upwards and downwards
	 * 3. Depending on the count of continuous gaps calculates per WB pTable
	 * 4. Returns mean pTable of all WBs of block
	 *
	 * @param block
	 * @param features
	 * @return
	 */
	protected double method3(ChunkBlock block){
		double pTableSum = 0.0;
		for(WordBlock wb : block.getWordBlocks()){
			pTableSum += checkForSeparation(wb);
		}
		pTableSum = pTableSum / block.getWordBlocks().size();

		block.setTableProbability(pTableSum);
		return pTableSum;
	}

	/**
	 * Creates a boolean array with a cell for every pixel of the TableCandidate
	 * The first index indicates the line of the TC, the second index indicates the pixel
	 * True means that this position is empty (i.e. space char / space between WordBlocks)
	 * False means that this position is filled with a char
	 * <p>
	 * TODO : There is an issue preventing this approach to be realized - there is no way to obtain a precise
	 * TODO   measure for the exact width of a letter in the given font and size.
	 *
	 * @param tableCandidate The (possible) table for which we want to create a map of spaces
	 * @return A 2D boolean array ("map of spaces") where empty coordinates in the input table are "
	 */
	protected boolean[][] createMapOfSpaces(ArrayList<Line> tableCandidate) {
		int height = -1, width = -1;
		int topY = -1, bottomY = -1, leftX = -1, rightX = -1;

		//Find maximum dimensions of table candidate
		for (Line l : tableCandidate) {
			if (l.getY1() < topY || topY == -1)
				topY = l.getY1();
			if (l.getY2() > bottomY || bottomY == -1)
				bottomY = l.getY2();
			if (l.getX1() < leftX || leftX == -1)
				leftX = l.getX1();
			if (l.getX2() > rightX || rightX == -1)
				rightX = l.getX2();
		}

		//Set height and width and initialize the array
		//Each element in the array represents a pixel in our table candidate rectangle
		height = bottomY - topY;
		width = rightX - leftX;
		boolean[][] map = new boolean[height][width];

		//Set values
		for (Line l : tableCandidate) {
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


	private double checkForSeparation(WordBlock b){
		//Create lines
		ArrayList<Line> lines = PageOperations.createLinesOfPage(b.getPage().getAllWordBlocks(SpatialOrdering.MIXED_MODE));

		//Find the line containing b
		Line thisLine = null;
		for(Line l : lines)
				if(l.getWordBlocks().contains(b))
					thisLine = l;
		if(thisLine == null){
			//FIXME: ERROR
			//Could not determine the Line of a WordBlock. Mixed Model usage?
			return 0.0;
		}

		//take the gaps of the line
		ArrayList<Gap> thisLineGaps = thisLine.getGaps();

		//Create Array of gaps
		int gapOccurrences[] = new int[b.getPage().getPageBoxWidth()];

		//For each gap raise counter
		for (Gap g : thisLineGaps) {
			for (int i = g.getGlobalBeginning(); i <= g.getGlobalEnd(); i++)
				gapOccurrences[i]++;
		}

		//Counter for the lines checked upwards
		int aboveCounter = 0;

		boolean newGapFound = false;
		Line aboveLine = thisLine;
		while(newGapFound || aboveCounter == 0) {
			//Take line above
			aboveLine = PageOperations.getLineInDirOf("UP", aboveLine, lines);

			if(aboveLine == null)
				break;

			//Take it's gaps
			ArrayList<Gap> aboveGaps = aboveLine.getGaps();

			//for each gap in the line raise the counter
			for (Gap g : aboveGaps) {
				for (int i = g.getGlobalBeginning(); i < g.getGlobalEnd(); i++)
					gapOccurrences[i]++;
			}

			newGapFound = false;
			//Check whether we found new gap overlaps
			for (int i = 0; i < b.getPage().getPageBoxWidth(); i++) {
				if (gapOccurrences[i] == aboveCounter)
					newGapFound = true;
			}
			if (newGapFound)
				aboveCounter++;
			//Repeat check with next line above
		}
		//Abort search upwards

		//Continue search downwards
		//Create Array for gap occurrences
		int gapOccurrencesBelow[] = new int[b.getPage().getPageBoxWidth()];

		//For each gap in this line raise counter by 1
		for (Gap g : thisLineGaps) {
			for (int i = g.getGlobalBeginning(); i < g.getGlobalEnd(); i++)
				gapOccurrencesBelow[i]++;
		}

		//Count the lines checked downwards
		int belowCounter = 0;

		newGapFound = false;
		Line belowLine = thisLine;
		while(newGapFound || belowCounter == 0) {
			//Take the line below
			belowLine = PageOperations.getLineInDirOf("DOWN", belowLine, lines);

			if(belowLine == null)
				break;

			//Take the gaps of the line below
			ArrayList<Gap> belowGaps = belowLine.getGaps();

			//For each of those gaps raise the counter by 1
			for (Gap g : belowGaps) {
				for (int i = g.getGlobalBeginning(); i < g.getGlobalEnd(); i++)
					gapOccurrencesBelow[i]++;
			}

			//Check whether we found new gap overlaps
			for (int i = 0; i < b.getPage().getPageBoxWidth(); i++) {
				if (gapOccurrencesBelow[i] == aboveCounter)
					newGapFound = true;
			}
			if (newGapFound)
				belowCounter++;
			//Repeat with next line below
		}

		//Table Probability
		double pTable = 0.0;

		//Evaluate!
		if(aboveCounter > 0 || belowCounter > 0){
			//Might be a table!
			if(aboveCounter > 0){
				pTable += 0.1;
			}
			if(belowCounter > 0){
				pTable += 0.1;
			}

			//Check above for column count
			int gapOccurrencesReduced[] = new int[gapOccurrences.length];
			for(int i = 0; i < gapOccurrences.length; i++){
				if(gapOccurrences[i] - aboveCounter < 0)
					gapOccurrencesReduced[i] = 0;
				else
					gapOccurrencesReduced[i] = 1;
			}

			int sumColumnsAbove = 0;
			for(int i : gapOccurrencesReduced){
				sumColumnsAbove += i;
			}

			//Check below for column count
			gapOccurrencesReduced = new int[gapOccurrences.length];
			for(int i = 0; i < gapOccurrences.length; i++){
				if(gapOccurrences[i] - aboveCounter < 0)
					gapOccurrencesReduced[i] = 0;
				else
					gapOccurrencesReduced[i] = 1;
			}

			int sumColumnsBelow = 0;
			for(int i : gapOccurrencesReduced){
				sumColumnsBelow += i;
			}

			//Set Table probability
			if(sumColumnsAbove >= 2)
				pTable += 0.2;
			else if(sumColumnsAbove > 1)
				pTable += 0.1;
			if(sumColumnsBelow >= 2)
				pTable += 0.2;
			else if(sumColumnsBelow > 1)
				pTable += 0.1;
			if(sumColumnsAbove == sumColumnsBelow && sumColumnsAbove != 0)
				pTable += 0.2;

		}

		return pTable;
	}
}