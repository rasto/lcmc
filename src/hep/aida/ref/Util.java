package hep.aida.ref;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * Convenient histogram utilities. 
 */
class Util
{
	/**
	 * Creates a new utility object.
	 */
	public Util() {}
	/** 
	 * Returns the index of the in-range bin containing the maxBinHeight().
	 */ 
	public int maxBin(IHistogram1D h)
	{
		int maxBin = -1;
		double maxValue = Double.MIN_VALUE;
		for (int i=h.xAxis().bins(); --i >= 0; ) {
			double value = h.binHeight(i);
			if (value > maxValue) {
				maxValue = value;
				maxBin = i;
			}
		}
		return maxBin;
	}
	/** 
	 * Returns the indexX of the in-range bin containing the maxBinHeight().
	 */ 
	public int maxBinX(IHistogram2D h)
	{
		double maxValue = Double.MIN_VALUE;
		int maxBinX = -1;
		int maxBinY = -1;
		for (int i=h.xAxis().bins(); --i >= 0; ) {
			for (int j=h.yAxis().bins(); --j >= 0; ) {
				double value = h.binHeight(i,j);
				if (value > maxValue) {
					maxValue = value;
					maxBinX = i;
					maxBinY = j;
				}
			}
		}
		return maxBinX;
	}
	/** 
	 * Returns the indexY of the in-range bin containing the maxBinHeight().
	 */ 
	public int maxBinY(IHistogram2D h)
	{
		double maxValue = Double.MIN_VALUE;
		int maxBinX = -1;
		int maxBinY = -1;
		for (int i=h.xAxis().bins(); --i >= 0; ) {
			for (int j=h.yAxis().bins(); --j >= 0; ) {
				double value = h.binHeight(i,j);
				if (value > maxValue) {
					maxValue = value;
					maxBinX = i;
					maxBinY = j;
				}
			}
		}
		return maxBinY;
	}
	/** 
	 * Returns the index of the in-range bin containing the minBinHeight().
	 */ 
	public int minBin(IHistogram1D h)
	{
		int minBin = -1;
		double minValue = Double.MAX_VALUE;
		for (int i=h.xAxis().bins(); --i >= 0; ) {
			double value = h.binHeight(i);
			if (value < minValue) {
				minValue = value;
				minBin = i;
			}
		}
		return minBin;
	}
	/** 
	 * Returns the indexX of the in-range bin containing the minBinHeight().
	 */ 
	public int minBinX(IHistogram2D h)
	{
		double minValue = Double.MAX_VALUE;
		int minBinX = -1;
		int minBinY = -1;
		for (int i=h.xAxis().bins(); --i >= 0; ) {
			for (int j=h.yAxis().bins(); --j >= 0; ) {
				double value = h.binHeight(i,j);
				if (value < minValue) {
					minValue = value;
					minBinX = i;
					minBinY = j;
				}
			}
		}
		return minBinX;
	}
	/** 
	 * Returns the indexY of the in-range bin containing the minBinHeight().
	 */ 
	public int minBinY(IHistogram2D h)
	{
		double minValue = Double.MAX_VALUE;
		int minBinX = -1;
		int minBinY = -1;
		for (int i=h.xAxis().bins(); --i >= 0; ) {
			for (int j=h.yAxis().bins(); --j >= 0; ) {
				double value = h.binHeight(i,j);
				if (value < minValue) {
					minValue = value;
					minBinX = i;
					minBinY = j;
				}
			}
		}
		return minBinY;
	}
}
