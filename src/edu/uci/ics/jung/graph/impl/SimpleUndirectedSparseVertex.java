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
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.ParallelEdgePredicate;

/**
 * An implementation of <code>Vertex</code> that resides in a 
 * undirected graph; none of its adjoining edges may be parallel.
 * <P>
 * This implementation stores hash tables that map the neighbors
 * of this vertex to its incident edges.  This enables an 
 * efficient implementation of <code>findEdge(Vertex)</code>.
 * 
 * @author Joshua O'Madadhain
 * 
 * @see UndirectedGraph
 * @see UndirectedEdge
 */

public class SimpleUndirectedSparseVertex extends AbstractSparseVertex
{
    /**
     * A map of the neighbors of this vertex to its incident edges.
     * Used to speed up <code>findEdge(Vertex)</code>.
     */
    private Map mNeighborsToEdges;

    public SimpleUndirectedSparseVertex()
    {
        super();
    }

    /**
     * 
     * @see Vertex#getPredecessors()
     */
    public Set getPredecessors()
    {
        return Collections.unmodifiableSet(getNeighborsToEdges().keySet());
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
     * 
     * @see Vertex#getSuccessors()
     */
    public Set getSuccessors()
    {
        return Collections.unmodifiableSet(getNeighborsToEdges().keySet());
    }

    /**
     * 
     * @see edu.uci.ics.jung.graph.Vertex#numPredecessors()
     */
    public int numSuccessors()
    {
        return getSuccessors().size();
    }

    /**
     * 
     * @see Vertex#getInEdges()
     */
    public Set getInEdges()
    {
        return Collections.unmodifiableSet(new HashSet(getEdges_internal()));
    }

    /**
     * 
     * @see Vertex#getOutEdges()
     */
    public Set getOutEdges()
    {
        return Collections.unmodifiableSet(new HashSet(getEdges_internal()));
    }

    /**
     * 
     * @see Vertex#inDegree()
     */
    public int inDegree()
    {
        return getNeighborsToEdges().values().size();
    }

    /**
     * 
     * @see Vertex#outDegree()
     */
    public int outDegree()
    {
        return getNeighborsToEdges().values().size();
    }

    /**
     * @see Vertex#isSuccessorOf(Vertex)
     */
    public boolean isSuccessorOf(Vertex v)
    {
        return getNeighborsToEdges().containsKey(v);
    }

    /**
     * @see Vertex#isPredecessorOf(Vertex)
     */
    public boolean isPredecessorOf(Vertex v)
    {
        return getNeighborsToEdges().containsKey(v);
    }

    /**
     * @see Vertex#isSource(Edge)
     */
    public boolean isSource(Edge e)
    {
        return isIncident(e);
    }

    /**
     * @see Vertex#isDest(Edge)
     */
    public boolean isDest(Edge e)
    {
        return isIncident(e);
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
     * 
     */
    public Edge findEdge(Vertex v)
    {
        return (Edge) getNeighborsToEdges().get(v);
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
     * @see AbstractSparseVertex#initialize()
     */
    protected void initialize()
    {
        super.initialize();
        setNeighborsToEdges(null);
    }

    /**
     * @see AbstractSparseVertex#getNeighbors_internal()
     */
    protected Collection getNeighbors_internal()
    {
        return getNeighborsToEdges().keySet();
    }

    /**
     * @see AbstractSparseVertex#getEdges_internal()
     */
    protected Collection getEdges_internal()
    {
        return getNeighborsToEdges().values();
    }

    /**
     * @see AbstractSparseVertex#addNeighbor_internal(Edge, Vertex)
     */
    protected void addNeighbor_internal(Edge e, Vertex v)
    {
        if (ParallelEdgePredicate.getInstance().evaluate(e))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation does not support parallel edges");
        
        if (! (e instanceof UndirectedEdge))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation only accepts undirected edges");
        getNeighborsToEdges().put(v, e);
    }

    /**
     * Removes the neighbor from this vertex's internal map.
     * 
     * @see AbstractSparseVertex#removeNeighbor_internal(Edge, Vertex)
     */
    protected void removeNeighbor_internal(Edge connectingEdge, Vertex neighbor)
    {
        // does connectingEdge connect us to neighbor?
        if (connectingEdge == getNeighborsToEdges().get(neighbor))
        {
            getNeighborsToEdges().remove(neighbor);
        }
        else
        {
            // self-loop; already removed this node 
            // in a previous call to removeNeighbor_internal()
            if (this == neighbor)
                return;

            throw new FatalException("Internal error: " + "edge "
                    + connectingEdge + " not incident to vertex " + neighbor);
        }
    }

    /**
     * Returns a map from the successors of this vertex to its outgoing
     * edges.  If this map has not yet been created, it creates it.
     * This method should not be directly accessed by users.
     */
    protected Map getNeighborsToEdges()
    {
        if (mNeighborsToEdges == null)
        {
            setNeighborsToEdges(new HashMap(5));
        }
        return mNeighborsToEdges;
    }

    /**
     * Sets this vertex's internal successor -> out-edge map to
     * the specified map <code>succsToOutEdges</code>.
     * This method should not be directly accessed by users.
     */
    protected void setNeighborsToEdges(Map neighborsToEdges)
    {
        this.mNeighborsToEdges = neighborsToEdges;
    }

}