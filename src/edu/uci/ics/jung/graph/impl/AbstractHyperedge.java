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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Hyperedge;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.Hypervertex;

/**
 * This class provides a skeletal implementation of the <code>Hyperedge</code>
 * interface to minimize the effort required to implement this interface.
 * <P>
 * This class extends <code>UserData</code>, which provides storage and
 * retrieval mechanisms for user-defined data for each edge instance.
 * This allows users to attach data to edges without having to extend
 * this class.
 *
 * @author Joshua O'Madadhain
 *
 * @see SetHypergraph
 * @see AbstractHypervertex
 */
public abstract class AbstractHyperedge extends AbstractArchetypeEdge implements
        Hyperedge
{
    /**
     * The next edge ID.
     */
    private static int nextGlobalEdgeID = 0;
    
    public AbstractHyperedge()
    {
        super();
        this.id = nextGlobalEdgeID++;
        initialize();
    }
    
    protected void initialize()
    {
        super.initialize();
    }
    
    /**
     * Connects <code>hv1</code> to this edge and vice versa.  If <code>hv1</code> is already
     * incident to this edge, returns <code>false</code>; otherwise, returns <code>true</code>. 
     * Throws <code>IllegalArgumentException</code> if this edge is an 
     * orphan, or if <code>hv1</code> is either an orphan or part of a different graph than this edge.
     *  
     * @see edu.uci.ics.jung.graph.Hyperedge#connectVertex(edu.uci.ics.jung.graph.Hypervertex)
     */
    public boolean connectVertex(Hypervertex hv1)
    {
        ArchetypeGraph g = this.getGraph();
        if (g == null)
            throw new IllegalArgumentException("Orphaned hyperedges may not be " +
                    "connected to (or disconnected from) vertices");
        
        if (g != hv1.getGraph())
            throw new IllegalArgumentException("Hypervertex " + hv1 + " is either orphaned" +
                    "or an element of a graph other than " + g); 
        
        if (hv1.isIncident(this))
            return false;
        
        if (hv1 instanceof AbstractHypervertex)
        {
            AbstractHypervertex av = (AbstractHypervertex)hv1;
            av.getEdges_internal().add(this);
        }
        getVertices_internal().add(hv1);
        
        return true;
    }

    /**
     * Disconnects <code>hv1</code> from this edge and vice versa.  If <code>hv1</code> is not
     * incident to this edge, returns <code>false</code>; otherwise, returns <code>true</code>.
     *  
     * @see edu.uci.ics.jung.graph.Hyperedge#disconnectVertex(edu.uci.ics.jung.graph.Hypervertex)
     */
    public boolean disconnectVertex(Hypervertex hv1)
    {
        ArchetypeGraph g = this.getGraph();
        if (g == null)
            throw new IllegalArgumentException("Orphaned hyperedges may not be " +
                    "connected to (or disconnected from) vertices");
        
        if (g != hv1.getGraph())
            throw new IllegalArgumentException("Hypervertex " + hv1 + " is either orphaned" +
                    "or an element of a graph other than " + g); 
        
        if (!hv1.isIncident(this))
            return false;

        if (hv1 instanceof AbstractHypervertex)
        {
            AbstractHypervertex av = (AbstractHypervertex)hv1;
            av.getEdges_internal().remove(this);
        }
        getVertices_internal().remove(hv1);
        
        return true;
    }

    /**
     * Creates a copy of this edge in the specified graph <code>newGraph</code>,
     * and copies this edge's user data to the new edge.  Connects this 
     *
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
     */
    public ArchetypeEdge copy(ArchetypeGraph newGraph)
    {
        Hyperedge e = (Hyperedge)super.copy(newGraph);
        ((Hypergraph)newGraph).addEdge(e);
        
        for (Iterator iter = getVertices_internal().iterator(); iter.hasNext(); )
        {
            Hypervertex v = (Hypervertex)iter.next();
            e.connectVertex((Hypervertex)v.getEqualVertex(newGraph));
        }
        return e;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#getIncidentVertices()
     */
    public Set getIncidentVertices()
    {
        return Collections.unmodifiableSet(new HashSet(getVertices_internal()));
    }
    
 
    /**
     * Returns a human-readable representation of this edge.
     *
     * @see java.lang.Object#toString()
     */
    public String toString() 
    {
        String label = "HE" + id + "(";
        for (Iterator iter = getVertices_internal().iterator(); iter.hasNext(); )
        {
            Hypervertex v = (Hypervertex)iter.next();
            label += v.toString();
            if (iter.hasNext())
                label += ",";
        }
        return label + ")";
    }

    protected abstract Collection getVertices_internal();
}
