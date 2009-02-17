/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.PredicateDecorator;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;


/**
 * Convenience methods for handling Predicates in JUNG (as constraints, 
 * as subset specifications, and in general).  Not a replacement
 * for the Jakarta Commons-Collections <code>PredicateUtils</code> class.
 * 
 * @author Joshua O'Madadhain
 */
public class PredicateUtils
{
    /**
     * <p>Returns a <code>Set</code> consisting of all vertices <code>v</code> 
     * in graph <code>g</code> that satisfy predicate <code>p</code>,
     * that is, those for which <code>p.evaluate(v)</code> returns true.</p>
     * 
     * <p>If <code>g</code> has a <code>SubsetManager</code> that defines
     * a cached subset based on <code>p</code>, that subset is returned.
     */
    public static Set getVertices(ArchetypeGraph g, Predicate p)
    {
        SubsetManager sm = (SubsetManager)g.getUserDatum(ArchetypeGraph.SUBSET_MANAGER);
        if (sm != null)
        {
            Set s = sm.getVertices(p);
            if (s != null)
                return s;
        }
        
        Set s = new HashSet();
        Set vertices = g.getVertices();
        for (Iterator v_it = vertices.iterator(); v_it.hasNext(); )
        {
            ArchetypeVertex v = (ArchetypeVertex)v_it.next();
            if (p.evaluate(v))
                s.add(v);
        }
        return Collections.unmodifiableSet(s);
    }
    
    /**
     * Returns a <code>Set</code> consisting of all edges <code>e</code> 
     * in graph <code>g</code> that satisfy predicate <code>p</code>,
     * that is, those for which <code>p.evaluate(e)</code> returns true.
     */
    public static Set getEdges(ArchetypeGraph g, Predicate p)
    {
        SubsetManager sm = (SubsetManager)g.getUserDatum(ArchetypeGraph.SUBSET_MANAGER);
        if (sm != null)
        {
            Set s = sm.getEdges(p);
            if (s != null)
                return s;
        }
        
        Set s = new HashSet();
        Set edges = g.getEdges();
        for (Iterator e_it = edges.iterator(); e_it.hasNext(); )
        {
            ArchetypeEdge e = (ArchetypeEdge)e_it.next();
            if (p.evaluate(e))
                s.add(e);
        }
        return Collections.unmodifiableSet(s);
    }
    
    /**
     * Creates a vertex subset for <code>g</code> based on <code>p</code>, which will
     * be maintained by the <code>g</code>'s <code>SubsetManager</code>.
     * @param p     the predicate defining the subset
     * @return      true if a subset was created; false if the subset already existed
     */
    public static boolean addVertexSubset(ArchetypeGraph g, Predicate p)
    {
        return SubsetManager.getInstance(g).addVertexSubset(p);
    }

    /**
     * Creates an edge subset for <code>g</code> based on <code>p</code>, which will
     * be maintained by the <code>g</code>'s <code>SubsetManager</code>.
     * @param p     the predicate defining the subset
     * @return      true if a subset was created; false if the subset already existed
     */
    public static boolean addEdgeSubset(ArchetypeGraph g, Predicate p)
    {
        return SubsetManager.getInstance(g).addEdgeSubset(p);
    }
    
    /**
     * Removes the vertex subset based on <code>p</code> from 
     * <code>g</code>'s <code>SubsetManager</code>.
     * @param p     the predicate defining the subset
     */
    public static void removeVertexSubset(ArchetypeGraph g, Predicate p)
    {
        SubsetManager.getInstance(g).removeVertexSubset(p);
    }

    /**
     * Removes the edge subset based on <code>p</code> from 
     * <code>g</code>'s <code>SubsetManager</code>.
     * @param p     the predicate defining the subset
     */
    public static void removeEdgeSubset(ArchetypeGraph g, Predicate p)
    {
        SubsetManager.getInstance(g).removeEdgeSubset(p);
    }

    /**
     * Returns <code>true</code> if <code>p</code> is an edge
     * constraint of <code>g</code>, and <code>false</code> otherwise.
     */
    public static boolean enforcesEdgeConstraint(ArchetypeGraph g, Predicate p)
    {
        return g.getEdgeConstraints().contains(p);
    }

