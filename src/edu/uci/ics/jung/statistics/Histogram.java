/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.statistics;

import cern.colt.list.DoubleArrayList;
import edu.uci.ics.jung.utils.NumericalPrecision;

/**
 * General-purpose class for representing experimental distributions via a histogram.
 * A histogram is primarily characterized by three things: <br>
 * 1) the minimum value in the data, minX <br>
 * 2) the bin width, w<br>
 * 3) the number of bins, n <br>                  *
 * The ith bin represents the interval [minX + (i-1)w, minX + i*w]. Each bin contains the
 * number of times a value in the original data set falls within its corresponding interval
 * @author Didier H. Besset (modified by Scott White)
 */
public class Histogram {
	/**
	 * Lower limit of first histogram bin.
	 */
	private double minimum;
	/**
	 * Width of a bin.
	 */
	private double binWidth;
	/**
	 * Histogram contents.
	 */
	private int[] contents;
	/**
	 * Flag to allow automatical growth.
	 */
	private boolean growthAllowed = false;
	/**
	 * Flag to enforce integer bin width.
	 */
	private boolean integerBinWidth = false;
	/**
	 * Counts of values located below first bin.
	 * Note: also used to count cached values when caching is in effect.
	 */
	private int underflow;
	/**
	 * Counts of values located above last bin.
	 * Note: also used to hold desired number of bins when caching is in effect.
	 */
	private int overflow;
	/**
	 * Statistical moments of values accumulated within the histogram limits.
	 */
	private StatisticalMoments moments;
	/**
	 * Flag indicating the histogram is caching values to compute adequate range.
	 */
	private boolean cached = false;
	/**
	 * Cache for accumulated values.
	 */
	private double cache[];

    //private DoubleArrayList values;


	/**
	 * Constructor method with unknown limits and a desired number
	 * of 50 bins. The first 100 accumulated values are cached.
	 * Then, a suitable range is computed.
	 */
	public Histogram() {
		this(100);
	}

	/**
	 * Constructor method for approximate range for a desired number
	 * of 50 bins.
	 * All parameters are adjusted so that the bin width is a round number.
	 * @param from approximate lower limit of first histogram bin.
	 * @param to approximate upper limit of last histogram bin.
	 */
	public Histogram(double from, double to) {
		this(from, to, 50);
	}

	/**
	 * Constructor method for approximate range and desired number of bins.
	 * All parameters are adjusted so that the bin width is a round number.
	 * @param from approximate lower limit of first histogram bin.
	 * @param to approximate upper limit of last histogram bin.
	 * @param bins desired number of bins.
	 */
	public Histogram(double from, double to, int bins) {
		defineParameters(from, to, bins);
	}

	/**
	 * Constructor method with unknown limits and a desired number of
	 * 50 bins.
	 * Accumulated values are first cached. When the cache is full,
	 * a suitable range is computed.
	 * @param n size of cache.
	 */
	public Histogram(int n) {
		this(n, 50);
	}

	/**
	 * General constructor method.
	 * @param n number of bins.
	 * @param min lower limit of first histogram bin.
	 * @param width bin width (must be positive).
	 * @exception java.lang.IllegalArgumentException
	 *							if the number of bins is non-positive,
	 *							if the limits are inversed.
	 */
	public Histogram(int n, double min, double width)
			throws IllegalArgumentException {
		if (width <= 0)
			throw new IllegalArgumentException(
					"Non-positive bin width: " + width);
		contents = new int[n];
		minimum = min;
		binWidth = width;
		reset();
	}

	/**
	 * Constructor method with unknown limits.
	 * Accumulated values are first cached. When the cache is full,
	 * a suitable range is computed.
	 * @param n size of cache.
	 * @param m desired number of bins
	 */
	public Histogram(int n, int m) {
		cached = true;
		cache = new double[n];
		underflow = 0;
		overflow = m;
	}

    /**
     * Fills the histogram with the list of random values
     * @param list a list of double values
     */
    public void fill(DoubleArrayList list) {
        for (int i=0; i<list.size(); i++) {
            fill(list.get(i));
        }
    }

	/**
	 * Fills with a random variable.
	 * @param x value of the random variable.
	 */
	public void fill(double x) {
		if (cached) {
			cache[underflow++] = x;
			if (underflow == cache.length)
				flushCache();
		} else if (x < minimum) {
			if (growthAllowed) {
				expandDown(x);
				moments.accumulate(x);
			} else
				underflow++;
		} else {
			int index = binIndex(x);
			if (index < contents.length) {
				contents[index]++;
				moments.accumulate(x);
			} else if (growthAllowed) {
				expandUp(x);
				moments.accumulate(x);
			} else
				overflow++;
		}
        //values.add(x);
	}

	/**
	 * Returns the average of the values accumulated in the histogram bins.
	 * @return average.
	 */
	public double average() {
		if (cached)
			flushCache();
		return moments.average();
	}

