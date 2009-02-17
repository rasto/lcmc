/*
 * Created on Apr 27, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.Hyperedge;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.Hypervertex;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * A basic implementation of <code>Hypergraph</code>.  Edges
 * and vertices are stored as <code>Set</code>s.
 * 
 * @author Joshua O'Madadhain
 */
public class SetHypergraph extends AbstractArchetypeGraph
        implements Hypergraph
{
    protected Set edges;
    protected Set vertices;
    
    /**
     * 
     */
    public SetHypergraph()
    {
        super();
        initialize();
    }

    public void initialize()
    {
        edges = new HashSet();
        vertices = new HashSet();
        super.initialize();
    }
    
    /**
     * @see edu.uci.ics.jung.graph.Hypergraph#addVertex(edu.uci.ics.jung.graph.Hypervertex)
     */
    public Hypervertex addVertex(Hypervertex v)
    {
        checkConstraints(v, vertex_requirements);
        
        if (v instanceof AbstractElement) 
        {
            AbstractElement ae = (AbstractElement) v;
            ae.checkIDs(mVertexIDs);
            ae.addGraph_internal(this);
        }
        vertices.add(v);
        mGraphListenerHandler.handleAdd( v );
        return v;
    }

    /**
     * Removes the vertex from this graph.  If the vertex is an instance of 
     * <code>AbstractElement</code>, notifies it that it 
     * has been removed.  Disconnects this vertex from any hyperedges to which
     * it may be connected.
     */
    public void removeVertex(Hypervertex v) 
    {
        if (v.getGraph() != this)
            throw new IllegalArgumentException("This vertex is not in this graph");
        
        Set v_edges = new HashSet(v.getIncidentEdges());
        for (Iterator iter = v_edges.iterator(); iter.hasNext(); )
            v.disconnectEdge((Hyperedge)iter.next());
        
        if (v instanceof AbstractElement) 
        {
            AbstractElement ae = (AbstractElement) v;
            ae.removeGraph_internal();
            mVertexIDs.remove(new Integer(ae.getID()));
        }
        
        vertices.remove(v);
        mGraphListenerHandler.handleRemove( v );
    }
    
    /**
     * @see edu.uci.ics.jung.graph.Hypergraph#addEdge(edu.uci.ics.jung.graph.Hyperedge)
     */
    public Hyperedge addEdge(Hyperedge e)
    {
        checkConstraints(e, edge_requirements);
        
        if (e instanceof AbstractElement) 
        {
            AbstractElement ae = (AbstractElement) e;
            ae.checkIDs(mEdgeIDs);
            ae.addGraph_internal(this);
        }
        edges.add(e);
        mGraphListenerHandler.handleAdd( e );
        return e;
    }

    /**
     * Removes the edge from this graph.  If the edge is an instance of 
     * <code>AbstractElement</code>, notifies it that it 
     * has been removed.
     */
    public void removeEdge(Hyperedge e)
    {
        if (e.getGraph() != this)
            throw new IllegalArgumentException("This edge is not in this graph");

        Set e_vertices = new HashSet(e.getIncidentVertices());
        for (Iterator iter = e_vertices.iterator(); iter.hasNext(); )
            e.disconnectVertex((Hypervertex)iter.next());
        
        if (e instanceof AbstractElement) 
        {
            AbstractElement ae = (AbstractElement)e;
            ae.removeGraph_internal();
            mEdgeIDs.remove(new Integer(ae.getID()));
        }

        edges.remove(e);
        mGraphListenerHandler.handleRemove( e );
    }
    
    /**
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#getVertices()
     */
    public Set getVertices()
    {
        return Collections.unmodifiableSet(vertices);
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeGraph#getEdges()
     */
    public Set getEdges()
    {
        return Collections.unmodifiableSet(edges);
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
