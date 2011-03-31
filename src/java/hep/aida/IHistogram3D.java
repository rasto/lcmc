package hep.aida;

/**
A Java interface corresponding to the AIDA 3D Histogram.
<p> 
<b>Note</b> All methods that accept a bin number as an argument will
also accept the constants OVERFLOW or UNDERFLOW as the argument, and 
as a result give the contents of the resulting OVERFLOW or UNDERFLOW
bin.
@see <a href="http://wwwinfo.cern.ch/asd/lhc++/AIDA/">AIDA</a>
@author Pavel Binko, Dino Ferrero Merlino, Wolfgang Hoschek, Tony Johnson, Andreas Pfeiffer, and others.
@version 1.0, 23/03/2000
*/
public interface IHistogram3D extends IHistogram
{
	/**
	 * The number of entries (ie the number of times fill was called for this bin).
	 * @param indexX the x bin number (0...Nx-1) or OVERFLOW or UNDERFLOW.
	 * @param indexY the y bin number (0...Ny-1) or OVERFLOW or UNDERFLOW.
	 * @param indexZ the z bin number (0...Nz-1) or OVERFLOW or UNDERFLOW.
	 */
	public int binEntries(int indexX, int indexY, int indexZ);
	/**
	 * The error on this bin.
	 * @param indexX the x bin number (0...Nx-1) or OVERFLOW or UNDERFLOW.
	 * @param indexY the y bin number (0...Ny-1) or OVERFLOW or UNDERFLOW.
	 * @param indexZ the z bin number (0...Nz-1) or OVERFLOW or UNDERFLOW.
	 */ 
	public double binError(int indexX,int indexY,int indexZ );
	/**
	 * Total height of the corresponding bin (ie the sum of the weights in this bin).
	 * @param indexX the x bin number (0...Nx-1) or OVERFLOW or UNDERFLOW.
	 * @param indexY the y bin number (0...Ny-1) or OVERFLOW or UNDERFLOW.
	 * @param indexZ the z bin number (0...Nz-1) or OVERFLOW or UNDERFLOW.
	 */ 
	public double binHeight(int indexX,int indexY,int indexZ);
	/**
	 * Fill the histogram with weight 1; equivalent to <tt>fill(x,y,z,1)</tt>..
	 */
	public void fill( double x, double y, double z);
	/**
	 * Fill the histogram with specified weight.
	 */
	public void fill( double x, double y, double z, double weight);
	/**
	 *  Returns the mean of the histogram, as calculated on filling-time projected on the X axis.
	 */ 
	public double meanX();
	/**
	 *  Returns the mean of the histogram, as calculated on filling-time projected on the Y axis.
	 */ 
	public double meanY();
	/**
	 *  Returns the mean of the histogram, as calculated on filling-time projected on the Z axis.
	 */ 
	public double meanZ();
	/** 
	 * Indexes of the in-range bins containing the smallest and largest binHeight(), respectively.
	 * @return <tt>{minBinX,minBinY,minBinZ, maxBinX,maxBinY,maxBinZ}</tt>.
	 */ 
	public int[] minMaxBins();
	/**
	 * Create a projection parallel to the XY plane.
	 * Equivalent to <tt>sliceXY(UNDERFLOW,OVERFLOW)</tt>.
	 */
	public IHistogram2D projectionXY();
	/**
	 * Create a projection parallel to the XZ plane.
	 * Equivalent to <tt>sliceXZ(UNDERFLOW,OVERFLOW)</tt>.
	 */
	public IHistogram2D projectionXZ();
	/**
	 * Create a projection parallel to the YZ plane.
	 * Equivalent to <tt>sliceYZ(UNDERFLOW,OVERFLOW)</tt>.
	 */
	public IHistogram2D projectionYZ();
	/**
	 * Returns the rms of the histogram as calculated on filling-time projected on the X axis.
	 */
	public double rmsX();
	/**
	 * Returns the rms of the histogram as calculated on filling-time projected on the Y axis.
	 */
	public double rmsY();
	/**
	 * Returns the rms of the histogram as calculated on filling-time projected on the Z axis.
	 */
	public double rmsZ();
	/**
	 * Create a slice parallel to the XY plane at bin indexZ and one bin wide.
	 * Equivalent to <tt>sliceXY(indexZ,indexZ)</tt>.
	 */
	public IHistogram2D sliceXY(int indexZ );
	/**
	 * Create a slice parallel to the XY plane, between "indexZ1" and "indexZ2" (inclusive).
	 * The returned IHistogram2D represents an instantaneous snapshot of the
	 * histogram at the time the slice was created.
	 */ 
	public IHistogram2D sliceXY(int indexZ1, int indexZ2);
	/**
	 * Create a slice parallel to the XZ plane at bin indexY and one bin wide.
	 * Equivalent to <tt>sliceXZ(indexY,indexY)</tt>.
	 */
	public IHistogram2D sliceXZ(int indexY );
	/**
	 * Create a slice parallel to the XZ plane, between "indexY1" and "indexY2" (inclusive).
	 * The returned IHistogram2D represents an instantaneous snapshot of the
	 * histogram at the time the slice was created.
	 */ 
	public IHistogram2D sliceXZ(int indexY1, int indexY2);
	/**
	 * Create a slice parallel to the YZ plane at bin indexX and one bin wide.
	 * Equivalent to <tt>sliceYZ(indexX,indexX)</tt>.
	 */
	public IHistogram2D sliceYZ(int indexX );
	/**
	 * Create a slice parallel to the YZ plane, between "indexX1" and "indexX2" (inclusive).
	 * The returned IHistogram2D represents an instantaneous snapshot of the
	 * histogram at the time the slice was created.
	 */ 
	public IHistogram2D sliceYZ(int indexX1, int indexX2);
	/**
	 * Return the X axis.
	 */
	public IAxis xAxis();
	/**
	 * Return the Y axis.
	 */
	public IAxis yAxis();
	/**
	 * Return the Z axis.
	 */
	public IAxis zAxis();
}
