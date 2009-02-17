/*
 * Created on Apr 2, 2004
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

import org.apache.commons.collections.CollectionUtils;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.predicates.ParallelEdgePredicate;

/**
 * An implementation of <code>Vertex</code> that resides in a 
 * sparse graph which may contain both directed and undirected edges.
 * It does not support parallel edges.
 * 
 * <P>
 * This implementation stores hash tables that map the successors
 * of this vertex to its outgoing edges, and its predecessors to
 * its incoming edges.  This enables an efficient implementation of
 * <code>findEdge(Vertex)</code>, but causes the routines that
 * return the sets of neighbors and of incident edges to require
 * time proportional to the number of neighbors.
 *
 * @author Joshua O'Madadhain
 */
public class SimpleSparseVertex extends AbstractSparseVertex
{
    /**
     * A map of the predecessors of this vertex to the corresponding 
     * sets of incoming edges.
     * Used to speed up <code>findEdge(Vertex)</code>.
     */
    protected Map mPredsToInEdges;

    /**
     * A map of the successors of this vertex to the corresponding 
     * sets of outgoing edges.
     * Used to speed up <code>findEdge(Vertex)</code>.
     */
    protected Map mSuccsToOutEdges;

    /**
     * A map of the vertices connected to this vertex by undirected
     * edges to the corresponding sets of edges.
     * Used to speed up <code>findEdge(Vertex)</code>.
     */
    protected Map mNeighborsToEdges;

    /**
     * Creates a new instance of a vertex for inclusion in a 
     * sparse graph.
     */
    public SimpleSparseVertex()
    {
        super();
    }

    /**
     * @see Vertex#getPredecessors()
     */
    public Set getPredecessors() {
        Collection preds = CollectionUtils.union(
            getPredsToInEdges().keySet(), 
            getNeighborsToEdges().keySet());
        
        return Collections.unmodifiableSet(new HashSet(preds));
    }

