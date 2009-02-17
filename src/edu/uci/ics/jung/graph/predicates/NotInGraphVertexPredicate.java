/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
* 
* Created on Mar 9, 2004
*/
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A predicate that tests to see whether a specified 
 * vertex is currently part of a graph.  May be used as 
 * a constraint.  <code>AbstractSparseGraph</code>
 * includes this vertex constraint by default.  Should
 * not be used as a subset specification.
 * 
 * @author Joshua O'Madadhain
 */
public class NotInGraphVertexPredicate extends VertexPredicate implements UncopyablePredicate
{
    private ArchetypeGraph ag;
    private final static String message = "NotInGraphVertexPredicate: ";
    
    public NotInGraphVertexPredicate(ArchetypeGraph ag)
    {
        this.ag = ag;
    }

    public String toString()
    {
        return message + ag;
    }
    
    public boolean equals(Object o)
    {
        if (!(o instanceof NotInGraphVertexPredicate))
            return false;
        return ((NotInGraphVertexPredicate)o).ag.equals(ag);
    }
    
    public int hashCode()
    {
        return ag.hashCode();
    }
    
    /**
     * Returns <code>true</code> if this vertex is not currently 
     * a member of any graph.
     */
    public boolean evaluateVertex(ArchetypeVertex av)
    {
        return (!ag.getVertices().contains(av) && (av.getGraph() == null));
    }
}