    /**
     * Returns <code>true</code> if each edge in <code>g</code>
     * satisfies <code>p</code>, and false otherwise.  (Note: this may be 
     * true even if <code>p</code> is not a constraint of <code>g</code>.)
     */
    public static boolean satisfiesEdgeConstraint(ArchetypeGraph g, Predicate p)
    {
        if (PredicateUtils.enforcesEdgeConstraint(g, p))
            return true;
        else
            return satisfiesPredicate(g.getEdges(), p);
    }

    /**
     * Returns <code>true</code> if <code>p</code> is an edge
     * constraint of <code>g</code>, and <code>false</code> otherwise.
     */
    public static boolean enforcesVertexConstraint(ArchetypeGraph g, Predicate p)
    {
        return g.getVertexConstraints().contains(p);
    }

    /**
     * Returns <code>true</code> if each vertex in <code>g</code>
     * satisfies <code>p</code>, and false otherwise.  (Note: this may be 
     * true even if <code>p</code> is not a constraint of <code>g</code>.)
     */
    public static boolean satisfiesVertexConstraint(ArchetypeGraph g, Predicate p)
    {
        if (PredicateUtils.enforcesVertexConstraint(g, p))
            return true;
        else
            return satisfiesPredicate(g.getVertices(), p);
    }

    /**
     * Returns <code>true</code> if all elements of <code>c</code>
     * satisfy <code>p</code>.
     */
    public static boolean satisfiesPredicate(Collection c, Predicate p)
    {
        for (Iterator iter = c.iterator(); iter.hasNext(); )
        {
            if (!p.evaluate(iter.next()))
                return false;
        }
        return true;
    }
    
    public static Collection getSatisfyingElements(Collection c, Predicate p)
    {
        Collection satisfied = new LinkedList();
        for (Iterator iter = c.iterator(); iter.hasNext(); )
        {
            Object o = iter.next();
            if (p.evaluate(o))
                satisfied.add(o);
        }
        return satisfied;
    }
    
    /**
     * Returns <code>true</code> if <code>g</code> is constrained to only
     * accept directed edges, and false otherwise.
     */
    public static boolean enforcesDirected(Graph g)
    {
        return g.getEdgeConstraints().contains(Graph.DIRECTED_EDGE);
    }

    /**
     * Returns <code>true</code> if <code>g</code> is constrained to only
     * accept undirected edges.
     */
    public static boolean enforcesUndirected(Graph g)
    {
        return g.getEdgeConstraints().contains(Graph.UNDIRECTED_EDGE);
    }
    
    /**
     * Returns <code>true</code> if <code>g</code> is constrained to 
     * reject parallel edges.
     * @see edu.uci.ics.jung.graph.predicates.ParallelEdgePredicate
     */
    public static boolean enforcesNotParallel(Graph g)
    {
        return g.getEdgeConstraints().contains(Graph.NOT_PARALLEL_EDGE);
    }
    
    /**
     * Returns a <code>Map</code> of each constituent predicate of <code>p</code>
     * (if any) to the result of evaluating this predicate on <code>o</code>.  
     * If <code>p</code> is a <code>PredicateDecorator</code>, i.e., a predicate
     * that operates on other <code>Predicate</code>s, the output will consist of
     * the results of evaluting the constituents of <code>p</code> on <code>o</code>;
     * otherwise, the output will be the result of evaluating <code>p</code> itself
     * on <code>o</code>.
     */
    public static Map evaluateNestedPredicates(Predicate p, Object o)
    {
        Map evaluations = new HashMap();
        if (p instanceof PredicateDecorator)
        {
            Predicate[] nested_preds = ((PredicateDecorator)p).getPredicates();
            for (int i = 0; i < nested_preds.length; i++)
                evaluations.put(nested_preds[i], new Boolean(nested_preds[i].evaluate(o)));
        }
        else
            evaluations.put(p, new Boolean(p.evaluate(o)));
        return evaluations;
    }
}