	/**
	 * @return int	index of the bin where x is located
	 * @param x double
	 */
	public int binIndex(double x) {
		return (int) Math.floor((x - minimum) / binWidth);
	}

	/**
	 * Returns the number of accumulated counts.
	 * @return number of counts.
	 */
	public long count() {
		return cached ? underflow : moments.count();
	}

	/**
	 * Compute suitable limits and bin width.
	 * @param from approximate lower limit of first histogram bin.
	 * @param to approximate upper limit of last histogram bin.
	 * @param bins desired number of bins.
	 * @exception java.lang.IllegalArgumentException
	 *							if the number of bins is non-positive,
	 *							if the limits are inversed.
	 */
	private void defineParameters(double from, double to, int bins)
			throws IllegalArgumentException {
		if (from >= to)
			throw new IllegalArgumentException(
					"Inverted range: minimum = " + from + ", maximum = " + to);
		if (bins < 1)
			throw new IllegalArgumentException(
					"Non-positive number of bins: " + bins);
		binWidth = NumericalPrecision.roundToScale((to - from) / bins,
				integerBinWidth);
		minimum = binWidth * Math.floor(from / binWidth);
		int numberOfBins = (int) Math.ceil((to - minimum) / binWidth);
		if (minimum + numberOfBins * binWidth <= to)
			numberOfBins++;
		contents = new int[numberOfBins];
        //values = new DoubleArrayList();
		cached = false;
		cache = null;
		reset();
	}

	/**
	 * Returns the error on average. May throw divide by zero exception.
	 * @return error on average.
	 */
	public double errorOnAverage() {
		if (cached)
			flushCache();
		return moments.errorOnAverage();
	}

	/**
	 * Expand the contents so that the lowest bin include the specified
	 *																value.
	 * @param x value to be included.
	 */
	private void expandDown(double x) {
		int addSize = (int) Math.ceil((minimum - x) / binWidth);
		int newContents[] = new int[addSize + contents.length];
		minimum -= addSize * binWidth;
		int n;
		newContents[0] = 1;
		for (n = 1; n < addSize; n++)
			newContents[n] = 0;
		for (n = 0; n < contents.length; n++)
			newContents[n + addSize] = contents[n];
		contents = newContents;
	}

	/**
	 * Expand the contents so that the highest bin include the specified
	 *																value.
	 * @param x value to be included.
	 */
	private void expandUp(double x) {
		int newSize = (int) Math.ceil((x - minimum) / binWidth);
		int newContents[] = new int[newSize];
		int n;
		for (n = 0; n < contents.length; n++)
			newContents[n] = contents[n];
		for (n = contents.length; n < newSize - 1; n++)
			newContents[n] = 0;
		newContents[n] = 1;
		contents = newContents;
	}

	/**
	 * Flush the values from the cache.
	 */
	private void flushCache() {
		double min = cache[0];
		double max = min;
		int cacheSize = underflow;
		double[] cachedValues = cache;
		int n;
		for (n = 1; n < cacheSize; n++) {
			if (cache[n] < min)
				min = cache[n];
			else if (cache[n] > max)
				max = cache[n];
		}
		defineParameters(min, max, overflow);
		for (n = 0; n < cacheSize; n++)
			fill(cachedValues[n]);
	}

	/**
     * retrieves the bin given a random value and returns the corresponding height of the bin
     * @param x the random value
     * @return total height of the corresponding bin
     */
	public double binHeight(double x) {
		if (x < minimum)
			return Double.NaN;
		int n = binIndex(x);
		return n < contents.length ? yValueAt(n) : Double.NaN;
	}

	/**
	 * Returns the low and high limits and the content of the bin
	 * containing the specified number or nul if the specified number
	 * lies outside of the histogram limits.
	 * @return a 3-dimensional array containing the bin limits and
	 *													the bin content.
	 */
	public double[] getBinParameters(double x) {
		if (x >= minimum) {
			int index = (int) Math.floor((x - minimum) / binWidth);
			if (index < contents.length) {
				double[] answer = new double[3];
				answer[0] = minimum + index * binWidth;
				answer[1] = answer[0] + binWidth;
				answer[2] = contents[index];
				return answer;
			}
		}
		return null;
	}

	/**
	 * Returns the bin width.
	 * @return bin width.
	 */
	public double getBinWidth() {
		return binWidth;
	}

	/**
	 * @return double
	 * @param x double
	 * @param y double
	 */
	public double getCountsBetween(double x, double y) {
		int n = binIndex(x);
		int m = binIndex(y);
		double sum = contents[n] * ((minimum - x) / binWidth - (n + 1))
				+ contents[m] * ((y - minimum) / binWidth - m);
		while (++n < m)
			sum += contents[n];
		return sum;
	}

