package edu.isi.bmkeg.lapdf.model.lineBasedModel;

/**
 * @author Maximilian Schirm (denkbares GmbH), 9.3.2016
 * Gaps in a line or word
 */
public class Gap{
    int beginning, end;

    public Gap(int beginning, int end){
        this.beginning = beginning;
        this.end = end;
    }

    //Gaps are solely 2D, as 2 dimensional checking suffices for checking the Gap-Overlap on
    //multiple lines as well as in a single line.
    public boolean doesOverlap(Gap g){
        boolean a = beginning < g.beginning && end > g.beginning;
        boolean b = end > g.beginning && beginning < g.end;
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