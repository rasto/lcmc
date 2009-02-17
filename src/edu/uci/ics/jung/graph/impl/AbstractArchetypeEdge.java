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
public abstract class AbstractArchetypeEdge extends AbstractElement implements
        ArchetypeEdge
{

    public Set getIncidentElements()
    {
        return getIncidentVertices();
    }
    
    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#getEqualEdge(edu.uci.ics.jung.graph.ArchetypeGraph)
     */
    public ArchetypeEdge getEqualEdge(ArchetypeGraph ag)
    {
        if (ag instanceof AbstractArchetypeGraph)
        {
            AbstractArchetypeGraph aag = (AbstractArchetypeGraph)ag;
            return aag.getEdgeByID(this.getID());
        }
        else 
            return null;
    }
    
    /**
     * @deprecated As of version 1.4, renamed to getEqualEdge(ag).
     */
    public ArchetypeEdge getEquivalentEdge(ArchetypeGraph ag)
    {
        return getEqualEdge(ag);
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#numVertices()
     */
    public int numVertices()
    {
        return getIncidentVertices().size();
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#isIncident(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public boolean isIncident(ArchetypeVertex v)
    {
        return getIncidentVertices().contains(v);
    }

    /**
     * @see edu.uci.ics.jung.graph.ArchetypeEdge#copy(edu.uci.ics.jung.graph.ArchetypeGraph)
     */
    public ArchetypeEdge copy(ArchetypeGraph g)
    {
        if (g == this.getGraph())
            throw new IllegalArgumentException("Source and destination " + 
                "graphs must be different");
        
        for (Iterator iter = getIncidentVertices().iterator(); iter.hasNext(); )
        {
            ArchetypeVertex av = (ArchetypeVertex)iter.next();
            if (av.getEqualVertex(g) == null)
                throw new IllegalArgumentException("Cannot create edge: " +
                        "source edge's incident vertex " + av + "has no equivalent " +
                        "in target graph");
        }

        AbstractArchetypeEdge e;
        try 
        {
            e = (AbstractArchetypeEdge)this.clone();
        }
        catch (CloneNotSupportedException cnse)
        {
            throw new FatalException("Can't copy edge " + this, cnse); 
        }
        e.initialize();
        e.importUserData(this);
        return e;
    }

    /**
     * Returns <code>true</code> if <code>o</code> is an instance of
     * <code>ArchetypeEdge</code> that is equivalent to this edge.
     * Respects the edge
     * equivalences which are established by <code>copy()</code> and
     * referenced by <code>getEquivalentEdge()</code>.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     * @see ArchetypeEdge#getEqualEdge(ArchetypeGraph)
     * @see ArchetypeEdge#copy
     */
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ArchetypeEdge))
            return false;
        ArchetypeEdge e = (ArchetypeEdge) o;
        return (this == e.getEqualEdge(this.getGraph()));
    }
}
