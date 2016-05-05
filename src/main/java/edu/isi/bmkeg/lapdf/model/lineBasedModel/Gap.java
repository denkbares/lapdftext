package edu.isi.bmkeg.lapdf.model.lineBasedModel;

/**
 * @author Maximilian Schirm (denkbares GmbH), 9.3.2016
 * Gaps in a line or word
 */
public class Gap{
    private int beginning, end;
    private int globalCorrectionFactor;

    public Gap(int beginning, int end){
        this.beginning = beginning;
        this.end = end;
    }

    //Gaps are solely 2D, as 2 dimensional checking suffices for checking the Gap-Overlap on
    //multiple lines as well as in a single line.
    public boolean doesOverlap(Gap g){
        int gBeg, gEnd;

        gBeg = g.getGlobalBeginning();
        gEnd = g.getGlobalEnd();

        boolean a = getGlobalBeginning()< gBeg&& getGlobalEnd() > gBeg;
        boolean b = getGlobalEnd() > gBeg&& getGlobalBeginning()< gEnd;
        boolean c = getGlobalBeginning()== gBeg;
        boolean d = getGlobalEnd() == gEnd;
        boolean e = getGlobalEnd() < gEnd&& getGlobalEnd() > gBeg;

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

    public int getWidth(){
        return getEnd() - getBeginning();
    }

    public int getGlobalBeginning(){
        return beginning + globalCorrectionFactor;
    }

    public int getGlobalEnd(){
        return end + globalCorrectionFactor;
    }

    public int getGlobalCorrectionFactor() {
        return globalCorrectionFactor;
    }

    public void setGlobalCorrectionFactor(int globalCorrectionFactor) {
        this.globalCorrectionFactor = globalCorrectionFactor;
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