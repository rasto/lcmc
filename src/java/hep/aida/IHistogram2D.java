package hep.aida;

/**
A Java interface corresponding to the AIDA 2D Histogram.
<p> 
<b>Note</b> All methods that accept a bin number as an argument will
also accept the constants OVERFLOW or UNDERFLOW as the argument, and 
as a result give the contents of the resulting OVERFLOW or UNDERFLOW
bin.
@see <a href="http://wwwinfo.cern.ch/asd/lhc++/AIDA/">AIDA</a>
@author Pavel Binko, Dino Ferrero Merlino, Wolfgang Hoschek, Tony Johnson, Andreas Pfeiffer, and others.
@version 1.0, 23/03/2000
*/
public interface IHistogram2D extends IHistogram
{
	/**
	 * The number of entries (ie the number of times fill was called for this bin).
	 * @param indexX the x bin number (0...Nx-1) or OVERFLOW or UNDERFLOW.
	 * @param indexY the y bin number (0...Ny-1) or OVERFLOW or UNDERFLOW.
	 */
	public int binEntries(int indexX, int indexY );
	/**
	 * Equivalent to <tt>projectionX().binEntries(indexX)</tt>.
	 */
	public int binEntriesX(int indexX);
	/**
	 * Equivalent to <tt>projectionY().binEntries(indexY)</tt>.
	 */
	public int binEntriesY(int indexY);
	/**
	 * The error on this bin.
	 * @param indexX the x bin number (0...Nx-1) or OVERFLOW or UNDERFLOW.
	 * @param indexY the y bin number (0...Ny-1) or OVERFLOW or UNDERFLOW.
	 */ 
	public double binError(int indexX,int indexY );
	/**
	 * Total height of the corresponding bin (ie the sum of the weights in this bin).
	 * @param indexX the x bin number (0...Nx-1) or OVERFLOW or UNDERFLOW.
	 * @param indexY the y bin number (0...Ny-1) or OVERFLOW or UNDERFLOW.
	 */ 
	public double binHeight(int indexX,int indexY );
	/**
	 * Equivalent to <tt>projectionX().binHeight(indexX)</tt>.
	 */
	public double binHeightX(int indexX);
	/**
	 * Equivalent to <tt>projectionY().binHeight(indexY)</tt>.
	 */
	public double binHeightY(int indexY);
	/**
	 * Fill the histogram with weight 1.
	 */
	public void fill( double x, double y);
	/**
	 * Fill the histogram with specified weight.
	 */
	public void fill( double x, double y, double weight);
	/**
	 *  Returns the mean of the histogram, as calculated on filling-time projected on the X axis.
	 */ 
	public double meanX();
	/**
	 *  Returns the mean of the histogram, as calculated on filling-time projected on the Y axis.
	 */ 
	public double meanY();
	/** 
	 * Indexes of the in-range bins containing the smallest and largest binHeight(), respectively.
	 * @return <tt>{minBinX,minBinY, maxBinX,maxBinY}</tt>.
	 */ 
	public int[] minMaxBins();
	/**
	 * Create a projection parallel to the X axis.
	 * Equivalent to <tt>sliceX(UNDERFLOW,OVERFLOW)</tt>.
	 */
	public IHistogram1D projectionX();
	/**
	 * Create a projection parallel to the Y axis.
	 * Equivalent to <tt>sliceY(UNDERFLOW,OVERFLOW)</tt>.
	 */
	public IHistogram1D projectionY();
	/**
	 * Returns the rms of the histogram as calculated on filling-time projected on the X axis.
	 */
	public double rmsX();
	/**
	 * Returns the rms of the histogram as calculated on filling-time projected on the Y axis.
	 */
	public double rmsY();
	/**
	 * Slice parallel to the Y axis at bin indexY and one bin wide.
	 * Equivalent to <tt>sliceX(indexY,indexY)</tt>.
	 */
	public IHistogram1D sliceX(int indexY );
	/**
	 * Create a slice parallel to the axis X axis, between "indexY1" and "indexY2" (inclusive).
	 * The returned IHistogram1D represents an instantaneous snapshot of the
	 * histogram at the time the slice was created.
	 */ 
	public IHistogram1D sliceX(int indexY1, int indexY2);
	/**
	 * Slice parallel to the X axis at bin indexX and one bin wide.
	 * Equivalent to <tt>sliceY(indexX,indexX)</tt>.
	 */
	public IHistogram1D sliceY(int indexX );
	/**
	 * Create a slice parallel to the axis Y axis, between "indexX1" and "indexX2" (inclusive)
	 * The returned IHistogram1D represents an instantaneous snapshot of the
	 * histogram at the time the slice was created.
	 */ 
	public IHistogram1D sliceY(int indexX1, int indexX2);
	/**
	 * Return the X axis.
	 */
	public IAxis xAxis();
	/**
	 * Return the Y axis.
	 */
	public IAxis yAxis();
}
