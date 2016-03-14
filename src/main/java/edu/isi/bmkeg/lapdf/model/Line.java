package edu.isi.bmkeg.lapdf.model;

import java.util.ArrayList;

/**
 * @author Maximilian Schirm (denkbares Gmbh), 9.3.2016
 * Class for easier management of collisions and other line specific operations.
 */
public class Line{
    int ytop, ybottom, xleft, xright;
    ArrayList<WordBlock> wordBlocks;
    ArrayList<Gap> gaps = new ArrayList<>();

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

    public void addGap(Gap gap){
        if(!gaps.contains(gap))
            gaps.add(gap);
    }

    public ArrayList<Gap> getGaps(){
        return gaps;
    }

    public void setWordBlocks(ArrayList<WordBlock> wordBlocks){
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

    public int getX1(){
        return xleft;
    }

    public int getX2(){
        return xright;
    }

    public int getY1(){
        return ytop;
    }

    public int getY2(){
        return ybottom;
    }

    public int getHeight(){
        return getY2() - getY1();
    }

    public int getWidth(){
        return getX2() - getX1();
    }

    public String getWord(){
        ArrayList<WordBlock> wordsSorted = new ArrayList<>(wordBlocks);
        String returner = "";
        for(WordBlock w : wordsSorted)
            returner += w.getWord();
        return returner;
    }

    public String toString(){
        String returner = "";
        for(WordBlock b : wordBlocks)
            returner += b.getWord();
        return returner;
    }
}