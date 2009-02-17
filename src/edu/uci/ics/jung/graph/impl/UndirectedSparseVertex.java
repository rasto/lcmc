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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;


/**
 * A vertex class for instances of <code>UndirectedGraph</code>
 * that may contain parallel edges.
 * 
 * {@link edu.uci.ics.jung.graph.UndirectedGraph}
 * @author Joshua O'Madadhain
 */
public class UndirectedSparseVertex extends SimpleUndirectedSparseVertex
{
    /**
     * Creates a new instance of a vertex for inclusion in a 
     * sparse graph.
     */
    public UndirectedSparseVertex()
    {
        super();
    }

    /**
     * Returns the edge that connects this
     * vertex to the specified vertex <code>v</code>, or
     * <code>null</code> if there is no such edge.
     * Implemented using a hash table for a performance
     * improvement over the implementation in 
     * <code>AbstractSparseVertex</code>.
     * 
     * Looks for a directed edge first, and then for an
     * undirected edge if no directed edges are found.
     * 
     * @see Vertex#findEdge(Vertex)
     */
    public Edge findEdge(Vertex v)
    {
        Set outEdges = (Set)getNeighborsToEdges().get(v);
        if (outEdges == null)
            return null;
        return (Edge)outEdges.iterator().next();
    }

    /**
     * @see Vertex#findEdgeSet(Vertex)
     */
    public Set findEdgeSet(Vertex v)
    {
        Set edgeSet = new HashSet();
        Set edges = (Set)getNeighborsToEdges().get(v);
        if (edges != null)
            edgeSet.addAll(edges);
        return Collections.unmodifiableSet(edgeSet);
    }

    /**
     * Returns a list of all incident edges of this vertex.
     * Requires time proportional to the number of incident edges.
     *  
     * @see AbstractSparseVertex#getEdges_internal()
     */
    protected Collection getEdges_internal() 
    {
        HashSet edges = new HashSet();

        Collection edgeSets = getNeighborsToEdges().values();
        
        for (Iterator e_iter = edgeSets.iterator(); e_iter.hasNext(); )
            edges.addAll((Set)e_iter.next());
        
        return edges;
    }

    /**
     * @see AbstractSparseVertex#addNeighbor_internal(Edge, Vertex)
     */
    protected void addNeighbor_internal(Edge e, Vertex v)
    {
        if (! (e instanceof UndirectedEdge))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation only accepts undirected edges");

        Map nte = getNeighborsToEdges();
        Set edges = (Set)nte.get(v);

        if (edges == null)
        {
            edges = new HashSet();
            nte.put(v, edges);
        }
        edges.add(e);
    }

    /**
     * @see AbstractSparseVertex#removeNeighbor_internal(Edge, Vertex)
     */
    protected void removeNeighbor_internal(Edge e, Vertex v)
    {
        // if v doesn't point to e, and it's not a self-loop
        // that's been removed in a previous call to removeNeighbor...
        Map nte = getNeighborsToEdges();
        Set edges = (Set)nte.get(v);
        if (edges != null)
        {
            boolean removed = edges.remove(e);
            if (edges.isEmpty())
                nte.remove(v);
            if (!removed && this != v)
                throw new FatalException("Internal error in data structure" +
                    "for vertex " + this);
        }
        else if (this != v)
            throw new FatalException("Internal error in data structure" +
                "for vertex " + this);

        // if it *is* a self-loop, we're already done
    }
}
