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
 * A Hypergraph consists of hypervertices and hyperedges.
 * Hyperedges connect arbitrary Sets of hypervertices 
 * together.
 * 
 * @author danyelf
 */
public interface Hypergraph extends ArchetypeGraph {

	/**
     * Adds <code>v</code> to this graph, and returns
     * a reference to the added vertex.
	 */
	public Hypervertex addVertex(Hypervertex v);

	/**
     * Adds <code>e</code> to this graph, and returns
     * a reference to the added edge.
	 */
	public Hyperedge addEdge(Hyperedge e);

    /**
     * Removes <code>e</code> from this graph.  Throws 
     * <code>IllegalArgumentException</code> if <code>e</code> is not 
     * in this graph.
     */
    public void removeEdge(Hyperedge e);

    /**
     * Removes <code>v</code> from this graph.  Throws 
     * <code>IllegalArgumentException</code> if <code>v</code> is not 
     * in this graph.
     */
    public void removeVertex(Hypervertex v);
    
}
