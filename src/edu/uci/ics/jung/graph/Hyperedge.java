/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Dec 28, 2003
 */
package edu.uci.ics.jung.graph;


/**
 * An element of a Hypergraph that
 * connects to zero or more Hypervertices.
 * Note that two different Hyperedges are 
 * NOT equal, even if they point to the same
 * set of vertices.  Also note that Hyperedge
 * is mutable; it is possible to add and remove
 * vertices from the edge.
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 */
public interface Hyperedge extends ArchetypeEdge 
{
	/**
     * Connects <code>hv1</code> to this hyperedge and vice versa.
     * Does not affect <code>hv1</code>'s membership in the graph.
     * Equivalent to calling <code>hv1.connectEdge(this)</code>.
	 */
	public boolean connectVertex(Hypervertex hv1);

    /**
     * Disconnects <code>hv1</code> from this hyperedge and vice versa.  
     * Does not affect <code>hv1</code>'s membership in the graph.
     * Equivalent to calling <code>hv1.disconnectEdge(this)</code>.
     */
    public boolean disconnectVertex(Hypervertex hv1); 
}
