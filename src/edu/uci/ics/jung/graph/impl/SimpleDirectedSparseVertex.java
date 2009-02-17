/*
 * Created on Apr 3, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.ParallelEdgePredicate;

/**
 * An implementation of <code>Vertex</code> that resides in a 
 * directed graph; none of its adjoining edges may be parallel.
 * <P>
 * This implementation stores hash tables that map the neighbors
 * of this vertex to its incident edges.  This enables an 
 * efficient implementation of <code>findEdge(Vertex)</code>.
 * Optimally, this is to be used with DirectedSparseEdge.
 * 
 * @author Joshua O'Madadhain
 * 
 * @see DirectedSparseGraph
 * @see DirectedSparseEdge
 */
public class SimpleDirectedSparseVertex extends AbstractSparseVertex
{
    /**
     * A map of the predecessors of this vertex to its incoming edges.
     * Used to speed up <code>findEdge(Vertex)</code>.
     */
    private Map mPredsToInEdges;

    /**
     * A map of the successors of this vertex to its outgoing edges.
     * Used to speed up <code>findEdge(Vertex)</code>.
     */
    private Map mSuccsToOutEdges;

    /**
     * Creates a new instance of a vertex for inclusion in a 
     * sparse directed graph.
     */
    public SimpleDirectedSparseVertex()
    {
        super();
    }

    /**
     * @see Vertex#getPredecessors()
     */
    public Set getPredecessors() {
        return Collections.unmodifiableSet(getPredsToInEdges().keySet());
    }

    /**
     * 
     * @see edu.uci.ics.jung.graph.Vertex#numPredecessors()
     */
    public int numPredecessors()
    {
        return getPredecessors().size();
    }
    
