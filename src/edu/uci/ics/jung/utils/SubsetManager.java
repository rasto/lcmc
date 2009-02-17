/*
 * Created on Apr 22, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.event.GraphEvent;
import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.event.GraphEventType;


/**
 * <p>A class which allows users to create and maintain 
 * <code>Predicate</code>-specified vertex and edge subsets.
 * The subsets are automatically maintained as vertices and
 * edges are added to, and removed from, the constructor-specified
 * graph.</p>
 * 
 * <p>A subset is created by providing a <code>Predicate</code>;
 * those graph elements that pass the predicate are added to the 
 * subset.</p>
 * 
 * @author Joshua O'Madadhain
 */
public class SubsetManager implements GraphEventListener
{
    /**
     * Map specifications (in the form of Predicates) to the corresponding subsets.
     * These are only instantiated if a subset of the appropriate type is created.
     */
    protected Map vertexMap;
    protected Map edgeMap;
    
    /**
     * The graph for which this instance manages subsets.
     */
    protected ArchetypeGraph g;
    
    /**
     * Creates a <code>SubsetManager</code>, adds it to the specified
     * graph's user data repository, and adds itself as a listener to
     * the graph's vertex and edge addition and removal events, so that the
     * subsets' memberships can be maintained.
     */
    protected SubsetManager(ArchetypeGraph g)
    {
        super();
        g.addUserDatum(ArchetypeGraph.SUBSET_MANAGER, this, UserData.REMOVE);
        g.addListener(this, GraphEventType.ALL_SINGLE_EVENTS);
        this.g = g;
    }
    
    /**
     * Gets this graph's <code>SubsetManager</code>, creating it if necessary.
     * 
     * @param g     the graph whose subset manager is requested
     */
    public static SubsetManager getInstance(ArchetypeGraph g)
    {
        SubsetManager sm = (SubsetManager)g.getUserDatum(ArchetypeGraph.SUBSET_MANAGER);
        if (sm == null)
            sm = new SubsetManager(g);
        return sm;
    }
    
    /**
     * Adds the vertex whose event this is to all appropriate subsets.
     */
    public void vertexAdded(GraphEvent event)
    {
        ArchetypeVertex v = (ArchetypeVertex)event.getGraphElement();
        Map vMap = getVertexMap();
        for (Iterator iter = vMap.keySet().iterator(); iter.hasNext(); )
        {
//            VertexPredicate p = (VertexPredicate)iter.next();
            Predicate p = (Predicate)iter.next();
//            if (p.evaluateVertex(v))
            if (p.evaluate(v))
            {
                Set s = (Set)vMap.get(p);
                s.add(v);
            }
        }
    }

    /**
     * Removes the vertex whose event this is from all appropriate subsets.
     */
    public void vertexRemoved(GraphEvent event)
    {
        ArchetypeVertex v = (ArchetypeVertex)event.getGraphElement();
        Map vMap = getVertexMap();
        for (Iterator iter = vMap.keySet().iterator(); iter.hasNext(); )
        {
            Set s = (Set)vMap.get(iter.next());
            s.remove(v);
        }
    }

    /**
     * Adds the edge whose event this is to all appropriate subsets.
     */
    public void edgeAdded(GraphEvent event)
    {
        ArchetypeEdge e = (ArchetypeEdge)event.getGraphElement();
        Map eMap = getEdgeMap();
        for (Iterator iter = eMap.keySet().iterator(); iter.hasNext(); )
        {
//            EdgePredicate p = (EdgePredicate)iter.next();
            Predicate p = (Predicate)iter.next();
//            if (p.evaluateEdge(e))
            if (p.evaluate(e))
            {
                Set s = (Set)eMap.get(p);
                s.add(e);
            }
        }
    }

    /**
     * Removes the edge whose event this is from all appropriate subsets.
     */
    public void edgeRemoved(GraphEvent event)
    {
        ArchetypeEdge e = (ArchetypeEdge)event.getGraphElement();
        Map eMap = getEdgeMap();
        for (Iterator iter = eMap.keySet().iterator(); iter.hasNext(); )
        {
            Set s = (Set)eMap.get(iter.next());
            s.remove(e);
        }
    }
    
    /**
     * Returns the vertex subset, if any, which this instance has defined
     * based on <code>p</code>.  If this instance has defined no such
     * subset, returns null.
     * @param p     the predicate which may define a subset
     */
    public Set getVertices(Predicate p)
    {
        Set s = (Set)getVertexMap().get(p);
        if (s != null)
            return Collections.unmodifiableSet(s);
        else
            return null;
    }

    /**
     * Returns the edge subset, if any, which this instance has defined
     * based on <code>p</code>.  If this instance has defined no such
     * subset, returns null.
     * @param p     the predicate which may define a subset
     */
    public Set getEdges(Predicate p)
    {
        Set s = (Set)getEdgeMap().get(p);
        if (s != null)
            return Collections.unmodifiableSet(s);
        else
            return null;
    }
    
    /**
     * Creates a vertex subset based on <code>p</code>.
     * @param p     the predicate defining the subset
     * @return      true if a subset was created; false if the subset already existed
     */
    public boolean addVertexSubset(Predicate p)
    {
        Map vMap = getVertexMap();
        if (! vMap.containsKey(p))
        {
            Set subset = new HashSet();
            vMap.put(p, subset);
            for (Iterator v_iter = g.getVertices().iterator(); v_iter.hasNext(); )
            {
                ArchetypeVertex v = (ArchetypeVertex)v_iter.next();
                if (p.evaluate(v))
                    subset.add(v);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Creates an edge subset based on <code>p</code>.
     * @param p     the predicate defining the subset
     * @return      true if a subset was created; false if the subset already existed
     */
    public boolean addEdgeSubset(Predicate p)
    {
        Map eMap = getEdgeMap();
        if (! eMap.containsKey(p))
        {
            Set subset = new HashSet();
            eMap.put(p, subset);
            for (Iterator e_iter = g.getEdges().iterator(); e_iter.hasNext(); )
            {
                ArchetypeEdge e = (ArchetypeEdge)e_iter.next();
                if (p.evaluate(e))
                    subset.add(e);
            }
            return true;
        }
        return false;
    }

    /**
     * Removes the vertex subset based on <code>p</code>.
     * @param p     the predicate defining the subset
     */
    public void removeVertexSubset(Predicate p)
    {
        getVertexMap().remove(p);
    }
    
    /**
     * Removes the edge subset based on <code>p</code>.
     * @param p     the predicate defining the subset
     */
    public void removeEdgeSubset(Predicate p)
    {
        getVertexMap().remove(p);
    }

    protected Map getVertexMap()
    {
        if (vertexMap == null)
            vertexMap = new HashMap();
        return vertexMap;
    }

    protected Map getEdgeMap()
    {
        if (edgeMap == null)
            edgeMap = new HashMap();
        return edgeMap;
    }
}
