package de.mpicbg.tds.knime.hcstools.utils;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math.util.ResizableDoubleArray;

import java.lang.reflect.InvocationTargetException;

/**
 * The class provides further statistics
 * Extensions:
 * - median absolute deviation
 * - median
 * <p/>
 * <p/>
 * Created by IntelliJ IDEA.
 * User: Antje Niederlein
 * Date: 2/20/12
 * Time: 10:48 AM
 */
public class ExtDescriptiveStats extends DescriptiveStatistics {

    /**
     * Median absolute deviation implementation - can be reset by setter.
     */
    private UnivariateStatistic madImpl = new MadStatistic();

    /**
     * Constructor
     */
    public ExtDescriptiveStats() {
        super();
    }

    /**
     * Contructor to provide a data array
     *
     * @param data
     */
    public ExtDescriptiveStats(ResizableDoubleArray data) {
        super.eDA = data;
    }

    /**
     * @return 50th percentile of the data; dependent on PercentileImpl
     */
    public double getMedian() {
        return getPercentile(50);
    }

    /**
     * @return median absolute deviation; dependent on PercentileImpl and MadImpl
     */
    public double getMad() {
        //calculate the median and set the value for the MAD calculation
        double median = getPercentile(50);
        if (madImpl instanceof MadStatistic) {
            ((MadStatistic) madImpl).setMedian(median);
        } else {
            try {
                madImpl.getClass().getMethod("setMedian", new Class[]{Double.TYPE}).invoke(madImpl,
                        new Object[]{median});
            } catch (NoSuchMethodException e1) { // Setter guard should prevent
                throw new IllegalArgumentException("Percentile implementation does not support setQuantile");
            } catch (IllegalAccessException e2) {
                throw new IllegalArgumentException("IllegalAccessException setting quantile");
            } catch (InvocationTargetException e3) {
                throw new IllegalArgumentException("Error setting quantile" + e3.toString());
            }
        }

        return apply(madImpl);
    }

    public void setMadImpl(UnivariateStatistic madImpl) {
        this.madImpl = madImpl;
    }


    public static void main(String[] args) {
        double[] vec = {-1.47367098, 2.33110135, -0.01785387, 0.04113220, -0.18595962,
                0.05292957, 1.50823067, 0.23152174, -0.08921781, 0.38920663, Double.NaN};

        ExtDescriptiveStats stats = new ExtDescriptiveStats();

        for (int i = 0; i < vec.length; i++)
            stats.addValue(vec[i]);

        double median = stats.getMedian();
        double mad = stats.getMad();

        System.out.println("Median: " + median);
        System.out.println("Mad: " + mad);
    }
}
