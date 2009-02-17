/*
 * Copyright (c) 2004, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Jun 15, 2004
 */
package edu.uci.ics.jung.graph.predicates;

import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.DirectedEdge;

/**
 * Returns <code>true</code> if and only if this edge is
 * a <code>DirectedEdge</code> that has an antiparallel 
 * <code>DirectedEdge</code> in this graph.  Two directed 
 * edges are antiparallel to one another if one edge's 
 * source is the other's destination, and vice versa.
 *  
 * @author Joshua O'Madadhain
 */
public class ReciprocatedDirectedEdgePredicate extends EdgePredicate
{
    private static ReciprocatedDirectedEdgePredicate instance;
    private static final String message = "ReciprocatedDirectedEdgePredicate";
    
    protected ReciprocatedDirectedEdgePredicate()
    {
        super();
    }
    
    public static ReciprocatedDirectedEdgePredicate getInstance()
    {
        if (instance == null)
            instance = new ReciprocatedDirectedEdgePredicate();
        return instance;
    }

    public String toString()
    {
        return message;
    }

    /**
     * @see edu.uci.ics.jung.graph.predicates.EdgePredicate#evaluateEdge(edu.uci.ics.jung.graph.ArchetypeEdge)
     */
    public boolean evaluateEdge(ArchetypeEdge e)
    {
        if (!(e instanceof DirectedEdge))
            return false;
        DirectedEdge de = (DirectedEdge)e;
        
        // get set of edges going the other direction...
        Set edges = de.getDest().findEdgeSet(de.getSource());
        for (Iterator iter = edges.iterator(); iter.hasNext(); )
        {
            // if any of these edges is directed, then the connection is 
            // reciprocated; return true
            if (iter.next() instanceof DirectedEdge)
                return true;
        }
        return false;
    }

}
