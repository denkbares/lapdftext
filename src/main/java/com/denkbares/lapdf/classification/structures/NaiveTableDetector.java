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
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

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

	//Class for easier management of collisions and other line specific operations.
	class Line{
		int ytop, ybottom, xleft, xright;
		ArrayList<WordBlock> wordBlocks;
		ArrayList<Gap> gaps = new ArrayList<>();
		double partOfTable = 0;

		String UP="UP", DOWN="DOWN", LEFT="LEFT", RIGHT="RIGHT";

		public Line(int ytop,int ybottom,int xleft,int xright){
			this.ytop = ytop;
			this.ybottom = ybottom;
			this.xleft = xleft;
			this.xright = xright;
		}

		public Line(ArrayList<WordBlock> words){
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
			ytop = minY;
			ybottom = maxY;
			xleft = minX;
			xright = maxX;
			wordBlocks = words;
		}

		protected void setPartOfTable(double d){
			partOfTable = d;
		}

		public double getPartOfTable(){
			return partOfTable;
		}

		protected void addGap(Gap gap){
			if(!gaps.contains(gap))
				gaps.add(gap);
		}

		public ArrayList<Gap> getGaps(){
			return gaps;
		}

		protected void setWordBlocks(ArrayList<WordBlock> wordBlocks){
			this.wordBlocks = wordBlocks;
		}

		public ArrayList<WordBlock> getWordBlocks(){
			return wordBlocks;
		}

		public boolean collidesWith(Line l){
			//TODO Correct?!
			if(ytop >= l.ybottom && l.ytop >= ybottom)
				if(xleft < l.xright)
					return true;
			return false;
		}

		public double distanceTo(Line line){
			return distanceTo(line, "");
		}

		public double distanceTo(Line line, String direction){
			if(line == null)
				return -1;
			switch (direction){
				case "UP":
					return ytop - line.ybottom;
				case "DOWN":
					return line.ytop - ybottom;
				case "LEFT":
					return xleft - line.xright;
				case "RIGHT":
					return line.xleft - xright;
				default:
					double centerX = (xright-xleft)/2, centerY = (ybottom-ytop)/2;
					double centerLineX = (line.xright-line.xleft)/2, centerLineY = (line.ytop-ybottom)/2;
					return Math.sqrt(Math.pow(centerX - centerLineX, 2) + Math.pow(centerY - centerLineY, 2));
			}
		}

		int getX1(){
			return xleft;
		}

		int getX2(){
			return xright;
		}

		int getY1(){
			return ytop;
		}

		int getY2(){
			return ybottom;
		}

		public String toString(){
			String returner = "";
			for(WordBlock b : wordBlocks)
				returner += b.getWord();
			return returner;
		}
	}

	//Gaps in a line or word
	class Gap{
		int beginning, end;

		public Gap(int beginning, int end){
			this.beginning = beginning;
			this.end = end;
		}

		boolean doesOverlap(Gap g){
			boolean a = beginning < g.beginning && end > g.beginning;
			boolean b = beginning > g.beginning && beginning < g.end;
			boolean c = beginning == g.beginning;
			boolean d = end == g.end;
			boolean e = end < g.end && end > g.beginning;
			if(a || b || c || d || e)
				return true;
			return false;
		}

		public int getBeginning(){
			return beginning;
		}

		public int getEnd(){
			return end;
		}

		@Override
		public boolean equals(Object o){
			if(o == null)
				return false;
			if(o.getClass() != Gap.class)
				return false;
			Gap temp = ((Gap)o);
			if((temp.getBeginning() == getBeginning()) && (temp.getEnd() == getEnd())) {
				return true;
			}
			return false;
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

	//TODO tweak
	protected double method2(ChunkBlock block, ChunkFeatures features){
		//STEP 1 : Create Lines from WordBlocks of Page
		ArrayList<Line> lines = createLinesOfDocument(block);
		//STEP 2 : Select adjacent Lines that are further apart than the page's average line distance (w/o outliers)
		//TODO!
		ArrayList<ArrayList<Line>> potentialTables=  new ArrayList<ArrayList<Line>>();
		for(Line line : lines){
				//Two checks :
				// 1. Does this line have a line above it, with the distance to that line being greater than avg?
				// 2. Does this line have a line below it, with the distance to that line being greater than avg?
				Line lineAbove = getLineInDirOf("UP", line, lines);
				Line lineBelow = getLineInDirOf("DOWN", line, lines);
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
							lineAbove = getLineInDirOf("UP", lineAbove, lines);
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
					lineBelow = getLineInDirOf("DOWN", topmostLine, lines);
					distBelow = topmostLine.distanceTo(lineBelow, "DOWN");
					while(lineBelow != null && distBelow == originalDist){
						tableCandidate.add(lineBelow);
						Line oldLineBelow = lineBelow;
						lineBelow = getLineInDirOf("DOWN", lineBelow, lines);
						distBelow = oldLineBelow.distanceTo(lineBelow, "DOWN");
					}
					//tableCandidate now contains all our suspected table lines downwards. TODO Doesn't add next one that was originally referenced!
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
					line.setPartOfTable(tableEvidence);
				}
			}
			//Now we check for unusual horizontal margins. This includes spacing between WordBlocks as well as space characters.
			for(ArrayList<Line> tableCandidate : potentialTables){
				double gradeOfSeparation = getGradeOfSeparation(tableCandidate);
				for(Line line : tableCandidate){
					line.setPartOfTable(line.getPartOfTable() + gradeOfSeparation);
				}
			}

			//Finally, we add up the possibilities and get the Overlapping value for our ChunkBlock
			double returner = 0;
			for(ArrayList<Line> table : potentialTables){
				for(Line line : table){
					returner += getOverlap(line,block) * line.getPartOfTable();
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

	//Creates a boolean array with a cell for every pixel of the TableCandidate
	//The first index indicates the line of the TC, the second index indicates the pixel
	//True means that this position is empty (i.e. space char / space between WordBlocks)
	//False means that this position is filled with a char
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

	//This method generates Lines from the document. Lines are used for all the further processing steps.
	private ArrayList<Line> createLinesOfDocument(ChunkBlock block) {
		ArrayList<WordBlock> mixedWords = (ArrayList<WordBlock>) block.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);
		final ArrayList<WordBlock> originalWords = (ArrayList<WordBlock>) block.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE);
		ArrayList<Line> lines = new ArrayList<>();
		for(WordBlock w : originalWords){
			if(mixedWords.contains(w)){
				//Find leftmost WordBlock in line
				WordBlock lineStart = w;
				while(getWordBlockInDirOf("LEFT", lineStart, mixedWords) != null){
					lineStart = getWordBlockInDirOf("LEFT", lineStart, mixedWords);
				}
				//lineStart is now the leftmost WordBlock in line
				//Go through all blocks to the right now and add them progressively to a new line
				//Remove blocks that are in a line from mixedWords, so that any WordBlock is always on just one line
				ArrayList<WordBlock> tempLineWordBlocks = new ArrayList<>();
				tempLineWordBlocks.add(lineStart);
				while(getWordBlockInDirOf("RIGHT", lineStart, mixedWords) != null){
					lineStart = getWordBlockInDirOf("RIGHT", lineStart, mixedWords);
					tempLineWordBlocks.add(lineStart);
					mixedWords.remove(lineStart);
				}

				//Build a new line and save it
				lines.add(new Line(tempLineWordBlocks));
			}
		}
		return lines;
	}

	//This method returns the next WordBlock in the given direction from the WordBlock passed in the argument
	WordBlock getWordBlockInDirOf(String direction, WordBlock from){
		return getWordBlockInDirOf(direction, from, (ArrayList<WordBlock>)from.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE));
	}

	//This method returns the next WordBlock in the given direction from the WordBlock passed in the argument,
	//looking only in the passed List of blocks
	WordBlock getWordBlockInDirOf(String direction, WordBlock from, ArrayList<WordBlock> blocks){
		double minDistance = -1;
		WordBlock returner = null;
		double centerX = from.getX1() + ((from.getX2() - from.getX1()) / 2), centerY = from.getY1() + ((from.getY2() - from.getY1()) / 2);

		for(WordBlock wb : blocks){
			if(!wb.equals(from)) {
				//Ensure that they are on the same line before proceeding
				if(centerY < wb.getY2() && centerY > wb.getY1()) {
					double tempResult;
					switch (direction) {
						case "UP":
							tempResult = from.getY2() - wb.getY1();
							if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
								minDistance = tempResult;
								returner = wb;
							}
							break;
						case "DOWN":
							tempResult = wb.getY2() - from.getY1();
							if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
								minDistance = tempResult;
								returner = wb;
							}
							break;
						case "LEFT":
							tempResult = from.getX1() - wb.getX2();
							if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
								minDistance = tempResult;
								returner = wb;
							}
							break;
						case "RIGHT":
							tempResult = wb.getX1() - from.getX2();
							if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
								minDistance = tempResult;
								returner = wb;
							}
							break;
						default:
							double centerLineX = wb.getX1() + ((wb.getX2() - wb.getX1()) / 2), centerLineY = wb.getY1() + ((wb.getY2() - wb.getY1()) / 2);
							double distance = Math.sqrt(Math.pow(centerX - centerLineX, 2) + Math.pow(centerY - centerLineY, 2));
							if ((minDistance == -1 || distance < minDistance) && distance >= 0) {
								minDistance = distance;
								returner = wb;
							}
							break;
					}
				}
			}
		}
		return returner;
	}

	//This method returns the next Line in the given direction, coming from the current line
	Line getLineInDirOf(String direction, Line from, ArrayList<Line> pageLines){
		double minDistance = -1;
		Line returner = null;
		double centerX = from.getX1() + ((from.getX2() - from.getX1()) / 2), centerY = from.getY1() + ((from.getY2() - from.getY1()) / 2);

		for(Line line : pageLines){
			if(!line.equals(from)) {
				double tempResult;
				switch (direction) {
					case "UP":
						tempResult = from.getY1() - line.getY2();
						if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
							minDistance = tempResult;
							returner = line;
						}
						break;
					case "DOWN":
						tempResult = line.getY1() - from.getY2();
						if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
							minDistance = tempResult;
							returner = line;
						}
						break;
					case "LEFT":
						tempResult = from.getX1() - line.getX2();
						if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
							minDistance = tempResult;
							returner = line;
						}
						break;
					case "RIGHT":
						tempResult = line.getX1() - from.getX2();
						if ((minDistance == -1 || tempResult < minDistance) && tempResult >= 0) {
							minDistance = tempResult;
							returner = line;
						}
						break;
					default:
						double centerLineX = line.getX1() + ((line.getX2() - line.getX1()) / 2), centerLineY = line.getY1() + ((line.getY2() - line.getY1()) / 2);
						double distance = Math.sqrt(Math.pow(centerX - centerLineX, 2) + Math.pow(centerY - centerLineY, 2));
						if ((minDistance == -1 || distance < minDistance) && distance >= 0) {
							minDistance = distance;
							returner = line;
						}
						break;
				}
			}
		}
		return returner;
	}
}
