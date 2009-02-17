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
 * Created on Jun 23, 2003
 *
 */
package edu.uci.ics.jung.graph.decorators;

import edu.uci.ics.jung.graph.Edge;

/**
 * An EdgeColorFunction returns an <tt>int</tt>, given an <tt>Edge</tt>.
 * Is used for <tt>SettableRenderer</tt> in order to allow a variety of
 * edge thicknesses to be chosen based on whatever the user wants to use.
 * 
 * @author danyelf
 * @deprecated Use <code>EdgeStrokeFunction</code> instead.
 */
public interface EdgeThicknessFunction 
{
	public float getEdgeThickness( Edge e );
}
