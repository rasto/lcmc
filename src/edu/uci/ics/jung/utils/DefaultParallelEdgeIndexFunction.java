/*
 * Created on Sep 24, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * A class which creates and maintains indices for parallel edges.
 * Parallel edges are defined here to be those edges of type <code>Edge</code>
 * that are returned by <code>v.findEdgeSet(w)</code> for some 
 * <code>v</code> and <code>w</code>.
 * 
 * <p>At this time, users are responsible for resetting the indices if changes to the
 * graph make it appropriate.</p>
 * 
 * @author Joshua O'Madadhain
 * @author Tom Nelson
 *
 */
public class DefaultParallelEdgeIndexFunction implements ParallelEdgeIndexFunction
{
    protected Map edge_index = new HashMap();
    
    private DefaultParallelEdgeIndexFunction() { }
    
    public static DefaultParallelEdgeIndexFunction getInstance() {
        return new DefaultParallelEdgeIndexFunction();
    }
    /**
     * Returns the index for the specified edge.
     * Calculates the indices for <code>e</code> and for all edges parallel
     * to <code>e</code>.
     */
    public int getIndex(Edge e)
    {
        Integer index = (Integer)edge_index.get(e);
        if(index == null) 
            index = getIndex_internal(e);
        return index.intValue();
    }

    protected Integer getIndex_internal(Edge e)
    {
        Pair endpoints = e.getEndpoints();
        Vertex u = (Vertex)(endpoints.getFirst());
        Vertex v = (Vertex)(endpoints.getSecond());
        Set commonEdgeSet = u.findEdgeSet(v);
        int count = 0;
        for(Iterator iterator=commonEdgeSet.iterator(); iterator.hasNext(); ) {
            Edge other = (Edge)iterator.next();
            if (e.equals(other) == false)
            {
                edge_index.put(other, new Integer(count));
                count++;
            }
        }
        Integer index = new Integer(count);
        edge_index.put(e, index);
        
        return index;
    }
    
    /**
     * Resets the indices for this edge and its parallel edges.
     * Should be invoked when an edge parallel to <code>e</code>
     * has been added or removed.
     * @param e
     */
    public void reset(Edge e)
    {
        getIndex_internal(e);
    }
    
    /**
     * Clears all edge indices for all edges in all graphs.
     * Does not recalculate the indices.
     */
    public void reset()
    {
        edge_index.clear();
    }
}
