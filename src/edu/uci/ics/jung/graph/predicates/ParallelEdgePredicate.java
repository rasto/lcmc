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

import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * <p>A predicate that checks to see whether the specified edge
 * is parallel to any other edge.  A negation of this predicate 
 * may be used as an edge constraint that will prevent the 
 * constrained graph from accepting parallel edges.  This 
 * predicate is probably not appropriate for use as a subset 
 * specification.</p>
 * 
 * <p>Two distinct edges are considered to be <i>parallel</i> to one another
 * if the following conditions hold:
 * <ul>
 * <li/>the edges are both directed or both undirected
 * <li/>if undirected, the incident vertex sets for each edge are the same
 * <li/>if directed, the edges have the same source vertex and the same
 * destination vertex
 * </ul>
 * </p>
 * 
 * @author Joshua O'Madadhain
 */
public class ParallelEdgePredicate extends EdgePredicate
{
    private static ParallelEdgePredicate instance;
    private static final String message = "ParallelEdgePredicate";
    
    protected ParallelEdgePredicate()
    {
        super();
    }
    
    public static ParallelEdgePredicate getInstance()
    {
        if (instance == null)
            instance = new ParallelEdgePredicate();
        return instance;
    }

    public String toString()
    {
        return message;
    }
    
    /**
     * <p>Returns <code>true</code> if there exists an 
     * edge which is parallel to the specified edge.</p>
     * 
     * @see Vertex#findEdgeSet(Vertex)
     */
    public boolean evaluateEdge(ArchetypeEdge ae)
    {
        Edge e = (Edge)ae;
        Pair endpoints = e.getEndpoints();
        Vertex u = (Vertex)(endpoints.getFirst());
        Vertex v = (Vertex)(endpoints.getSecond());
        Set s = u.findEdgeSet(v);
        if (isDirected(e))
            return evaluateDirectedEdge((DirectedEdge)e, s.iterator());
        else
            return evaluateUndirectedEdge((UndirectedEdge)e, s.iterator());
    }
    
    protected boolean evaluateDirectedEdge(DirectedEdge de, Iterator s_iter)
    {
        while (s_iter.hasNext())
        {
            Edge f = (Edge)s_iter.next();
            if (de != f && isDirected(f))
            {
                DirectedEdge df = (DirectedEdge)f;
                if ((df.getSource() == de.getSource()) &&
                    (df.getDest() == de.getDest()))
                    return true;
            }
        }
        return false;
    }        
    
    protected boolean evaluateUndirectedEdge(UndirectedEdge ue, Iterator s_iter)
    {
        while (s_iter.hasNext())
        {
            Edge f = (Edge)s_iter.next();
            if (ue != f && !isDirected(f))
            {
                if (f.getIncidentVertices().equals(ue.getIncidentVertices()))
                    return true;
            }
        }
        return false;
    }
    
    protected boolean isDirected(Edge e)
    {
        if (e instanceof DirectedEdge)
            return true;
        else if (e instanceof UndirectedEdge)
            return false;
        else
            throw new IllegalArgumentException(e + "is neither directed nor undirected");
    }
}
