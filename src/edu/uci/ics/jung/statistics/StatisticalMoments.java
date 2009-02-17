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

/**
 * A data structure representing the central moments of a distribution including: <ul>
 * <li> the mean </li>
 * <li> the variance </li>
 * <li> the skewness</li>
 * <li> the kurtosis </li></ul> <br>
 * Data values that are observed are passed into this data structure via the accumulate(...) method
 * and the corresponding central moments are updated on each call
 *
 * @author Didier H. Besset (modified by Scott White)
 */
public class StatisticalMoments {
	/**
	 * Vector containing the points.
	 */
	protected double[] moments;

	/**
	 * Default constructor methods: declare space for 5 moments.
	 */
	public StatisticalMoments() {
		this(5);
	}

	/**
	 * General constructor methods.
	 * @param n number of moments to accumulate.
	 */
	public StatisticalMoments(int n) {
		moments = new double[n];
		reset();
	}

	/**
	 * statistical moment accumulation up to order 4.
	 * @param x double	value to accumulate
	 */
	public void accumulate(double x) {
		double n = moments[0];
		double n1 = n + 1;
		double n2 = n * n;
		double delta = (moments[1] - x) / n1;
		double d2 = delta * delta;
		double d3 = delta * d2;
		double r1 = (double) n / (double) n1;
		moments[4] += 4 * delta * moments[3] + 6 * d2 * moments[2]
				+ (1 + n * n2) * d2 * d2;
		moments[4] *= r1;
		moments[3] += 3 * delta * moments[2] + (1 - n2) * d3;
		moments[3] *= r1;
		moments[2] += (1 + n) * d2;
		moments[2] *= r1;
		moments[1] -= delta;
		moments[0] = n1;
		return;
	}

	/**
	 * @return double average.
	 */
	public double average() {
		return moments[1];
	}

	/**
	 * Returns the number of accumulated counts.
	 * @return number of counts.
	 */
	public long count() {
		return (long) moments[0];
	}

	/**
	 * Returns the error on average. May throw divide by zero exception.
	 * @return error on average.
	 */
	public double errorOnAverage() {
		return Math.sqrt(variance() / moments[0]);
	}

	/**
	 * The kurtosis measures the sharpness of the distribution near
	 *														the maximum.
	 * Note: The kurtosis of the Normal distribution is 0 by definition.
	 * @return double kurtosis or NaN.
	 */
	public double kurtosis() throws ArithmeticException {
		if (moments[0] < 4)
			return Double.NaN;
		double kFact = (moments[0] - 2) * (moments[0] - 3);
		double n1 = moments[0] - 1;
		double v = variance();
		return (moments[4] * moments[0] * moments[0] * (moments[0] + 1)
				/ (v * v * n1) - n1 * n1 * 3) / kFact;
	}

	/**
	 * Reset all counters.
	 */
	public void reset() {
		for (int n = 0; n < moments.length; n++)
			moments[n] = 0;
	}

	/**
	 * @return double skewness.
	 */
	public double skewness() throws ArithmeticException {
		if (moments[0] < 3)
			return Double.NaN;
		double v = variance();
		return moments[3] * moments[0] * moments[0]
				/ (Math.sqrt(v) * v * (moments[0] - 1)
				* (moments[0] - 2));
	}

	/**
	 * Returns the standard deviation. May throw divide by zero exception.
	 * @return double standard deviation.
	 */
	public double standardDeviation() {
		return Math.sqrt(variance());
	}

	/**
	 * @return double
	 */
	public double unnormalizedVariance() {
		return moments[2] * moments[0];
	}

	/**
	 * Note: the variance includes the Bessel correction factor.
	 * @return double variance.
	 */
	public double variance() throws ArithmeticException {
		if (moments[0] < 2)
			return Double.NaN;
		return unnormalizedVariance() / (moments[0] - 1);
	}
}