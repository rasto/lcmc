/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.PredicateUtils;

/**
 * This class provides a skeletal implementation of the <code>Graph</code>
 * interface to minimize the effort required to implement this interface. This
 * graph implementation stores vertices and edges in Sets. It is appropriate
 * for sparse graphs (those in which each vertex has only a few neighbors). For
 * dense graphs (those in which each vertex is connected to most other
 * vertices), a different implementation (for example, one which represents a
 * graph via a matrix) may be more appropriate.
 * 
 * <P>Currently, the addition and removal methods will notify their arguments that
 * they have been added to or removed from this graph only if they are
 * instances of <code>AbstractSparseVertex</code> or <code>AbstractSparseEdge</code>.</p>
 * 
 * <P>This class extends <code>UserData</code>, which provides storage and
 * retrieval mechanisms for user-defined data for each graph instance. This
 * allows users to attach data to graphs without having to extend this class.</p>
 * 
 * <p>Constraints imposed by this class:
 * <ul>
 * <li/><b>system</b> (invisible, unmodifiable) constraints: 
 * <code>NotInGraphVertexPredicate</code>, <code>NotInGraphEdgePredicate</code>
 * <li/><b>user</b> (visible, modifiable via <code>getEdgeConstraints</code>): none
 * </ul></p>

 * @author Scott D. White
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 */
public abstract class AbstractSparseGraph
	extends AbstractArchetypeGraph
	implements Graph, Cloneable {

    /**
     * The set of vertices registered with the graph.
     */
    protected Set mVertices;

    /**
     * The set of edges registered with the graph.
     */
    protected Set mEdges;
    

	//------------------------- CONSTRUCTION -----------------

	/**
	 * Creates an instance of a sparse graph. Invokes <code>initialize()</code>
	 * to set up the local data structures.
	 * 
	 * @see #initialize()
	 */
	public AbstractSparseGraph() {
        initialize();
//        super();
	}

    protected void initialize()
    {
        mVertices = new HashSet();
        mEdges = new HashSet();
        super.initialize();
    }

    /**
     * Returns an unmodifiable set view of the vertices in this graph. Will
     * reflect changes made to the graph after the view is generated.
     * 
     * @see ArchetypeGraph#getVertices()
     * @see java.util.Collections#unmodifiableSet(java.util.Set)
     */
    public Set getVertices() {
        return Collections.unmodifiableSet(mVertices);
    }

    /**
     * Returns an unmodifiable set view of the edges in this graph. Will
     * reflect changes made to the graph after the view is generated.
     * 
     * @see ArchetypeGraph#getEdges()
     * @see java.util.Collections#unmodifiableSet(java.util.Set)
     */
    public Set getEdges() {
        return Collections.unmodifiableSet(mEdges);
    }



	// --------------------------- ADDERS

    /**
     * @see edu.uci.ics.jung.graph.Graph#addEdge(edu.uci.ics.jung.graph.Edge)
     */
    public Edge addEdge(Edge e)
    {
        // needs to happen before ase.addGraph_internal() so
        // that test for e.getGraph() will be valid
        checkConstraints(e, edge_requirements);
        
        if (e instanceof AbstractElement) 
        {
            AbstractElement ae = (AbstractElement)e;
            ae.checkIDs(mEdgeIDs);
            if (ae instanceof AbstractSparseEdge)
                ((AbstractSparseEdge)ae).addGraph_internal(this);
            else
                ae.addGraph_internal(this);
        }
        mEdges.add(e);
        mGraphListenerHandler.handleAdd( e );
        return e;
    }

    /**
     * 
     * @see edu.uci.ics.jung.graph.Graph#addVertex(edu.uci.ics.jung.graph.Vertex)
     */
    public Vertex addVertex(Vertex v) 
    {
        // needs to happen before addGraph_internal() so
        // that test for v.getGraph() will be valid
        checkConstraints(v, vertex_requirements);
        
        if (v instanceof AbstractElement) 
        {
            AbstractElement ae = (AbstractElement)v;
            ae.checkIDs(mVertexIDs);
            ae.addGraph_internal(this);
        }
        mVertices.add(v);
        mGraphListenerHandler.handleAdd( v );
        return v;
    }


	// ---------------------------- ACCESSORS ---------------------------




	/**
	 * Removes all edges adjacent to the specified vertex, removes the vertex,
	 * and notifies the vertex that it has been removed. <b>Note</b>: the
	 * vertex will not be notified unless it is an instance of <code>AbstractSparseVertex</code>.
	 */
	public void removeVertex(Vertex v) {
        if (v.getGraph() != this)
            throw new IllegalArgumentException("This vertex is not in this graph");
		GraphUtils.removeEdges(this, v.getIncidentEdges());
		mVertices.remove(v);
		if (v instanceof AbstractSparseVertex) {
			AbstractSparseVertex asv = (AbstractSparseVertex) v;
			asv.removeGraph_internal();
			mVertexIDs.remove(new Integer(asv.getID()));
		}
		mGraphListenerHandler.handleRemove( v );
	}

	/**
	 * Removes the edge from the graph, and notifies that the edge and its
	 * incident vertices that it has been removed. <b>Note</b>: the edge
	 * will not be notified unless it is an instance of <code>AbstractSparseEdge</code>,
	 * and the incident vertices will not be notified unless they are instances
	 * of <code>AbstractSparseVertex</code>.
	 */
	public void removeEdge(Edge e) {
		if (e.getGraph() != this)
			throw new IllegalArgumentException("This edge is not in this graph");

		Pair endpoints = e.getEndpoints();
		Vertex from = (Vertex) endpoints.getFirst();
		Vertex to = (Vertex) endpoints.getSecond();

		if (from instanceof AbstractSparseVertex)
			((AbstractSparseVertex) from).removeNeighbor_internal(e, to);
		if (to instanceof AbstractSparseVertex)
			((AbstractSparseVertex) to).removeNeighbor_internal(e, from);

		if (e instanceof AbstractSparseEdge) {
			AbstractSparseEdge ase = (AbstractSparseEdge) e;
			ase.removeGraph_internal();
			mEdgeIDs.remove(new Integer(ase.getID()));
		}

		mEdges.remove(e);
		mGraphListenerHandler.handleRemove( e );
	}

    /**
     * @see Graph#isDirected()
     * @deprecated As of version 1.4, the semantics of this method have 
     * changed; it no longer returns a boolean value that is hardwired into 
     * the class definition, but checks to see whether one of the 
     * requirements of this graph is that it only accepts directed edges.
     * 
     */
    public boolean isDirected()
    {
        return PredicateUtils.enforcesDirected(this);
    }

    /**
     * Removes all vertices in the specified set from <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.removeVertex</code> on all elements
     * of the set.
     * If any element of <code>vertices</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any vertices that may have been removed already 
     * are not guaranteed to be restored to the graph.     
     */
    public void removeVertices(Set vertices)
    {
        GraphUtils.removeVertices(this, vertices);
    }

    /**
     * Removes all vertices in the specified set from <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.removeVertex</code> on all elements
     * of the set.
     * If any element of <code>edges</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any edges that may have been removed already 
     * are not guaranteed to be restored to the graph.  
     */
    public void removeEdges(Set edges)
    {
        GraphUtils.removeEdges(this, edges);
    }
}
