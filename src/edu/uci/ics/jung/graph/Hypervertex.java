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
 * connects to zero or more Hyperedges.
 * Note that two different Hypervertices are 
 * NOT equal, even if they are connected to the same
 * set of edges.  
 *  
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 */
public interface Hypervertex extends ArchetypeVertex 
{
    /**
     * Connects <code>he</code> to this hypervertex and vice versa.
     * Does not affect <code>he</code>'s membership in the graph.
     * Equivalent to calling <code>he.connectVertex(this)</code>.
     */
    public boolean connectEdge(Hyperedge he);

    /**
     * Disconnects <code>he</code> from this hypervertex and vice versa.  
     * Does not affect <code>he</code>'s membership in the graph.
     * Equivalent to calling <code>he.disconnectVertex(this)</code>.
     */
    public boolean disconnectEdge(Hyperedge he);
}
