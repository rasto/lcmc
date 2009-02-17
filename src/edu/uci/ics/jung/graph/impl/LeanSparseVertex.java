/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Mar 3, 2004
 */
package edu.uci.ics.jung.graph.impl;

import java.util.*;
import edu.uci.ics.jung.graph.*;

/**
 * This fully functional class is provided as a different sort of way to think about the creation
 * and use of Vertices, and a reminder that the user is always welcome to create
 * their own vertices. While most vertex classes keep a table from neighboring vertex to edge,
 * this one keeps a linked list.  This reduces the memory footprint, but makes 
 * <code>findEdge()</code> (as well as most other methods) require time proportional
 * to the vertex's degree.  
 * 
 * @author Joshua O'Madadhain
 */
public class LeanSparseVertex extends AbstractSparseVertex {

    protected List incident_edges;

    protected void initialize() {
        super.initialize();
        this.incident_edges = new LinkedList();
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#getNeighbors_internal()
     */
    protected Collection getNeighbors_internal() {
        Set neighbors = new HashSet();
        for (Iterator inco_it = incident_edges.iterator(); inco_it.hasNext();)
            neighbors.add(((Edge) inco_it.next()).getOpposite(this));
        return neighbors;
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#getEdges_internal()
     */
    protected Collection getEdges_internal() {
        return incident_edges;
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#addNeighbor_internal(edu.uci.ics.jung.graph.Edge,
     *      edu.uci.ics.jung.graph.Vertex)
     */
    protected void addNeighbor_internal(Edge e, Vertex v) {
        incident_edges.add(e);
    }

    /**
     * @see edu.uci.ics.jung.graph.impl.AbstractSparseVertex#removeNeighbor_internal(edu.uci.ics.jung.graph.Edge,
     *      edu.uci.ics.jung.graph.Vertex)
     */
    protected void removeNeighbor_internal(Edge e, Vertex v) {
        incident_edges.remove(e);
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#findEdgeSet(Vertex)
     */
    public Set findEdgeSet(Vertex w) {
        Set edges = new HashSet();
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e instanceof UndirectedEdge
                    || ((DirectedEdge) e).getDest() == w) edges.add(e);
        }
        return edges;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#getPredecessors()
     */
    public Set getPredecessors() {
        Set preds = new HashSet();
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e instanceof UndirectedEdge
                    || ((DirectedEdge) e).getDest() == this)
                    preds.add(e.getOpposite(this));
        }
        return preds;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#getSuccessors()
     */
    public Set getSuccessors() {
        Set succs = new HashSet();
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e instanceof UndirectedEdge
                    || ((DirectedEdge) e).getSource() == this)
                    succs.add(e.getOpposite(this));
        }
        return succs;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#getInEdges()
     */
    public Set getInEdges() {
        Set in = new HashSet();
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e instanceof UndirectedEdge
                    || ((DirectedEdge) e).getDest() == this) in.add(e);
        }
        return in;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#getOutEdges()
     */
    public Set getOutEdges() {
        Set out = new HashSet();
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e instanceof UndirectedEdge
                    || ((DirectedEdge) e).getSource() == this) out.add(e);
        }
        return out;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#inDegree()
     */
    public int inDegree() {
        return getInEdges().size();
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#outDegree()
     */
    public int outDegree() {
        return getOutEdges().size();
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#numPredecessors()
     */
    public int numPredecessors() {
        return getPredecessors().size();
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#numSuccessors()
     */
    public int numSuccessors() {
        return getSuccessors().size();
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#isSuccessorOf(edu.uci.ics.jung.graph.Vertex)
     */
    public boolean isSuccessorOf(Vertex v) {
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e.getOpposite(this) == v) {
                if (e instanceof DirectedEdge) {
                    return ((DirectedEdge) e).getDest() == this;
                } else
                    return true;
            }
        }
        return false;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#isPredecessorOf(edu.uci.ics.jung.graph.Vertex)
     */
    public boolean isPredecessorOf(Vertex v) {
        for (Iterator iter = incident_edges.iterator(); iter.hasNext();) {
            Edge e = (Edge) iter.next();
            if (e.getOpposite(this) == v) {
                if (e instanceof DirectedEdge) {
                    return ((DirectedEdge) e).getSource() == this;
                } else
                    return true;
            }
        }
        return false;
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#isSource(edu.uci.ics.jung.graph.Edge)
     */
    public boolean isSource(Edge e) {
        if (e instanceof DirectedEdge)
            return ((DirectedEdge) e).getSource() == this;
        else
            // UndirectedEdge
            return e.isIncident(this);
    }

    /**
     * @see edu.uci.ics.jung.graph.Vertex#isDest(edu.uci.ics.jung.graph.Edge)
     */
    public boolean isDest(Edge e) {
        if (e instanceof DirectedEdge)
            return ((DirectedEdge) e).getDest() == this;
        else
            // UndirectedEdge
            return e.isIncident(this);
    }
}