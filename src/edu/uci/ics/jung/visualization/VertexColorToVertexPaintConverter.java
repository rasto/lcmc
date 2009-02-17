/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Mar 31, 2005 by danyelf
 */
package edu.uci.ics.jung.visualization;

import java.awt.Paint;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.VertexColorFunction;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;

/**
 * This class converts a VertexColorFunction to a VertexPaintFunction
 * 
 * @author danyelf
 * @deprecated You should create a new VertexPaintFunction if you can
 */
public class VertexColorToVertexPaintConverter implements VertexPaintFunction {

	protected VertexColorFunction vcf;

	/**
	 * @param vcf
	 */
	public VertexColorToVertexPaintConverter(VertexColorFunction vcf) {
		this.vcf = vcf;
	}

	public Paint getFillPaint(Vertex v) {
		return vcf.getBackColor(v);
	}

	/* (non-Javadoc)
	 * @see edu.uci.ics.jung.graph.decorators.VertexPaintFunction#getForePaint(edu.uci.ics.jung.graph.Vertex)
	 */
	public Paint getDrawPaint(Vertex v) {
		return vcf.getForeColor(v);
	}

}
