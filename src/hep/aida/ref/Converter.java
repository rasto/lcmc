package hep.aida.ref;

import hep.aida.IAxis;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogram3D;

/**
 * Histogram conversions, for example to String and XML format; 
 * This class requires the Colt distribution, whereas the rest of the package is entirelly stand-alone.
 */
public class Converter
{
	/**
	 * Creates a new histogram converter.
	 */
	public Converter() {}
	/** 
	 * Returns all edges of the given axis.
	 */ 
	public double[] edges(IAxis axis)
	{
		int b = axis.bins();
		double[] bounds = new double[b+1];
		for (int i=0; i<b; i++) bounds[i]=axis.binLowerEdge(i);
		bounds[b]=axis.upperEdge();
		return bounds;
	}
	String form(cern.colt.matrix.impl.Former formatter, double value) 
	{
		return formatter.form(value);
	}
	/** 
	 * Returns an array[h.xAxis().bins()]; ignoring extra bins.
	 */ 
	protected double[] toArrayErrors(IHistogram1D h)
	{
		int xBins = h.xAxis().bins();
		double[] array = new double[xBins];
		for (int j=xBins; --j >= 0; ) {
				array[j] = h.binError(j);
		}
		return array;
	}
	/** 
	 * Returns an array[h.xAxis().bins()][h.yAxis().bins()]; ignoring extra bins.
	 */ 
	protected double[][] toArrayErrors(IHistogram2D h)
	{
		int xBins = h.xAxis().bins();
		int yBins = h.yAxis().bins();
		double[][] array = new double[xBins][yBins];
		for (int i=yBins; --i >= 0; ) {
			for (int j=xBins; --j >= 0; ) {
				array[j][i] = h.binError(j,i);
			}
		}
		return array;
	}
	/** 
	 * Returns an array[h.xAxis().bins()]; ignoring extra bins.
	 */ 
	protected double[] toArrayHeights(IHistogram1D h)
	{
		int xBins = h.xAxis().bins();
		double[] array = new double[xBins];
		for (int j=xBins; --j >= 0; ) {
				array[j] = h.binHeight(j);
		}
		return array;
	}
	/** 
	 * Returns an array[h.xAxis().bins()][h.yAxis().bins()]; ignoring extra bins.
	 */ 
	protected double[][] toArrayHeights(IHistogram2D h)
	{
		int xBins = h.xAxis().bins();
		int yBins = h.yAxis().bins();
		double[][] array = new double[xBins][yBins];
		for (int i=yBins; --i >= 0; ) {
			for (int j=xBins; --j >= 0; ) {
				array[j][i] = h.binHeight(j,i);
			}
		}
		return array;
	}
	/** 
	 * Returns an array[h.xAxis().bins()][h.yAxis().bins()][h.zAxis().bins()]; ignoring extra bins.
	 */ 
	protected double[][][] toArrayHeights(IHistogram3D h)
	{
		int xBins = h.xAxis().bins();
		int yBins = h.yAxis().bins();
		int zBins = h.zAxis().bins();
		double[][][] array = new double[xBins][yBins][zBins];
		for (int j=xBins; --j >= 0; ) {
			for (int i=yBins; --i >= 0; ) {
				for (int k=zBins; --k >= 0; ) {
					array[j][i][k] = h.binHeight(j,i,k);
				}
			}
		}
		return array;
	}
	/**
	 * Returns a string representation of the specified array.  The string
	 * representation consists of a list of the arrays's elements, enclosed in square brackets
	 * (<tt>"[]"</tt>).  Adjacent elements are separated by the characters
	 * <tt>", "</tt> (comma and space).
	 * @return a string representation of the specified array.
	 */
	protected static String toString(double[] array) {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		int maxIndex = array.length - 1;
		for (int i = 0; i <= maxIndex; i++) {
		    buf.append(array[i]);
		    if (i < maxIndex)
			buf.append(", ");
		}
		buf.append("]");
		return buf.toString();
	}
	/** 
	 * Returns a string representation of the given argument.
	 */ 
	public String toString(IAxis axis) 
	{
		StringBuffer buf = new StringBuffer();
		buf.append("Range: ["+axis.lowerEdge()+","+axis.upperEdge()+")");
		buf.append(", Bins: "+axis.bins());
		buf.append(", Bin edges: "+toString(edges(axis))+"\n");
		return buf.toString();
	}
	/** 
	 * Returns a string representation of the given argument.
	 */ 
	public String toString(IHistogram1D h)
	{		
		String columnAxisName = null; //"X";
		String rowAxisName = null;
		hep.aida.bin.BinFunction1D[] aggr = null; //{hep.aida.bin.BinFunctions1D.sum};
		String format = "%G";
		//String format = "%1.2G";

		cern.colt.matrix.impl.Former f = new cern.colt.matrix.impl.FormerFactory().create(format);
		String sep = System.getProperty("line.separator");
		int[] minMaxBins = h.minMaxBins();
		String title = h.title() + ":" + sep +
			"   Entries="+form(f,h.entries()) + ", ExtraEntries="+form(f,h.extraEntries()) + sep +
			"   Mean=" +form(f,h.mean()) + ", Rms="+form(f,h.rms()) + sep +			
			"   MinBinHeight=" +form(f,h.binHeight(minMaxBins[0])) + ", MaxBinHeight="+form(f,h.binHeight(minMaxBins[1])) + sep +
			"   Axis: " +
			"Bins="+form(f,h.xAxis().bins()) +
			", Min="+form(f,h.xAxis().lowerEdge()) +
			", Max="+form(f,h.xAxis().upperEdge());
				
		String[] xEdges = new String[h.xAxis().bins()];
		for (int i=0; i<h.xAxis().bins(); i++) xEdges[i]=form(f,h.xAxis().binLowerEdge(i));

		String[] yEdges = null;

		cern.colt.matrix.DoubleMatrix2D heights = new cern.colt.matrix.impl.DenseDoubleMatrix2D(1,h.xAxis().bins());
		heights.viewRow(0).assign(toArrayHeights(h));
		//cern.colt.matrix.DoubleMatrix2D errors = new cern.colt.matrix.impl.DenseDoubleMatrix2D(1,h.xAxis().bins());
		//errors.viewRow(0).assign(toArrayErrors(h));
		
		return title + sep +
			"Heights:" + sep +
			new cern.colt.matrix.doublealgo.Formatter().toTitleString(
				heights,yEdges,xEdges,rowAxisName,columnAxisName,null,aggr);
			/*
			+ sep +
			"Errors:" + sep +
			new cern.colt.matrix.doublealgo.Formatter().toTitleString(
				errors,yEdges,xEdges,rowAxisName,columnAxisName,null,aggr);
			*/
	}
	/** 
	 * Returns a string representation of the given argument.
	 */ 
	public String toString(IHistogram2D h)
	{		
		String columnAxisName = "X";
		String rowAxisName = "Y";
		hep.aida.bin.BinFunction1D[] aggr = {hep.aida.bin.BinFunctions1D.sum};
		String format = "%G";
		//String format = "%1.2G";

		cern.colt.matrix.impl.Former f = new cern.colt.matrix.impl.FormerFactory().create(format);
		String sep = System.getProperty("line.separator");
		int[] minMaxBins = h.minMaxBins();
		String title = h.title() + ":" + sep +
			"   Entries="+form(f,h.entries()) + ", ExtraEntries="+form(f,h.extraEntries()) + sep +
			"   MeanX=" +form(f,h.meanX()) + ", RmsX="+form(f,h.rmsX()) + sep +
			"   MeanY=" +form(f,h.meanY()) + ", RmsY="+form(f,h.rmsX()) + sep +
			"   MinBinHeight=" +form(f,h.binHeight(minMaxBins[0],minMaxBins[1])) + ", MaxBinHeight="+form(f,h.binHeight(minMaxBins[2],minMaxBins[3])) + sep +
			
			"   xAxis: " +
			"Bins="+form(f,h.xAxis().bins()) + 
			", Min="+form(f,h.xAxis().lowerEdge()) +
			", Max="+form(f,h.xAxis().upperEdge()) + sep +
			
			"   yAxis: " +
			"Bins="+form(f,h.yAxis().bins()) +
			", Min="+form(f,h.yAxis().lowerEdge()) +
			", Max="+form(f,h.yAxis().upperEdge());
		
		String[] xEdges = new String[h.xAxis().bins()];
		for (int i=0; i<h.xAxis().bins(); i++) xEdges[i]=form(f,h.xAxis().binLowerEdge(i));

		String[] yEdges = new String[h.yAxis().bins()];
		for (int i=0; i<h.yAxis().bins(); i++) yEdges[i]=form(f,h.yAxis().binLowerEdge(i));
		new cern.colt.list.ObjectArrayList(yEdges).reverse(); // keep coord. system

		cern.colt.matrix.DoubleMatrix2D heights = new cern.colt.matrix.impl.DenseDoubleMatrix2D(toArrayHeights(h));
		heights = heights.viewDice().viewRowFlip(); // keep the histo coord. system
		//heights = heights.viewPart(1,1,heights.rows()-2,heights.columns()-2); // ignore under&overflows
		
		//cern.colt.matrix.DoubleMatrix2D errors = new cern.colt.matrix.impl.DenseDoubleMatrix2D(toArrayErrors(h));
		//errors = errors.viewDice().viewRowFlip(); // keep the histo coord system
		////errors = errors.viewPart(1,1,errors.rows()-2,errors.columns()-2); // ignore under&overflows
		
		return title + sep +
			"Heights:" + sep +
			new cern.colt.matrix.doublealgo.Formatter().toTitleString(
				heights,yEdges,xEdges,rowAxisName,columnAxisName,null,aggr);
			/*
			+ sep +
			"Errors:" + sep +
			new cern.colt.matrix.doublealgo.Formatter().toTitleString(
				errors,yEdges,xEdges,rowAxisName,columnAxisName,null,aggr);
			*/
	}
	/** 
	 * Returns a string representation of the given argument.
	 */ 
	public String toString(IHistogram3D h)
	{		
		String columnAxisName = "X";
		String rowAxisName = "Y";
		String sliceAxisName = "Z";
		hep.aida.bin.BinFunction1D[] aggr = {hep.aida.bin.BinFunctions1D.sum};
		String format = "%G";
		//String format = "%1.2G";

		cern.colt.matrix.impl.Former f = new cern.colt.matrix.impl.FormerFactory().create(format);
		String sep = System.getProperty("line.separator");
		int[] minMaxBins = h.minMaxBins();
		String title = h.title() + ":" + sep +
			"   Entries="+form(f,h.entries()) + ", ExtraEntries="+form(f,h.extraEntries()) + sep +
			"   MeanX=" +form(f,h.meanX()) + ", RmsX="+form(f,h.rmsX()) + sep +
			"   MeanY=" +form(f,h.meanY()) + ", RmsY="+form(f,h.rmsX()) + sep +
			"   MeanZ=" +form(f,h.meanZ()) + ", RmsZ="+form(f,h.rmsZ()) + sep +
			"   MinBinHeight=" +form(f,h.binHeight(minMaxBins[0],minMaxBins[1],minMaxBins[2])) + ", MaxBinHeight="+form(f,h.binHeight(minMaxBins[3],minMaxBins[4],minMaxBins[5])) + sep +
			
			"   xAxis: " +
			"Bins="+form(f,h.xAxis().bins()) + 
			", Min="+form(f,h.xAxis().lowerEdge()) +
			", Max="+form(f,h.xAxis().upperEdge()) + sep +
			
			"   yAxis: " +
			"Bins="+form(f,h.yAxis().bins()) +
			", Min="+form(f,h.yAxis().lowerEdge()) +
			", Max="+form(f,h.yAxis().upperEdge()) + sep +
			
			"   zAxis: " +
			"Bins="+form(f,h.zAxis().bins()) +
			", Min="+form(f,h.zAxis().lowerEdge()) +
			", Max="+form(f,h.zAxis().upperEdge());
		
		String[] xEdges = new String[h.xAxis().bins()];
		for (int i=0; i<h.xAxis().bins(); i++) xEdges[i]=form(f,h.xAxis().binLowerEdge(i));

		String[] yEdges = new String[h.yAxis().bins()];
		for (int i=0; i<h.yAxis().bins(); i++) yEdges[i]=form(f,h.yAxis().binLowerEdge(i));
		new cern.colt.list.ObjectArrayList(yEdges).reverse(); // keep coord. system

		String[] zEdges = new String[h.zAxis().bins()];
		for (int i=0; i<h.zAxis().bins(); i++) zEdges[i]=form(f,h.zAxis().binLowerEdge(i));
		new cern.colt.list.ObjectArrayList(zEdges).reverse(); // keep coord. system

		cern.colt.matrix.DoubleMatrix3D heights = new cern.colt.matrix.impl.DenseDoubleMatrix3D(toArrayHeights(h));
		heights = heights.viewDice(2,1,0).viewSliceFlip().viewRowFlip(); // keep the histo coord. system
		//heights = heights.viewPart(1,1,heights.rows()-2,heights.columns()-2); // ignore under&overflows
		
		//cern.colt.matrix.DoubleMatrix2D errors = new cern.colt.matrix.impl.DenseDoubleMatrix2D(toArrayErrors(h));
		//errors = errors.viewDice().viewRowFlip(); // keep the histo coord system
		////errors = errors.viewPart(1,1,errors.rows()-2,errors.columns()-2); // ignore under&overflows
		
		return title + sep +
			"Heights:" + sep +
			new cern.colt.matrix.doublealgo.Formatter().toTitleString(
				heights,zEdges,yEdges,xEdges,sliceAxisName,rowAxisName,columnAxisName,"",aggr);
			/*
			+ sep +
			"Errors:" + sep +
			new cern.colt.matrix.doublealgo.Formatter().toTitleString(
				errors,yEdges,xEdges,rowAxisName,columnAxisName,null,aggr);
			*/
	}
	/** 
	 * Returns a XML representation of the given argument.
	 */ 
	public String toXML(IHistogram1D h)
	{
			StringBuffer buf = new StringBuffer();
			String sep = System.getProperty("line.separator");
			buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>"); buf.append(sep);
			buf.append("<!DOCTYPE plotML SYSTEM \"plotML.dtd\">"); buf.append(sep);
			buf.append("<plotML>"); buf.append(sep);
			buf.append("<plot>"); buf.append(sep);
			buf.append("<dataArea>"); buf.append(sep);
			buf.append("<data1d>"); buf.append(sep);
			buf.append("<bins1d title=\""+h.title()+"\">"); buf.append(sep);
			for (int i=0; i<h.xAxis().bins(); i++)
			{
				buf.append(h.binEntries(i)+","+h.binError(i)); buf.append(sep);
			}
			buf.append("</bins1d>"); buf.append(sep);
			buf.append("<binnedDataAxisAttributes type=\"double\" axis=\"x0\"");
			  buf.append(" min=\""+h.xAxis().lowerEdge()+"\"");
			  buf.append(" max=\""+h.xAxis().upperEdge()+"\"");
			  buf.append(" numberOfBins=\""+h.xAxis().bins()+"\"");
			  buf.append("/>"); buf.append(sep);
			buf.append("<statistics>"); buf.append(sep);
			buf.append("<statistic name=\"Entries\" value=\""+h.entries()+"\"/>"); buf.append(sep);
			buf.append("<statistic name=\"Underflow\" value=\""+h.binEntries(h.UNDERFLOW)+"\"/>"); buf.append(sep);
			buf.append("<statistic name=\"Overflow\" value=\""+h.binEntries(h.OVERFLOW)+"\"/>"); buf.append(sep);
			if (!Double.isNaN(h.mean())) {
				buf.append("<statistic name=\"Mean\" value=\""+h.mean()+"\"/>"); buf.append(sep);
			}
			if (!Double.isNaN(h.rms())) {
				buf.append("<statistic name=\"RMS\" value=\""+h.rms()+"\"/>"); buf.append(sep);
			}
			buf.append("</statistics>"); buf.append(sep);
			buf.append("</data1d>"); buf.append(sep);
			buf.append("</dataArea>"); buf.append(sep);
			buf.append("</plot>"); buf.append(sep);
			buf.append("</plotML>"); buf.append(sep);
			return buf.toString();
	}
	/** 
	 * Returns a XML representation of the given argument.
	 */ 
	public String toXML(IHistogram2D h)
	{
			StringBuffer out = new StringBuffer();
			String sep = System.getProperty("line.separator");
			out.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>"); out.append(sep);
			out.append("<!DOCTYPE plotML SYSTEM \"plotML.dtd\">"); out.append(sep);
			out.append("<plotML>"); out.append(sep);
			out.append("<plot>"); out.append(sep);
			out.append("<dataArea>"); out.append(sep);
			out.append("<data2d type=\"xxx\">"); out.append(sep);
			out.append("<bins2d title=\""+h.title()+"\" xSize=\""+h.xAxis().bins()+"\" ySize=\""+h.yAxis().bins()+"\">"); out.append(sep);
			for (int i=0; i<h.xAxis().bins(); i++)
				for (int j=0; j<h.yAxis().bins(); j++)
			{
				out.append(h.binEntries(i,j)+","+h.binError(i,j)); out.append(sep);
			}
			out.append("</bins2d>"); out.append(sep);
			out.append("<binnedDataAxisAttributes type=\"double\" axis=\"x0\"");
			  out.append(" min=\""+h.xAxis().lowerEdge()+"\"");
			  out.append(" max=\""+h.xAxis().upperEdge()+"\"");
			  out.append(" numberOfBins=\""+h.xAxis().bins()+"\"");
			  out.append("/>"); out.append(sep);
			out.append("<binnedDataAxisAttributes type=\"double\" axis=\"y0\"");
			  out.append(" min=\""+h.yAxis().lowerEdge()+"\"");
			  out.append(" max=\""+h.yAxis().upperEdge()+"\"");
			  out.append(" numberOfBins=\""+h.yAxis().bins()+"\"");
			  out.append("/>"); out.append(sep);
			//out.append("<statistics>"); out.append(sep);
			//out.append("<statistic name=\"Entries\" value=\""+h.entries()+"\"/>"); out.append(sep);
			//out.append("<statistic name=\"MeanX\" value=\""+h.meanX()+"\"/>"); out.append(sep);
			//out.append("<statistic name=\"RmsX\" value=\""+h.rmsX()+"\"/>"); out.append(sep);
			//out.append("<statistic name=\"MeanY\" value=\""+h.meanY()+"\"/>"); out.append(sep);
			//out.append("<statistic name=\"RmsY\" value=\""+h.rmsY()+"\"/>"); out.append(sep);
			//out.append("</statistics>"); out.append(sep);
			out.append("</data2d>"); out.append(sep);
			out.append("</dataArea>"); out.append(sep);
			out.append("</plot>"); out.append(sep);
			out.append("</plotML>"); out.append(sep);
			return out.toString();
	}
}
