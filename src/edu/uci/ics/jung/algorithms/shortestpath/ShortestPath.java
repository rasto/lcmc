/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
* 
* Created on Feb 12, 2004
*/
package edu.uci.ics.jung.algorithms.shortestpath;

import java.util.Map;

import edu.uci.ics.jung.graph.Vertex;

/**
 * 
 * @author Joshua O'Madadhain
 */
public interface ShortestPath
{
    /**
     * <p>Returns a <code>LinkedHashMap</code> which maps each vertex 
     * in the graph (including the <code>source</code> vertex) 
     * to the last edge on the shortest path from the 
     * <code>source</code> vertex.
     */ 
     public abstract Map getIncomingEdgeMap(Vertex source);
}
