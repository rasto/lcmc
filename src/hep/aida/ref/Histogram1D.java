package hep.aida.ref;

import hep.aida.IAxis;
import hep.aida.IHistogram1D;
/**
A reference implementation of hep.aida.IHistogram1D.
The goal is to provide a clear implementation rather than the most efficient implementation.
However, performance seems fine - filling 1.2 * 10^6 points/sec, both using FixedAxis or VariableAxis.

@author Wolfgang Hoschek, Tony Johnson, and others.
@version 1.0, 23/03/2000
*/
public class Histogram1D extends AbstractHistogram1D implements IHistogram1D
{
	private double[] errors;
	private double[] heights;
	private int[] entries;
	private int nEntry; // total number of times fill called
	private double sumWeight; // Sum of all weights
	private double sumWeightSquared; // Sum of the squares of the weights
	private double mean, rms;
	/**
	 * Creates a variable-width histogram.
	 * Example: <tt>edges = (0.2, 1.0, 5.0)</tt> yields an axis with 2 in-range bins <tt>[0.2,1.0), [1.0,5.0)</tt> and 2 extra bins <tt>[-inf,0.2), [5.0,inf]</tt>.
	 * @param title The histogram title.
	 * @param edges the bin boundaries the axis shall have;
	 *        must be sorted ascending and must not contain multiple identical elements.
	 * @throws IllegalArgumentException if <tt>edges.length < 1</tt>.
	 */
	public Histogram1D(String title, double[] edges)
	{
		this(title,new VariableAxis(edges));
	}
	/**
	 * Creates a fixed-width histogram.
	 * 
	 * @param title The histogram title.
	 * @param bins The number of bins.
	 * @param min The minimum value on the X axis.
	 * @param max The maximum value on the X axis.
	 */
	public Histogram1D(String title, int bins, double min, double max)
	{
		this(title,new FixedAxis(bins,min,max));
	}
	/**
	 * Creates a histogram with the given axis binning.
	 * 
	 * @param title The histogram title.
	 * @param axis The axis description to be used for binning.
	 */
	public Histogram1D(String title, IAxis axis)
	{
		super(title);
		xAxis = axis;
		int bins = axis.bins();
		entries = new int[bins+2];
		heights = new double[bins+2];
		errors = new double[bins+2];
	}
	public int allEntries() // perhaps to be deleted (default impl. in superclass sufficient)
	{
		return nEntry;
	}
	public int binEntries(int index)
	{
		//return entries[xAxis.map(index)];
		return entries[map(index)];
	}
	public double binError(int index)
	{
		//return Math.sqrt(errors[xAxis.map(index)]);
		return Math.sqrt(errors[map(index)]);
	}
	public double binHeight(int index)
	{
		//return heights[xAxis.map(index)];
		return heights[map(index)];
	}
	public double equivalentBinEntries()
	{
		return sumWeight*sumWeight/sumWeightSquared;
	}
	public void fill(double x)
	{
		//int bin = xAxis.getBin(x);
		int bin = map(xAxis.coordToIndex(x));
		entries[bin]++;
		heights[bin]++;
		errors[bin]++;
		nEntry++;
		sumWeight++;
		sumWeightSquared++;
		mean += x;
		rms += x*x;
	}
	public void fill(double x, double weight)
	{
		//int bin = xAxis.getBin(x);
		int bin = map(xAxis.coordToIndex(x));
		entries[bin]++;
		heights[bin] += weight;
		errors[bin] += weight*weight;
		nEntry++;
		sumWeight += weight;
		sumWeightSquared += weight*weight;
		mean += x*weight;
		rms += x*weight*weight;
	}
	public double mean()
	{
		return mean/sumWeight;
	}
	public void reset()
	{
		 for (int i=0; i<entries.length; i++)
		 {
			 entries[i] = 0;
			 heights[i] = 0;
			 errors[i] = 0;
		 }
		 nEntry = 0;
		 sumWeight = 0;
		 sumWeightSquared = 0;
		 mean = 0;
		 rms = 0;
	}
	public double rms()
	{
		return Math.sqrt(rms/sumWeight - mean*mean/sumWeight/sumWeight);
	}
	/**
	 * Used internally for creating slices and projections
	 */
	void setContents(int[] entries, double[] heights, double[] errors)
	{
		this.entries = entries;
		this.heights = heights;
		this.errors = errors;
		
		for (int i=0; i<entries.length; i++) 
		{
			nEntry += entries[i];
			sumWeight += heights[i];
		}
		// TODO: Can we do anything sensible/useful with the other statistics?
		sumWeightSquared = Double.NaN;
		mean = Double.NaN;
		rms = Double.NaN;
	}
}
