package hep.aida.ref;

import hep.aida.IAxis;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
/**
A reference implementation of hep.aida.IHistogram2D.
The goal is to provide a clear implementation rather than the most efficient implementation.
However, performance seems fine - filling 6 * 10^5 points/sec, both using FixedAxis or VariableAxis.

@author Wolfgang Hoschek, Tony Johnson, and others.
@version 1.0, 23/03/2000
*/
public class Histogram2D extends AbstractHistogram2D implements IHistogram2D
{
	private double[][] heights;
	private double[][] errors;
	private int[][] entries;
	private int nEntry; // total number of times fill called
	private double sumWeight; // Sum of all weights
	private double sumWeightSquared; // Sum of the squares of the weights
	private double meanX, rmsX;
	private double meanY, rmsY;
	/**
	 * Creates a variable-width histogram.
	 * Example: <tt>xEdges = (0.2, 1.0, 5.0, 6.0), yEdges = (-5, 0, 7)</tt> yields 3*2 in-range bins.
	 * @param title The histogram title.
	 * @param xEdges the bin boundaries the x-axis shall have;
	 *        must be sorted ascending and must not contain multiple identical elements.
	 * @param yEdges the bin boundaries the y-axis shall have;
	 *        must be sorted ascending and must not contain multiple identical elements.
	 * @throws IllegalArgumentException if <tt>xEdges.length < 1 || yEdges.length < 1</tt>.
	 */
	public Histogram2D(String title, double[] xEdges, double[] yEdges)
	{
		this(title,new VariableAxis(xEdges), new VariableAxis(yEdges));
	}
	/**
	 * Creates a fixed-width histogram.
	 * 
	 * @param title The histogram title.
	 * @param xBins The number of bins on the X axis.
	 * @param xMin The minimum value on the X axis.
	 * @param xMax The maximum value on the X axis.
	 * @param yBins The number of bins on the Y axis.
	 * @param yMin The minimum value on the Y axis.
	 * @param yMax The maximum value on the Y axis.
	 */
	public Histogram2D(String title, int xBins, double xMin, double xMax,
	                                 int yBins, double yMin, double yMax)
	{
		this(title, new FixedAxis(xBins,xMin,xMax), new FixedAxis(yBins,yMin,yMax));
	}
	/**
	 * Creates a histogram with the given axis binning.
	 * 
	 * @param title The histogram title.
	 * @param xAxis The x-axis description to be used for binning.
	 * @param yAxis The y-axis description to be used for binning.
	 */
	public Histogram2D(String title, IAxis xAxis, IAxis yAxis)
	{
		super(title);
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		int xBins = xAxis.bins();
		int yBins = yAxis.bins();

		entries = new int[xBins+2][yBins+2];
		heights = new double[xBins+2][yBins+2];
		errors = new double[xBins+2][yBins+2];

	}
	public int allEntries()
	{
		return nEntry;
	}
	public int binEntries(int indexX, int indexY)
	{
		//return entries[xAxis.map(indexX)][yAxis.map(indexY)];
		return entries[mapX(indexX)][mapY(indexY)];
	}
	public double binError(int indexX, int indexY)
	{
		//return Math.sqrt(errors[xAxis.map(indexX)][yAxis.map(indexY)]);
		return Math.sqrt(errors[mapX(indexX)][mapY(indexY)]);
	}
	public double binHeight(int indexX, int indexY)
	{
		//return heights[xAxis.map(indexX)][yAxis.map(indexY)];
		return heights[mapX(indexX)][mapY(indexY)];
	}
	public double equivalentBinEntries()
	{
		return sumWeight*sumWeight/sumWeightSquared;
	}
	public void fill(double x, double y)
	{
		//int xBin = xAxis.getBin(x);
		//int yBin = xAxis.getBin(y);
		int xBin = mapX(xAxis.coordToIndex(x));
		int yBin = mapY(yAxis.coordToIndex(y));
		entries[xBin][yBin]++;
		heights[xBin][yBin]++;
		errors[xBin][yBin]++;
		nEntry++;
		sumWeight++;
		sumWeightSquared++;
		meanX += x;
		rmsX += x;
		meanY += y;
		rmsY += y;
	}
	public void fill(double x, double y, double weight)
	{
		//int xBin = xAxis.getBin(x);
		//int yBin = xAxis.getBin(y);
		int xBin = mapX(xAxis.coordToIndex(x));
		int yBin = mapY(yAxis.coordToIndex(y));
		entries[xBin][yBin]++;
		heights[xBin][yBin] += weight;
		errors[xBin][yBin] += weight*weight;
		nEntry++;
		sumWeight += weight;
		sumWeightSquared += weight*weight;
		meanX += x*weight;
		rmsX += x*weight*weight;
		meanY += y*weight;
		rmsY += y*weight*weight;
	}
	/**
	 * The precise meaning of the arguments to the public slice
	 * methods is somewhat ambiguous, so we define this internal
	 * slice method and clearly specify its arguments.
	 * <p>
	 * <b>Note 0</b>indexY1 and indexY2 use our INTERNAL bin numbering scheme
	 * <b>Note 1</b>The slice is done between indexY1 and indexY2 INCLUSIVE
	 * <b>Note 2</b>indexY1 and indexY2 may include the use of under and over flow bins
	 * <b>Note 3</b>There is no note 3 (yet)
	 */
	protected IHistogram1D internalSliceX(String title, int indexY1, int indexY2)
	{
		// Attention: our internal definition of bins has been choosen
		// so that this works properly even if the indeces passed in include
		// the underflow or overflow bins
		if (indexY2 < indexY1) throw new IllegalArgumentException("Invalid bin range");
		
		int sliceBins = xAxis.bins() + 2;
		int[] sliceEntries = new int[sliceBins];
		double[] sliceHeights = new double[sliceBins];
		double[] sliceErrors = new double[sliceBins];
		
		//for (int i=xAxis.under; i<=xAxis.over; i++)
		for (int i=0; i<sliceBins; i++)
		{
			for (int j=indexY1; j<=indexY2; j++)
			{
				sliceEntries[i] += entries[i][j]; 
				sliceHeights[i] += heights[i][j]; 
				sliceErrors[i] += errors[i][j]; 
			}
		}
		Histogram1D result = new Histogram1D(title,xAxis);
		result.setContents(sliceEntries,sliceHeights,sliceErrors);
		return result;
	}
	/**
	 * The precise meaning of the arguments to the public slice
	 * methods is somewhat ambiguous, so we define this internal
	 * slice method and clearly specify its arguments.
	 * <p>
	 * <b>Note 0</b>indexX1 and indexX2 use our INTERNAL bin numbering scheme
	 * <b>Note 1</b>The slice is done between indexX1 and indexX2 INCLUSIVE
	 * <b>Note 2</b>indexX1 and indexX2 may include the use of under and over flow bins
	 * <b>Note 3</b>There is no note 3 (yet)
	 */
	protected IHistogram1D internalSliceY(String title, int indexX1, int indexX2)
	{
		// Attention: our internal definition of bins has been choosen
		// so that this works properly even if the indeces passed in include
		// the underflow or overflow bins
		if (indexX2 < indexX1) throw new IllegalArgumentException("Invalid bin range");
		
		int sliceBins = yAxis.bins() + 2;
		int[] sliceEntries = new int[sliceBins];
		double[] sliceHeights = new double[sliceBins];
		double[] sliceErrors = new double[sliceBins];
		
		for (int i=indexX1; i<=indexX2; i++)
		{
			//for (int j=yAxis.under; j<=yAxis.over; j++)
			for (int j=0; j<sliceBins; j++)
			{
				sliceEntries[j] += entries[i][j]; 
				sliceHeights[j] += heights[i][j]; 
				sliceErrors[j] += errors[i][j]; 
			}
		}
		Histogram1D result = new Histogram1D(title,yAxis);
		result.setContents(sliceEntries,sliceHeights,sliceErrors);
		return result;
	}
	public double meanX()
	{
		return meanX/sumWeight;
	}
	public double meanY()
	{
		return meanY/sumWeight;
	}
	public void reset()
	{
		 for (int i=0; i<entries.length; i++)
			 for (int j=0; j<entries[0].length; j++)
		 {
			 entries[i][j] = 0;
			 heights[i][j] = 0;
			 errors[i][j] = 0;
		 }
		 nEntry = 0;
		 sumWeight = 0;
		 sumWeightSquared = 0;
		 meanX = 0;
		 rmsX = 0;
		 meanY = 0;
		 rmsY = 0;
	}
	public double rmsX()
	{
		return Math.sqrt(rmsX/sumWeight - meanX*meanX/sumWeight/sumWeight);
	}
	public double rmsY()
	{
		return Math.sqrt(rmsY/sumWeight - meanY*meanY/sumWeight/sumWeight);
	}
	/**
	 * Used internally for creating slices and projections
	 */
	void setContents(int[][] entries, double[][] heights, double[][] errors)
	{
		this.entries = entries;
		this.heights = heights;
		this.errors = errors;
		
		for (int i=0; i<entries.length; i++) 
			for (int j=0; j<entries[0].length; j++) 
		{
			nEntry += entries[i][j];
			sumWeight += heights[i][j];
		}
		// TODO: Can we do anything sensible/useful with the other statistics?
		sumWeightSquared = Double.NaN;
		meanX = Double.NaN;
		rmsX = Double.NaN;
		meanY = Double.NaN;
		rmsY = Double.NaN;
	}
	public double sumAllBinHeights()
	{
		return sumWeight;
	}
}
