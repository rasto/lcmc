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

import edu.uci.ics.jung.utils.Pair;

/**
 * A specific type of <code>ArchetypeEdge</code> that connects exactly 
 * two instances of <code>Vertex</code>.  Instances
 * of <code>Edge</code> may be either directed or undirected.
 * <P>
 * If either of the vertices incident to an <code>Edge</code> is removed 
 * from its graph, then this edge becomes <i>ill-formed</i> and must also 
 * be removed from the graph.
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @author Scott White
 * 
 * @see Graph
 * @see Vertex
 */
public interface Edge extends ArchetypeEdge {

	/**
	 * Returns the vertex at the opposite end of this edge from the 
     * specified vertex <code>v</code>.  Throws 
     * <code>IllegalArgumentException</code> if <code>v</code> is 
     * not incident to this edge.
     * <P>
     * For example, if this edge connects vertices <code>a</code> and
     * <code>b</code>, <code>this.getOpposite(a)</code> returns 
     * <code>b</code>.
     * 
     * @throws IllegalArgumentException
	 */
    public Vertex getOpposite(Vertex v);
	
	/**
	 * Returns a pair consisting of both incident vertices. This
	 * is equivalent to getIncidentVertices, except that it returns
	 * the data in the form of a Pair rather than a Set. This allows
	 * easy access to the two vertices. Note that the pair is in no
	 * particular order.
	 */
	Pair getEndpoints();
}
