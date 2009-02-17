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

/**
 * A predicate that tests to see whether a specified edge is 
 * "simple" (neither parallel to any edge nor a self-loop).
 * This predicate may be used as an edge constraint; a graph
 * with this edge constraint will be a simple graph.
 * 
 * @author Joshua O'Madadhain
 */
public class SimpleEdgePredicate extends EdgePredicate
{
    private static SimpleEdgePredicate instance;
    private static final String message = "SimpleEdgePredicate";

    protected SimpleEdgePredicate()
    {
        super();
    }
    
    /**
     * Returns an instance of this class.
     */
    public static SimpleEdgePredicate getInstance()
    {
        if (instance == null)
            instance = new SimpleEdgePredicate();
        return instance;
    }

    public String toString()
    {
        return message;
    }
    
    /**
     * Returns <code>true</code> if <code>ae</code> is neither a 
     * self-loop nor parallel to an existing edge.
     */
    public boolean evaluateEdge(ArchetypeEdge ae)
    {
        EdgePredicate parallel = ParallelEdgePredicate.getInstance();
        EdgePredicate self_loop = SelfLoopEdgePredicate.getInstance();
        
        return (!parallel.evaluateEdge(ae) && !self_loop.evaluateEdge(ae));
    }

}
