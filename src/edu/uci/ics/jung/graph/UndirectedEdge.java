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
 * A tagging interface for implementations of <code>Edge</code> that do not
 * impose an ordering on their incident vertices.
 * 
 * An undirected edge <code>e</code> is an unordered pair of 
 * vertices <code>(v1, v2)</code> which connects 
 * <code>v1</code> to <code>v2</code>.  Each of <code>v1</code> and 
 * <code>v2</code> are considered to be both source and destination of
 * <code>e</code>; equivalently, <code>e</code> is both an outgoing edge
 * and an incoming edge of each vertex.
 * 
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 * @author Scott White
 * 
 * @see UndirectedGraph
 * @see DirectedEdge
 * @see Vertex
 */
public interface UndirectedEdge extends Edge {
}
