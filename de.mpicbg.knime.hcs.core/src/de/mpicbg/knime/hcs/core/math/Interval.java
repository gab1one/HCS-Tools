package de.mpicbg.knime.hcs.core.math;


/**
 * implements a 1D interval
 * <p/>
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 2/27/12
 * Time: 10:00 AM
 */
public class Interval {

    // lower Bound
    double lowerBound;

    // upper Bound
    double upperBound;

    // Label
    String label;

    /* different modes
    INCL_LEFT   - [a,b) 	right half-open interval in ? from a (included) to b (excluded) 	[a,b[ = {x ? ? ? a � x < b}
    INCL_RIGHT  - (a,b] 	left half-open interval in ? from a (excluded) to b (included) 	]a,b] = {x ? ? ? a < x � b}
    INC_NONE    - (a,b) 	open interval in ? from a (excluded) to b (excluded) 	]a,b[ = {x ? ? ? a < x < b}
    INCL_BOTH   - [a,b] 	closed interval in ? from a (included) to b (included) 	[a,b] = {x ? ? ? a � x � b}
     */
    public enum Mode {
        INCL_LEFT, INCL_RIGHT, INCL_BOTH, INCL_NONE
    }

    public Interval(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public Interval(double lowerBound, double upperBound, String label) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.label = label;
    }


    /**
     * @param x        value to test
     * @param inclMode testing mode
     * @return true if x falls within the interval regarding the testing mode
     */
    public boolean contains(double x, Mode inclMode) {
        boolean checkLowerBound = false;
        boolean checkUpperBound = false;

        int compareLowerBound = Double.compare(lowerBound, x);
        int compareUpperBound = Double.compare(x, upperBound);

        if (compareLowerBound < 0) checkLowerBound = true;
        else if (compareLowerBound == 0 && (inclMode == Mode.INCL_LEFT || inclMode == Mode.INCL_BOTH))
            checkLowerBound = true;

        if (compareUpperBound < 0) checkUpperBound = true;
        else if (compareUpperBound == 0 && (inclMode == Mode.INCL_RIGHT || inclMode == Mode.INCL_BOTH))
            checkUpperBound = true;


        return checkLowerBound && checkUpperBound;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public static void main(String[] args) {
        //Interval iv = new Interval(1.0,2.0);
        Interval iv = new Interval(Double.NEGATIVE_INFINITY, 2.0);

        double[] testNumbers = {-1.0, 1.0, 1.5, 2.5, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        boolean ret;

        for (int i = 0; i < testNumbers.length; i++) {
            ret = iv.contains(testNumbers[i], Mode.INCL_BOTH);
            System.out.println("[] " + testNumbers[i] + ": " + ret);

            ret = iv.contains(testNumbers[i], Mode.INCL_NONE);
            System.out.println("() " + testNumbers[i] + ": " + ret);

            ret = iv.contains(testNumbers[i], Mode.INCL_LEFT);
            System.out.println("[) " + testNumbers[i] + ": " + ret);

            ret = iv.contains(testNumbers[i], Mode.INCL_RIGHT);
            System.out.println("(] " + testNumbers[i] + ": " + ret);
        }
    }

}