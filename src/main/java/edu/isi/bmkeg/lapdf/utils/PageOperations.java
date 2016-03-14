package edu.isi.bmkeg.lapdf.utils;

import edu.isi.bmkeg.lapdf.model.Line;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Maximilian Schirm (denkbares GmbH), 12.3.2016
 */
public abstract class PageOperations {

    //Directions in this class are UP, DOWN, LEFT, RIGHT.
    public final String UP="UP", DOWN="DOWN", LEFT="LEFT", RIGHT="RIGHT";


    /**
     * This method returns the next Line in the given direction, coming from the current line
     * @param direction The direction in which to look for lines (UP,DOWN,LEFT,RIGHT)
     * @param from The line from which out we will search
     * @param pageLines The lines in which we will look (usually all Lines of page)
     * @return The next line in the given direction
     */
    public static Line getLineInDirOf(String direction, Line from, ArrayList<Line> pageLines){
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

    /**
     * This method returns the next WordBlock in the given direction from the WordBlock passed in the argument,
     * looking through the WordBlocks of the PageBlock of the passed WordBlock.
     * @param direction The direction in which to look for WordBlocks (UP,DOWN,LEFT,RIGHT)
     * @param from The WordBlock from which out we will search
     * @return The next WordBlock in the given direction
     */
    public static WordBlock getWordBlockInDirOf(String direction, WordBlock from){
        return getWordBlockInDirOf(direction, from, (ArrayList<WordBlock>)from.getPage().getAllWordBlocks(SpatialOrdering.ORIGINAL_MODE));
    }

    /**
     * This method returns the next WordBlock in the given direction from the WordBlock passed in as an argument,
     * looking only in the passed list of WordBlocks
     * @param direction The direction in which to look for WordBlocks (UP,DOWN,LEFT,RIGHT)
     * @param from The WordBlock from which out we will search
     * @param blocks The WordBlocks in which we will look (usually all WordBlocks on the page/of the line)
     * @return The nextWordBlock in the given direction
     */
    public static WordBlock getWordBlockInDirOf(String direction, WordBlock from, ArrayList<WordBlock> blocks){
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

    /**
     * This method returns the average VERTICAL distance between the lines it was passed.
     * @param lines The lines of which we want to obtain the average distance
     * @return The average VERTICAL distance of the given lines.
     */
    public static double getAverageVerticalDistanceOfLines(ArrayList<Line> lines){
        //First we need to sort the lines from top to bottom descending.
        Collections.sort(lines, new Comparator<Line>() {
            @Override
            public int compare(Line o1, Line o2) {
                if (o1.getY1() < o2.getY1())
                    return -1;
                else if (o1.getY1() == o2.getY1())
                    return 0;
                else
                    return 1;
            }
        });
        //Then we will sum the distances of the first line to the last line recursively.
        double sumOfDistances = 0;
        for(Line line : lines){
            Line nextLine = getLineInDirOf("DOWN",line,lines);
            if(nextLine == null)
                break;
            sumOfDistances += line.distanceTo(nextLine,"DOWN");
        }
        //We return the sumOfDistances/lines.size.
        return sumOfDistances/lines.size();
    }


}
