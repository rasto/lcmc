/*
 * Created on Mar 29, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.predicates;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.OnePredicate;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;


/**
 * An edge predicate that passes <code>Edge</code>s whose endpoints 
 * satisfy distinct elements of the Predicate collection passed in as 
 * a parameter to the constructor.  May be used as an edge constraint.
 * 
 * @author Joshua O'Madadhain
 *
 */
public class KPartiteEdgePredicate extends EdgePredicate
{
    private Collection vertex_partitions;
    private Predicate mutex;
    private static String message = "KPartiteEdgePredicate";
    
    public KPartiteEdgePredicate(Collection vertex_partitions)
    {
        // used to make sure that each vertex satisfies only one predicate
    		mutex = OnePredicate.getInstance(vertex_partitions);
        this.vertex_partitions = vertex_partitions;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.predicates.EdgePredicate#evaluateEdge(edu.uci.ics.jung.graph.ArchetypeEdge)
     */
    public boolean evaluateEdge(ArchetypeEdge edge)
    {
        Edge e = (Edge)edge;
        Pair endpoints = e.getEndpoints();
        Vertex v1 = (Vertex)endpoints.getFirst();
        Vertex v2 = (Vertex)endpoints.getSecond();
        Predicate p1 = getSatisfyingPredicate(v1);
        Predicate p2 = getSatisfyingPredicate(v2);
        
        return (mutex.evaluate(v1) && mutex.evaluate(v2) &&
                p1 != null && p2 != null && (p1 != p2));
    }
    
    public String toString()
    {
        return message;
    }

    public boolean equals(Object o) 
    {
        if (! (o instanceof KPartiteEdgePredicate))
            return false;
        return ((KPartiteEdgePredicate)o).vertex_partitions.equals(vertex_partitions);
    }
    
    public int hashCode()
    {
        return vertex_partitions.hashCode();
    }
    
    private Predicate getSatisfyingPredicate(Vertex v)
    {
        for (Iterator p_iter = vertex_partitions.iterator(); p_iter.hasNext(); )
        {
            Predicate p = (Predicate)p_iter.next();
            if (p.evaluate(v))
                return p;
        }
        return null;
    }
    
}