	/**
	 * @return double integrated count up to x
	 * @param x double
	 */
	public double getCountsUpTo(double x) {
		int n = binIndex(x);
		double sum = contents[n] * ((x - minimum) / binWidth - n)
				+ underflow;
		for (int i = 0; i < n; i++)
			sum += contents[i];
		return sum;
	}

	/**
	 * Returns the number of bins of the histogram.
	 * @return number of bins.
     * @deprecated use getNumBins
	 */
	public double getDimension() {
		if (cached)
			flushCache();
		return contents.length;
	}

    public int getNumBins() {
		if (cached)
			flushCache();
		return contents.length;
	}

	/**
	 * @return double
	 */
	public double getMaximum() {
		return minimum + (contents.length - 1) * binWidth;
	}

	/**
	 * Returns the lower bin limit of the first bin.
	 * @return minimum histogram range.
	 */
	public double getMinimum() {
		return minimum;
	}

	/**
	 * Returns the range of values to be plotted.
	 * @return An array of 4 double values as follows
	 * index 0: minimum of X range
	 *       1: maximum of X range
	 *       2: minimum of Y range
	 *       3: maximum of Y range
	 */
	public double[] getRange() {
		if (cached)
			flushCache();
		double[] range = new double[4];
		range[0] = minimum;
		range[1] = getMaximum();
		range[2] = 0;
		range[3] = 0;
		for (int n = 0; n < contents.length; n++)
			range[3] = Math.max(range[3], contents[n]);
		return range;
	}

	/**
	 * Returns the kurtosis of the values accumulated in the histogram bins.
	 * The kurtosis measures the sharpness of the distribution near the maximum.
	 * Note: The kurtosis of the Normal distribution is 0 by definition.
	 * @return double kurtosis.
	 */
	public double kurtosis() {
		if (cached)
			flushCache();
		return moments.kurtosis();
	}

	/**
	 * @return FixedStatisticalMoments
	 */
	protected StatisticalMoments moments() {
		return moments;
	}

	/**
	 * Returns the number of counts accumulated below the lowest bin.
	 * @return overflow.
	 */
	public long overflow() {
		return cached ? 0 : overflow;
	}

	/**
	 * Reset histogram.
	 */
	public void reset() {
		if (moments == null)
			moments = new StatisticalMoments();
		else
			moments.reset();
		underflow = 0;
		overflow = 0;
		for (int n = 0; n < contents.length; n++)
			contents[n] = 0;
	}

	/**
	 * Allows histogram contents to grow in order to contain all
	 *											accumulated values.
	 * Note: Should not be called after counts have been accumulated in
	 * the underflow and/or overflow of the histogram.
	 * @exception java.lang.RuntimeException
	 *								if the histogram has some contents.
	 */
	public void setGrowthAllowed() throws RuntimeException {
		if (underflow != 0 || overflow != 0) {
			if (!cached)
				throw new RuntimeException(
						"Cannot allow growth to a non-empty histogram");
		}
		growthAllowed = true;
	}

	/**
	 * Forces the bin width of the histogram to be integer.
	 * Note: Can only be called when the histogram is cached.
	 * @exception java.lang.RuntimeException
	 *								if the histogram has some contents.
	 */
	public void setIntegerBinWidth() throws RuntimeException {
		if (!cached)
			throw new RuntimeException(
					"Cannot change bin width of a non-empty histogram");
		integerBinWidth = true;
	}

	/**
	 * Returns the number of points in the series.
	 */
	public int size() {
		if (cached)
			flushCache();
		return contents.length;
	}

	/**
	 * Returns the skewness of the values accumulated in the histogram bins.
	 * @return double skewness.
	 */
	public double skewness() {
		if (cached)
			flushCache();
		return moments.skewness();
	}

	/**
	 * Returns the standard deviation of the values accumulated in the histogram bins.
	 * @return double standard deviation.
	 */
	public double standardDeviation() {
		if (cached)
			flushCache();
		return moments.standardDeviation();
	}

	/**
	 * @return long
	 */
	public long totalCount() {
		return cached ? underflow
				: moments.count() + overflow + underflow;
	}

	/**
	 * Returns the number of counts accumulated below the lowest bin.
	 * @return underflow.
	 */
	public long underflow() {
		return cached ? 0 : underflow;
	}

	/**
	 * Returns the variance of the values accumulated in the histogram bins.
	 * @return double variance.
	 */
	public double variance() {
		if (cached)
			flushCache();
		return moments.variance();
	}

	/**
	 * Returns the end of the bin at the specified index.
	 * @param index the index of the bin.
	 * @return middle of bin
	 */
	public double xValueAt(int index) {
		return (index) * binWidth + minimum;
	}

	/**
	 * Returns the content of the bin at the given index.
	 * @param index the index of the bin.
	 * @return bin content
	 */
	public double yValueAt(int index) {
		if (cached)
			flushCache();
		return (index >= 0 && index < contents.length) ? (double) contents[index] : 0;
	}
}