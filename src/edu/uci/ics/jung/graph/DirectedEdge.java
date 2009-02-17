/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph;

/**
 * A type of <code>Edge</code> which imposes an ordering on its incident
 * vertices.  A directed edge <code>e</code> is an ordered pair of 
 * vertices <code>&lt;v1, v2&gt;</code> which connects its <b>source</b>,
 * <code>v1</code>, to its <b>destination</b>, <code>v2</code>.
 * Equivalently, <code>e</code> is an outgoing edge of <code>v1</code>
 * and an incoming edge of <code>v2</code>.
 * 
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 * @author Scott White
 * 
 * @see DirectedGraph
 * @see UndirectedEdge
 * @see Vertex
 */
public interface DirectedEdge extends Edge 
{
    /**
     * Returns the source of this directed edge.  
     * 
     * @see Vertex#isSource(Edge)
     */
	public Vertex getSource();

    /**
     * Returns the destination of this directed edge.
     * 
     * @see Vertex#isDest(Edge)
     */
	public Vertex getDest();
}
