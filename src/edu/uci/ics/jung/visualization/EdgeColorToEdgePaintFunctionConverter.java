/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.Paint;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.decorators.AbstractEdgePaintFunction;
import edu.uci.ics.jung.graph.decorators.EdgeColorFunction;

/**
 * @author danyelf
 * @deprecated This utility class converts an EdgeColorFunction into an EdgePaintFunction
 */
public class EdgeColorToEdgePaintFunctionConverter extends AbstractEdgePaintFunction {

	private EdgeColorFunction ecf;

	/**
	 * @param ecf
	 */
	public EdgeColorToEdgePaintFunctionConverter(EdgeColorFunction ecf) {
		this.ecf = ecf;
	}

	/**
	 * @see edu.uci.ics.jung.graph.decorators.EdgePaintFunction#getDrawPaint(edu.uci.ics.jung.graph.Edge)
	 */
	public Paint getDrawPaint(Edge e) {
		return ecf.getEdgeColor(e);
	}
}
