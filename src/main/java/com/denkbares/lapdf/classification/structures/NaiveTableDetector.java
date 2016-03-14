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
import edu.isi.bmkeg.lapdf.model.Gap;
import edu.isi.bmkeg.lapdf.model.Line;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.utils.PageOperations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Sebastian Furth (denkbares GmbH), Maximilian Schirm
 * @created 19.08.15
 */
public class NaiveTableDetector implements StructureDetector {

	//set int method to preferred detection algorithm
	int method = 0;

	@Override
	public double classify(ChunkBlock block, ChunkFeatures features) {

		switch (method){
			case 0 :
				return method1(block, features);
			case 1:
				return method2(block, features);
			default:
				return method1(block, features);
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
		//STEP 1 : Create Lines from WordBlocks of Page
		ArrayList<Line> lines = createLinesOfDocument(block);
		//STEP 2 : Select adjacent Lines that are further apart than the page's average line distance (w/o outliers) (TODO)
		Map<Line, Double> lineTableEvidenceMap = new HashMap<Line, Double>();
		ArrayList<ArrayList<Line>> potentialTables=  new ArrayList<ArrayList<Line>>();
		for(Line line : lines){
				//Two checks :
				// 1. Does this line have a line above it, with the distance to that line being greater than avg?
				// 2. Does this line have a line below it, with the distance to that line being greater than avg?
				Line lineAbove = PageOperations.getLineInDirOf("UP", line, lines);
				Line lineBelow = PageOperations.getLineInDirOf("DOWN", line, lines);
				//TODO Create better avgLineDistance method - exclude outliers
				double avgLineDist = block.getPage().getAvgLineDistance();
				double distAbove, distBelow = 0, originalDist = 0;
				Line topmostLine = lineAbove;
				boolean topmostLineFound = false;

				if(lineAbove != null){
					distAbove = line.distanceTo(lineAbove, "UP");
					originalDist = distAbove;
					if(distAbove < avgLineDist){
						//We might have a table candidate here.
						//Proceed checking upwards whether there are more lines with that exact distance.
						while (lineAbove != null && distAbove == originalDist){
							topmostLine = lineAbove;
							lineAbove = PageOperations.getLineInDirOf("UP", lineAbove, lines);
							distAbove = topmostLine.distanceTo(lineAbove, "UP");
						}
						//topmostLine is now the highest line of the potential table.
						topmostLineFound = true;
					}
				}
				if(lineBelow != null && !topmostLineFound){
					distBelow = line.distanceTo(lineBelow, "DOWN");
					originalDist = distBelow;
					if(distBelow < avgLineDist){
						//We might have a table candidate here.
						//Since we had no line above this one, assume this is the topmost line in the table.
						topmostLine = line;
						topmostLineFound = true;
					}
				}

				//If we found the topmost line of our possible table, collect lines with our original distance downwards.
				if(topmostLineFound) {
					ArrayList<Line> tableCandidate = new ArrayList<>();
					tableCandidate.add(topmostLine);
					lineBelow = PageOperations.getLineInDirOf("DOWN", topmostLine, lines);
					distBelow = topmostLine.distanceTo(lineBelow, "DOWN");
					while(lineBelow != null && distBelow == originalDist){
						tableCandidate.add(lineBelow);
						Line oldLineBelow = lineBelow;
						lineBelow = PageOperations.getLineInDirOf("DOWN", lineBelow, lines);
						distBelow = oldLineBelow.distanceTo(lineBelow, "DOWN");
					}
					//tableCandidate now contains all our suspected table lines downwards. TODO Doesn't add next one that was originally referenced!
					//TODO (2) : Does above TODO still hold?
					potentialTables.add(tableCandidate);
				}
		}
		//Remove any duplicates
		ArrayList<ArrayList<Line>> duplicateFree = new ArrayList<>();
		for(ArrayList<Line> candidate : potentialTables){
			if(!duplicateFree.contains(candidate)){
				duplicateFree.add(candidate);
			}
		}
		potentialTables = new ArrayList<>(duplicateFree);

		//Did we find any possible tables?
		if(potentialTables.size() > 1){
			//Check whether the found tables contain elements of the ChunkBlock
			double tableEvidence = 0.1;

			for(ArrayList<Line> table : potentialTables){
				for(Line line : table){
					lineTableEvidenceMap.put(line, new Double(tableEvidence));
				}
			}
			//Now we check for unusual horizontal margins. This includes spacing between WordBlocks as well as space characters.
			for(ArrayList<Line> tableCandidate : potentialTables){
				double gradeOfSeparation = getGradeOfSeparation(tableCandidate);
				for(Line line : tableCandidate){
					double oldValue = lineTableEvidenceMap.get(line).doubleValue();
					lineTableEvidenceMap.put(line,oldValue+gradeOfSeparation);
				}
			}

			//Finally, we add up the possibilities and get the Overlapping value for our ChunkBlock
			double returner = 0;
			for(ArrayList<Line> table : potentialTables){
				for(Line line : table){
					returner += getOverlap(line,block) * lineTableEvidenceMap.get(line).doubleValue();
				}
			}

			//The probability that block is part of a table
			return (returner > 1) ? 1.0 : returner;
		}
		else {
			//Did not find any tables.
			return 0;
		}
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

		//Set height and width to initialize the array
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
	//1.0 means we are absolutely sure, 0 means we think that the lines are not separated
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
					if (word.charAt(i) == ' ') {
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
			//Count how often a Gap occurred. TODO Invent wicked smart formula for balancing
			int count = overlapCounter.get(g).intValue();
			returner = returner + (1 /(double) overlapCounter.size()) * count;
		}

		//
		//TODO CONTINUE
		//


		return (returner > 1) ? 1 : returner;
	}

	//Returns a value (0.0 - 1.0) representing the percentage of overlapping between the ChunkBlock and the line
	private double getOverlap(Line line, ChunkBlock block) {
		ArrayList<WordBlock> lineblocks = line.getWordBlocks();
		double overlap = 0;
		for(WordBlock w : block.getWordBlocks()) {
			if(lineblocks.contains(w))
				overlap += 1/block.getWordBlocks().size();
		}
		return overlap;
	}

	//TODO check for feasibility of removal - added method to MaxPowerChunkingClass
	//This method generates Lines from the document. Lines are used for all the further processing steps.
	private ArrayList<Line> createLinesOfDocument(ChunkBlock block) {
		ArrayList<WordBlock> mixedWords = (ArrayList<WordBlock>) block.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);
		final ArrayList<WordBlock> originalWords = (ArrayList<WordBlock>) block.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);
		ArrayList<Line> lines = new ArrayList<>();
		for(WordBlock w : originalWords){
			if(mixedWords.contains(w)){
				//Find leftmost WordBlock in line
				WordBlock lineStart = w;
				while(PageOperations.getWordBlockInDirOf("LEFT", lineStart, mixedWords) != null){
					lineStart = PageOperations.getWordBlockInDirOf("LEFT", lineStart, mixedWords);
				}
				//lineStart is now the leftmost WordBlock in line
				//Go through all blocks to the right now and add them progressively to a new line
				//Remove blocks that are in a line from mixedWords, so that any WordBlock is always on just one line
				ArrayList<WordBlock> tempLineWordBlocks = new ArrayList<>();
				tempLineWordBlocks.add(lineStart);
				while(PageOperations.getWordBlockInDirOf("RIGHT", lineStart, mixedWords) != null){
					lineStart = PageOperations.getWordBlockInDirOf("RIGHT", lineStart, mixedWords);
					tempLineWordBlocks.add(lineStart);
					mixedWords.remove(lineStart);
				}

				//Build a new line and save it
				lines.add(new Line(tempLineWordBlocks));
			}
		}
		return lines;
	}

}
