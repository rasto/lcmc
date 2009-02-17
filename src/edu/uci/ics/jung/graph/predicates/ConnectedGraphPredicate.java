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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * 
 * @author Joshua O'Madadhain
 */
public class ConnectedGraphPredicate extends GraphPredicate
{
    private static ConnectedGraphPredicate instance;
    private static String message = "connected graph predicate";
    
    /**
     * Returns an instance of this class.
     */
    public static ConnectedGraphPredicate getInstance()
    {
        if (instance == null)
            instance = new ConnectedGraphPredicate();
        return instance;
    }
    
    protected ConnectedGraphPredicate()
    {
        super();
    }
    
    public String toString()
    {
        return message;
    }
    
    /**
     * Returns <code>true</code> if there exists a path from each 
     * vertex to all other vertices (ignoring edge direction).
     * 
     * <p>Returns <code>true</code> for an empty graph.</p>
     * 
     * @see org.apache.commons.collections.Predicate#evaluate(java.lang.Object)
     */
    public boolean evaluateGraph(ArchetypeGraph graph)
    {
        Graph g = (Graph)graph;
        if (g.numVertices() == 0)
            return true;
        
        Vertex start = (Vertex)g.getVertices().iterator().next(); // pick any vertex
        Set visited = new HashSet();
        LinkedList stack = new LinkedList();
        stack.add(start);
        // traverse through graph in depth-first order
        while (!stack.isEmpty())
        {
            Vertex v = (Vertex)stack.removeFirst();
            visited.add(v);
            Set neighbors = v.getNeighbors();
            for (Iterator n_it = neighbors.iterator(); n_it.hasNext(); )
            {
                Vertex w = (Vertex)n_it.next();
                if (!visited.contains(w))
                    stack.addFirst(w);
            }
        }
        return (visited.size() == g.numVertices());
    }
}
