package hep.aida.ref;

import hep.aida.IAxis;
import hep.aida.IHistogram;
import hep.aida.IHistogram2D;
import hep.aida.IHistogram3D;
/**
Abstract base class extracting and implementing most of the redundancy of the interface.

@author Wolfgang Hoschek, Tony Johnson, and others.
@version 1.0, 23/03/2000
*/
abstract class AbstractHistogram3D extends Histogram implements IHistogram3D
{
	protected IAxis xAxis, yAxis, zAxis;
	AbstractHistogram3D(String title)
	{
		super(title);
	}
	public int allEntries()
	{
		int n = 0;
		for (int i=xAxis.bins(); --i >= -2;)
			for (int j=yAxis.bins(); --j >= -2;)
				for (int k=zAxis.bins(); --k >= -2;)
		{
			n += binEntries(i,j,k);	 
		}
		return n;
	}
	public int dimensions()
	{
		return 3;
	}
	public int entries()
	{
		int n = 0;
		for (int i=0; i<xAxis.bins(); i++)
			for (int j=0; j<yAxis.bins(); j++)
				for (int k=0;k<zAxis.bins(); k++)
		{
			n += binEntries(i,j,k);	
		}
		return n;
	}
	public int extraEntries()
	{
		return allEntries() - entries();
	}
	public void fill(double x, double y, double z)
	{
		fill(x,y,z,1);
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
	protected abstract IHistogram2D internalSliceXY(String title, int indexZ1, int indexZ2);
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
	protected abstract IHistogram2D internalSliceXZ(String title, int indexY1, int indexY2);
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
	protected abstract IHistogram2D internalSliceYZ(String title, int indexX1, int indexX2);
	/**
	 * Package private method to map from the external representation of bin
	 * number to our internal representation of bin number
	 */
	int mapX(int index)
	{
		int bins = xAxis.bins() + 2;
		if (index >= bins) throw new IllegalArgumentException("bin="+index);
		if (index >= 0) return index+1;
		if (index == IHistogram.UNDERFLOW) return 0;
		if (index == IHistogram.OVERFLOW) return bins-1;
		throw new IllegalArgumentException("bin="+index);
	}
	/**
	 * Package private method to map from the external representation of bin
	 * number to our internal representation of bin number
	 */
	int mapY(int index)
	{
		int bins = yAxis.bins() + 2;
		if (index >= bins) throw new IllegalArgumentException("bin="+index);
		if (index >= 0) return index+1;
		if (index == IHistogram.UNDERFLOW) return 0;
		if (index == IHistogram.OVERFLOW) return bins-1;
		throw new IllegalArgumentException("bin="+index);
	}
	/**
	 * Package private method to map from the external representation of bin
	 * number to our internal representation of bin number
	 */
	int mapZ(int index)
	{
		int bins = zAxis.bins() + 2;
		if (index >= bins) throw new IllegalArgumentException("bin="+index);
		if (index >= 0) return index+1;
		if (index == IHistogram.UNDERFLOW) return 0;
		if (index == IHistogram.OVERFLOW) return bins-1;
		throw new IllegalArgumentException("bin="+index);
	}
	public int[] minMaxBins()
	{
		double minValue = Double.MAX_VALUE;
		double maxValue = Double.MIN_VALUE;
		int minBinX = -1;
		int minBinY = -1;
		int minBinZ = -1;
		int maxBinX = -1;
		int maxBinY = -1;
		int maxBinZ = -1;
		for (int i=xAxis.bins(); --i >= 0; ) {
			for (int j=yAxis.bins(); --j >= 0; ) {
				for (int k=zAxis.bins(); --k >= 0; ) {
					double value = binHeight(i,j,k);
					if (value < minValue) {
						minValue = value;
						minBinX = i;
						minBinY = j;
						minBinZ = k;
					}
					if (value > maxValue) {
						maxValue = value;
						maxBinX = i;
						maxBinY = j;
						maxBinZ = k;
					}
				}
			}
		}
		int[] result = {minBinX,minBinY,minBinZ, maxBinX,maxBinY,maxBinZ};
		return result;
	}
	public IHistogram2D projectionXY()
	{
		String newTitle = title() + " (projectionXY)";
		return internalSliceXY(newTitle,mapZ(IHistogram.UNDERFLOW),mapZ(IHistogram.OVERFLOW));
	}
	public IHistogram2D projectionXZ()
	{
		String newTitle = title() + " (projectionXZ)";
		return internalSliceXZ(newTitle,mapY(IHistogram.UNDERFLOW),mapY(IHistogram.OVERFLOW));
	}
	public IHistogram2D projectionYZ()
	{
		String newTitle = title() + " (projectionYZ)";
		return internalSliceYZ(newTitle,mapX(IHistogram.UNDERFLOW),mapX(IHistogram.OVERFLOW));
	}
	public IHistogram2D sliceXY(int indexZ )
	{
		return sliceXY(indexZ,indexZ);
	}
	public IHistogram2D sliceXY(int indexZ1, int indexZ2)
	{
		int start = mapZ(indexZ1);
		int stop = mapZ(indexZ2);
		String newTitle = title() + " (sliceXY ["+indexZ1+":"+indexZ2+"])";
		return internalSliceXY(newTitle,start,stop);
	}
	public IHistogram2D sliceXZ(int indexY )
	{
		return sliceXZ(indexY,indexY);
	}
	public IHistogram2D sliceXZ(int indexY1, int indexY2)
	{
		int start = mapY(indexY1);
		int stop = mapY(indexY2);
		String newTitle = title() + " (sliceXZ ["+indexY1+":"+indexY2+"])";
		return internalSliceXY(newTitle,start,stop);
	}
	public IHistogram2D sliceYZ(int indexX )
	{
		return sliceYZ(indexX,indexX);
	}
	public IHistogram2D sliceYZ(int indexX1, int indexX2)
	{
		int start = mapX(indexX1);
		int stop = mapX(indexX2);
		String newTitle = title() + " (sliceYZ ["+indexX1+":"+indexX2+"])";
		return internalSliceYZ(newTitle,start,stop);
	}
	public double sumAllBinHeights()
	{
		double n = 0;
		for (int i=xAxis.bins(); --i >= -2;)
			for (int j=yAxis.bins(); --j >= -2;)
				for (int k=zAxis.bins(); --k >= -2;)
		{
			n += binHeight(i,j,k);	 
		}
		return n;
	}
	public double sumBinHeights()
	{
		double n = 0;
		for (int i=0; i<xAxis.bins(); i++)
			for (int j=0; j<yAxis.bins(); j++)
				for (int k=0; k<zAxis.bins(); k++)
		{
			n += binHeight(i,j,k);	 
		}
		return n;
	}
	public double sumExtraBinHeights()
	{
		return sumAllBinHeights() - sumBinHeights();
	}
	public IAxis xAxis()
	{
		return xAxis;
	}
	public IAxis yAxis()
	{
		return yAxis;
	}
	public IAxis zAxis()
	{
		return zAxis;
	}
}
