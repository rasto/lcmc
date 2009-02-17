/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.decorators;

import java.awt.Color;

import edu.uci.ics.jung.graph.Edge;

/**
 * An interface for classes that return a <code>Color</code> given an 
 * <code>Edge</code>.
 * Used by <code>PluggableRenderer</code> to specify the color in
 * which to render each edge.
 * 
 * @deprecated Use EdgePaintFunction instead
 */
public interface EdgeColorFunction {
	
	public Color getEdgeColor( Edge e );

}
