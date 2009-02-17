/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
* 
* Created on Mar 3, 2004
*/
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.utils.Pair;

/**
 * A predicate that checks to see whether a specified
 * edge is a self-loop.
 * 
 * @author Joshua O'Madadhain
 */
public class SelfLoopEdgePredicate extends EdgePredicate
{
    private static SelfLoopEdgePredicate instance;
    private static final String message = "SelfLoopEdgePredicate";

    protected SelfLoopEdgePredicate()
    {
        super();
    }
    
    /**
     * Returns an instance of this class.
     */
    public static SelfLoopEdgePredicate getInstance()
    {
        if (instance == null)
            instance = new SelfLoopEdgePredicate();
        return instance;
    }

    public String toString()
    {
        return message;
    }
    
    /**
     * Returns <code>true</code> if <code>e</code> is an
     * <code>Edge</code> that connects a vertex to itself.
     */
    public boolean evaluateEdge(ArchetypeEdge e)
    {
        Pair p = ((Edge)e).getEndpoints();
        return (p.getFirst().equals(p.getSecond()));
    }
}
