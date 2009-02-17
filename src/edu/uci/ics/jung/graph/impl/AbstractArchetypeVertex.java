/*
 * Created on Apr 26, 2005
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

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * 
 * @author Joshua O'Madadhain
 */
public abstract class AbstractArchetypeVertex extends AbstractElement implements ArchetypeVertex
{
    /**
     * 
     */
    public AbstractArchetypeVertex()
    {
        super();
        initialize();
    }

    /**
     * @see edu.uci.ics.jung.graph.Element#getIncidentElements()
     */
    public Set getIncidentElements()
    {
        return getIncidentEdges();
    }
    
    /**
     * @see ArchetypeVertex#getNeighbors()
     */
    public Set getNeighbors() {
        return Collections.unmodifiableSet(new HashSet(getNeighbors_internal()));
    }

    /**
     * 
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#numNeighbors()
     */
    public int numNeighbors() {
        return getNeighbors_internal().size();
    }
    
    /**
     * @see ArchetypeVertex#getIncidentEdges()
     */
    public Set getIncidentEdges() {
        return Collections.unmodifiableSet(new HashSet(getEdges_internal()));
    }

    /**
     * @see ArchetypeVertex#degree()
     */
    public int degree() {
        return getEdges_internal().size();
    }
    
    /**
     * @see ArchetypeVertex#isNeighborOf(ArchetypeVertex)
     */
    public boolean isNeighborOf(ArchetypeVertex v) {
        return getNeighbors_internal().contains(v);
    }

    /**
     * @see ArchetypeVertex#isIncident(ArchetypeEdge)
     */
    public boolean isIncident(ArchetypeEdge e) {
        return getEdges_internal().contains(e);
    }
    

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
     */
    public ArchetypeVertex copy(ArchetypeGraph g)
    {
        if (g == this.getGraph())
            throw new IllegalArgumentException("Source and destination graphs "
                    + "must be different");

        try
        {
            AbstractArchetypeVertex v = (AbstractArchetypeVertex) clone();
            v.initialize();
            v.importUserData(this);
            return v;
        }
        catch (CloneNotSupportedException cne)
        {
            throw new FatalException("Failure in cloning " + this, cne);
        }
    }

    /**
     * Returns <code>true</code> if <code>o</code> is an instance of
     * <code>ArchetypeVertex</code> that is equivalent to this vertex.
     * Respects the vertex
     * equivalences which are established by <code>copy()</code> and
     * referenced by <code>getEquivalentVertex()</code>.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     * @see ArchetypeVertex#getEqualVertex(ArchetypeGraph)
     * @see ArchetypeVertex#copy
     */
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof ArchetypeVertex))
            return false;
        ArchetypeVertex v = (ArchetypeVertex)o;
        return (this == v.getEqualVertex(this.getGraph()));
    }

    /**
     * Returns the vertex in the specified graph <code>ag</code>
     * that is equivalent to this vertex.  If there is no
     * such vertex, or if <code>ag</code> is not an instance
     * of <code>AbstractSparseGraph</code>, returns <code>null</code>.
     *
     * @see ArchetypeVertex#getEqualVertex(ArchetypeGraph)
     */
    public ArchetypeVertex getEqualVertex(ArchetypeGraph ag)
    {
        if (ag instanceof AbstractArchetypeGraph)
        {
            AbstractArchetypeGraph aag = (AbstractArchetypeGraph)ag;
            return aag.getVertexByID(this.getID());
        }
        else
            return null;
    }

    /**
     * @deprecated As of version 1.4, renamed to getEqualVertex(ag).
     */
    public ArchetypeVertex getEquivalentVertex(ArchetypeGraph ag)
    {
        return getEqualVertex(ag);
    }
    
    
    /**
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#findEdge(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public ArchetypeEdge findEdge(ArchetypeVertex v)
    {
        for (Iterator iter = getEdges_internal().iterator(); iter.hasNext(); )
        {
            ArchetypeEdge ae = (ArchetypeEdge)iter.next();
            if (ae.isIncident(v))
                return ae;
        }
        return null;
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeVertex#findEdgeSet(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public Set findEdgeSet(ArchetypeVertex v)
    {
        Set edges = new HashSet();
        for (Iterator iter = getEdges_internal().iterator(); iter.hasNext(); )
        {
            ArchetypeEdge ae = (ArchetypeEdge)iter.next();
            if (ae.isIncident(v))
                edges.add(ae);
        }
        return Collections.unmodifiableSet(edges);
    }

    /**
     * Returns a set containing all neighbors of this vertex.  This
     * is an internal method which is not intended for users.
     */
    protected abstract Collection getNeighbors_internal();

    /**
     * Returns a set containing all the incident edges of this vertex.
     * This is an internal method which is not intended for users.
     */
    protected abstract Collection getEdges_internal();

}