    /**
     * @see Vertex#getSuccessors()
     */
    public Set getSuccessors() {
        Collection succs = CollectionUtils.union(
            getSuccsToOutEdges().keySet(), 
            getNeighborsToEdges().keySet());
        
        return Collections.unmodifiableSet(new HashSet(succs));
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
     * @see Vertex#numPredecessors()
     */
    public int numPredecessors()
    {
        return getPredsToInEdges().size();
    }

    /**
     * @see Vertex#numSuccessors()
     */
    public int numSuccessors()
    {
        return getSuccsToOutEdges().size();
    }

    /**
     * @see Vertex#isSuccessorOf(Vertex)
     */
    public boolean isSuccessorOf(Vertex v) {
        return getPredsToInEdges().containsKey(v) || 
            getNeighborsToEdges().containsKey(v);
    }

    /**
     * @see Vertex#isPredecessorOf(Vertex)
     */
    public boolean isPredecessorOf(Vertex v) {
        return getSuccsToOutEdges().containsKey(v) ||
            getNeighborsToEdges().containsKey(v);
    }

    /**
     * @see Vertex#isSource(Edge)
     */
    public boolean isSource(Edge e) 
    {
        if (e instanceof DirectedEdge)
        {
            if (e.getGraph() == this.getGraph())
                return (this == ((DirectedEdge)e).getSource());
            else
                return false;
        }
        else if (e instanceof UndirectedEdge)
            return isIncident(e);
        else 
            throw new IllegalArgumentException("Edge is neither directed nor undirected");
    }

    /**
     * @see Vertex#isDest(Edge)
     */
    public boolean isDest(Edge e) 
    {
        if (e instanceof DirectedEdge)
        {
            if (e.getGraph() == this.getGraph())
                return (this == ((DirectedEdge)e).getDest());
            else
                return false;
        }
        else if (e instanceof UndirectedEdge)
            return isIncident(e);
        else 
            throw new IllegalArgumentException("Edge is neither directed nor undirected");
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#getInEdges()
     */
    public Set getInEdges()
    {
        Collection inEdges = getPredsToInEdges().values();
        Collection adjacentEdges = getNeighborsToEdges().values();
        
        Set edges = new HashSet();
        if (inEdges != null)
            edges.addAll(inEdges);
        if (adjacentEdges != null)
            edges.addAll(adjacentEdges);
        
        return Collections.unmodifiableSet(edges);
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#getOutEdges()
     */
    public Set getOutEdges()
    {
        Collection outEdges = getSuccsToOutEdges().values();
        Collection adjacentEdges = getNeighborsToEdges().values();
        
        Set edges = new HashSet();
        if (outEdges != null)
            edges.addAll(outEdges);
        if (adjacentEdges != null)
            edges.addAll(adjacentEdges);
        
        return Collections.unmodifiableSet(edges);
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#findEdge(Vertex)
     */
    public Edge findEdge(Vertex v)
    {
        Edge e = (Edge)getSuccsToOutEdges().get(v);
        if (e != null)
            return e;
        e = (Edge)getNeighborsToEdges().get(v);
        return e;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#findEdgeSet(Vertex)
     */
    public Set findEdgeSet(Vertex v)
    {
        Set s = new HashSet();
        Edge d = (Edge)getSuccsToOutEdges().get(v);
        Edge u = (Edge)getNeighborsToEdges().get(v);
        if (d != null)
            s.add(d);
        if (u != null)
            s.add(u);
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
        neighbors.addAll(getNeighborsToEdges().keySet());
        return neighbors;
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
     * Returns a map from the successors of this vertex to its outgoing
     * edges.  If this map has not yet been created, it creates it.
     * This method should not be directly accessed by users.
     */
    protected Map getNeighborsToEdges() {
        if (mNeighborsToEdges == null) {
            setNeighborsToEdges(new HashMap(5));
        }
        return mNeighborsToEdges;
    }

    /**
     * Sets this vertex's internal successor -> out-edge map to
     * the specified map <code>succsToOutEdges</code>.
     * This method should not be directly accessed by users.
     */
    protected void setNeighborsToEdges(Map neighborsToEdges) {
        this.mNeighborsToEdges = neighborsToEdges;
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
        setNeighborsToEdges(null);
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#getEdges_internal()
     */
    protected Collection getEdges_internal()
    {
        HashSet edges = new HashSet();

        Collection inEdges = getPredsToInEdges().values();
        Collection outEdges = getSuccsToOutEdges().values();
        Collection adjacentEdges = getNeighborsToEdges().values();
        
        if (inEdges != null)
            edges.addAll(inEdges);
        if (outEdges != null)
            edges.addAll(outEdges);
        if (adjacentEdges != null)
            edges.addAll(adjacentEdges);
        
        return edges;
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#addNeighbor_internal(edu.uci.ics.jung.graph.Edge, edu.uci.ics.jung.graph.Vertex)
     */
    protected void addNeighbor_internal(Edge e, Vertex v)
    {
        if (ParallelEdgePredicate.getInstance().evaluate(e))
            throw new IllegalArgumentException("This vertex " + 
                    "implementation does not support parallel edges");

        if (e instanceof DirectedEdge)
        {
            DirectedEdge de = (DirectedEdge) e;
            boolean added = false;
            if (this == de.getSource())
            {
                getSuccsToOutEdges().put(v, e);
                added = true;
            }
            if (this == de.getDest())
            {
                getPredsToInEdges().put(v, e);
                added = true;
            }
            if (!added)
                throw new IllegalArgumentException("Internal error: " + 
                    "this vertex is not incident to " + e);
        }
        else if (e instanceof UndirectedEdge)
        {   
            getNeighborsToEdges().put(v, e);
        }
        else throw new IllegalArgumentException("Edge is neither directed" +
            "nor undirected");
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#removeNeighbor_internal(edu.uci.ics.jung.graph.Edge, edu.uci.ics.jung.graph.Vertex)
     */
    protected void removeNeighbor_internal(Edge e, Vertex v)
    {
        String error = "Internal error: " + 
        "edge " + e + " not incident to vertex ";
        if (e instanceof DirectedEdge)
        {
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
        else if (e instanceof UndirectedEdge)
        {
            Map nte = getNeighborsToEdges();
            if (nte.get(v) == e)
            {
                nte.remove(v);
            }
            else
            {
                // if this is not a self-loop or a fatal error
                if (this != v)
                    throw new FatalException(error + v);
            }
        }
        else
            throw new IllegalArgumentException("Edge is neither directed" +
                "nor undirected");
    }
}
