/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
* 
*  Created on Jun 23, 2003
*/
package edu.uci.ics.jung.graph.decorators;

import java.awt.Color;

import edu.uci.ics.jung.graph.Vertex;

/**
 * An interface for classes that return <code>Color</code>s given a 
 * <code>Vertex</code>.
 * Used by <code>PluggableRenderer</code> to specify the colors in
 * which to render each vertex.
 * @deprecated Superseded by VertexPaintFunction
 * @author danyelf
 */
public interface VertexColorFunction 
{
	/**
	 * Returns the <code>Color</code> to use for drawing the border and 
     * text for the vertex <code>v</code>.
	 */
	public Color getForeColor(Vertex v);

	/**
     * Returns the <code>Color</code> to use for drawing the interior 
     * of the vertex <code>v</code>.
	 */
	public Color getBackColor(Vertex v);
}