    /**
     * @see Vertex#getSuccessors()
     */
    public Set getSuccessors() {
        return Collections.unmodifiableSet(getSuccsToOutEdges().keySet());
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#numSuccessors()
     */
    public int numSuccessors()
    {
        return getSuccessors().size();
    }
    
    /**
     * @see Vertex#getInEdges()
     */
    public Set getInEdges() {
        return Collections.unmodifiableSet(new HashSet(getPredsToInEdges().values()));
    }

    /**
     * @see Vertex#getOutEdges()
     */
    public Set getOutEdges() {
        return Collections.unmodifiableSet(new HashSet(getSuccsToOutEdges().values()));
    }

    /**
     * @see Vertex#inDegree()
     */
    public int inDegree() {
        return getInEdges().size();
    }

    /**
     * @see Vertex#outDegree()
     */
    public int outDegree() {
        return getOutEdges().size();
    }

    /**
     * @see Vertex#isSuccessorOf(Vertex)
     */
    public boolean isSuccessorOf(Vertex v) {
        return getPredsToInEdges().containsKey(v);
    }

    /**
     * @see Vertex#isPredecessorOf(Vertex)
     */
    public boolean isPredecessorOf(Vertex v) {
        return getSuccsToOutEdges().containsKey(v);
    }

    /**
     * @see Vertex#isSource(Edge)
     */
    public boolean isSource(Edge e) 
    {
        if (e.getGraph() == this.getGraph())
            return equals(((DirectedEdge)e).getSource());
        else
            return false;
    }

    /**
     * @see Vertex#isDest(Edge)
     */
    public boolean isDest(Edge e) 
    {
        if (e.getGraph() == this.getGraph())
            return equals(((DirectedEdge)e).getDest());
        else
            return false;
    }

    /**
     * Returns the edge that connects this
     * vertex to the specified vertex <code>v</code>, or
     * <code>null</code> if there is no such edge.
     * Implemented using a hash table for a performance
     * improvement over the implementation in 
     * <code>AbstractSparseVertex</code>.
     * 
     * @see Vertex#findEdge(Vertex)
     */
    public Edge findEdge(Vertex v)
    {
        return (Edge)getSuccsToOutEdges().get(v);
    }
    
    /**
     * Returns the set of edges that connect this vertex to the
     * specified vertex.  Since this implementation does not allow
     * for parallel edges, this implementation simply returns a
     * set whose contents consist of the return value from 
     * <code>findEdge(v)</code>.
     * 
     * @see Vertex#findEdgeSet(Vertex)
     */
    public Set findEdgeSet(Vertex v)
    {
        Set s = new HashSet();
        Edge e = findEdge(v);
        if (e != null)
            s.add(e);
        return Collections.unmodifiableSet(s);
    }

    /**
     * Returns a set of all neighbors attached to this vertex.
     * Requires time proportional to the number of neighbors.
     * 
     * @see AbstractSparseVertex#getNeighbors_internal()
     */
    protected Collection getNeighbors_internal()
    {
        HashSet neighbors = new HashSet();
        neighbors.addAll(getPredsToInEdges().keySet());
        neighbors.addAll(getSuccsToOutEdges().keySet());
        return neighbors;
    }

    /**
     * Returns a list of all incident edges of this vertex.
     * Requires time proportional to the number of incident edges.
     *  
     * @see AbstractSparseVertex#getEdges_internal()
     */
    protected Collection getEdges_internal() {
        HashSet edges = new HashSet();
        edges.addAll(getPredsToInEdges().values());
        edges.addAll(getSuccsToOutEdges().values());
        return edges;
    }

    /**
     * @see AbstractSparseVertex#addNeighbor_internal(Edge, Vertex)
     */
    protected void addNeighbor_internal(Edge e, Vertex v)
    {
        if (! (e instanceof DirectedEdge))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation only accepts directed edges");
        
        if (ParallelEdgePredicate.getInstance().evaluate(e))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation does not support parallel edges");
        
        DirectedEdge edge = (DirectedEdge) e;
        boolean added = false;
        if (this == edge.getSource())
        {
            getSuccsToOutEdges().put(v, e);
            added = true;
        }
        if (this == edge.getDest())
        {
            getPredsToInEdges().put(v, e);
            added = true;
        }
        if (!added)
            throw new IllegalArgumentException("Internal error: " + 
                "this vertex is not incident to " + e);
    }

    /**
     * @see AbstractSparseVertex#removeNeighbor_internal(Edge, Vertex)
     */
    protected void removeNeighbor_internal(Edge e, Vertex v)
    {
        String error = "Internal error: " + 
            "edge " + e + " not incident to vertex ";
        if (getSuccsToOutEdges().containsKey(v) && v.isDest(e))
        { // e is an outgoing edge of this vertex -> v is a successor
            if (getSuccsToOutEdges().remove(v) == null)
                throw new FatalException(error + v);
        }
        else if (getPredsToInEdges().containsKey(v) && v.isSource(e))
        { // e is an incoming edge of this vertex -> v is a predecessor
            if (getPredsToInEdges().remove(v) == null)
                throw new FatalException(error + v);
        }
        else
            throw new FatalException(error + this);
    }

    /**
     * Returns a map from the predecessors of this vertex to its incoming
     * edges.  If this map has not yet been created, it creates it.
     * This map should not be directly accessed by users.
     */
    protected Map getPredsToInEdges() {
        if (mPredsToInEdges == null) {
            setPredsToInEdges(new HashMap(5));
        }
        return mPredsToInEdges;
    }

    /**
     * Sets this vertex's internal predecessor -> in-edge map to
     * the specified map <code>predsToInEdges</code>.
     * This method should not be directly accessed by users.
     */
    protected void setPredsToInEdges(Map predsToInEdges) {
        this.mPredsToInEdges = predsToInEdges;
    }

    /**
     * Returns a map from the successors of this vertex to its outgoing
     * edges.  If this map has not yet been created, it creates it.
     * This method should not be directly accessed by users.
     */
    protected Map getSuccsToOutEdges() {
        if (mSuccsToOutEdges == null) {
            setSuccsToOutEdges(new HashMap(5));
        }
        return mSuccsToOutEdges;
    }

    /**
     * Sets this vertex's internal successor -> out-edge map to
     * the specified map <code>succsToOutEdges</code>.
     * This method should not be directly accessed by users.
     */
    protected void setSuccsToOutEdges(Map succsToOutEdges) {
        this.mSuccsToOutEdges = succsToOutEdges;
    }

    /**
     * Initializes the internal data structures of this vertex.
     * 
     * @see AbstractSparseVertex#initialize()
     */
    protected void initialize() {
        super.initialize();
        setPredsToInEdges(null);
        setSuccsToOutEdges(null);
    }

}
