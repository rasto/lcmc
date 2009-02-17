/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
* 
* Created on Mar 8, 2004
*/
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;

/**
 * A predicate that tests to see whether a specified 
 * edge is currently part of a graph.  May be used as 
 * a constraint.  <code>AbstractSparseGraph</code>
 * includes this edge constraint by default.  Should
 * not be used as a subset specification.
 * 
 * @author Joshua O'Madadhain
 */
public class NotInGraphEdgePredicate extends EdgePredicate implements UncopyablePredicate
{
    private ArchetypeGraph ag;
    private static final String message = "NotInGraphEdgePredicate: ";
    
    public NotInGraphEdgePredicate(ArchetypeGraph ag)
    {
        this.ag = ag;
    }
    
    public String toString()
    {
        return message + ag;
    }
    
    public boolean equals(Object o)
    {
        if (! (o instanceof NotInGraphEdgePredicate))
            return false;
        return ((NotInGraphEdgePredicate)o).ag.equals(ag);
    }
    
    public int hashCode()
    {
        return ag.hashCode();
    }
    
    /**
     * Returns <code>true</code> if this edge is not currently 
     * part of graph <code>ag</code>.
     */
    public boolean evaluateEdge(ArchetypeEdge e)
    {
        return (!ag.getEdges().contains(e) && (e.getGraph() == null));
    }

}
