/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.shortestpath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.uci.ics.jung.algorithms.connectivity.BFSDistanceLabeler;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserDataUtils;

/**
 * Computes the shortest path distances for graphs whose edges are not weighted (using BFS).
 * 
 * @author Scott White
 */
public class UnweightedShortestPath implements ShortestPath, Distance
{
	private Map mDistanceMap;
	private Map mIncomingEdgeMap;
	private Graph mGraph;

	/**
	 * Constructs and initializes algorithm
	 * @param g the graph
	 */
	public UnweightedShortestPath(Graph g)
	{
		mDistanceMap = new HashMap(g.numVertices() * 2);
		mIncomingEdgeMap = new HashMap(g.numVertices() * 2);
		mGraph = g;
	}

    /**
     * @see edu.uci.ics.jung.algorithms.shortestpath.Distance#getDistance(edu.uci.ics.jung.graph.ArchetypeVertex, edu.uci.ics.jung.graph.ArchetypeVertex)
     */
	public Number getDistance(ArchetypeVertex source, ArchetypeVertex target)
	{
		Map sourceSPMap = getDistanceMap(source);
		return (Number) sourceSPMap.get(target);
	}

    /**
     * @see edu.uci.ics.jung.algorithms.shortestpath.Distance#getDistanceMap(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
	public Map getDistanceMap(ArchetypeVertex source)
	{
		Map sourceSPMap = (Map) mDistanceMap.get(source);
		if (sourceSPMap == null)
		{
			computeShortestPathsFromSource(source);
			sourceSPMap = (Map) mDistanceMap.get(source);
		}
		return sourceSPMap;
	}

	/**
	 * @see edu.uci.ics.jung.algorithms.shortestpath.ShortestPath#getIncomingEdgeMap(edu.uci.ics.jung.graph.Vertex)
	 */
	public Map getIncomingEdgeMap(Vertex source)
	{
		Map sourceIEMap = (Map) mIncomingEdgeMap.get(source);
		if (sourceIEMap == null)
		{
			computeShortestPathsFromSource(source);
			sourceIEMap = (Map) mIncomingEdgeMap.get(source);
		}
		return sourceIEMap;
	}

	/**
	* Computes the shortest path distance from the source to target. If the shortest path distance has not already
	* been computed, then all pairs shortest paths will be computed.
	* @param source the source node
	* @param target the target node
	* @return the shortest path value (if the target is unreachable, NPE is thrown)
	* @deprecated use getDistance
	*/
	public int getShortestPath(Vertex source, Vertex target)
	{
		return getDistance(source, target).intValue();
	}

	/**
	 * Computes the shortest path distances from a given node to all other nodes.
	 * @param graph the graph
	 * @param source the source node
	 * @return A <code>Map</code> whose keys are target vertices and whose values are <code>Integers</code> representing the shortest path distance
	 */
	private void computeShortestPathsFromSource(ArchetypeVertex source)
	{
		String DISTANCE_KEY = "UnweightedShortestPath.DISTANCE";
		BFSDistanceLabeler labeler = new BFSDistanceLabeler(DISTANCE_KEY);
		labeler.labelDistances(mGraph, (Vertex)source);
		Map currentSourceSPMap = new HashMap();
		Map currentSourceEdgeMap = new HashMap();

		for (Iterator vIt = mGraph.getVertices().iterator(); vIt.hasNext();)
		{
			Vertex vertex = (Vertex) vIt.next();
			Number distanceVal = (Number) vertex.getUserDatum(DISTANCE_KEY);
            // BFSDistanceLabeler uses -1 to indicate unreachable vertices;
            // don't bother to store unreachable vertices
            if (distanceVal != null && distanceVal.intValue() >= 0) 
            {
                currentSourceSPMap.put(vertex, distanceVal);
                int minDistance = distanceVal.intValue();
                for (Iterator eIt = vertex.getInEdges().iterator(); eIt.hasNext();)
                {
                    Edge incomingEdge = (Edge) eIt.next();
                    Vertex neighbor = incomingEdge.getOpposite(vertex);
                    Number predDistanceVal =
                        (Number) neighbor.getUserDatum(DISTANCE_KEY);
                    int pred_distance = predDistanceVal.intValue();
//                    if (predDistanceVal.intValue() < minDistance)
                    if (pred_distance < minDistance && pred_distance >= 0)
                    {
                        minDistance = predDistanceVal.intValue();
                        currentSourceEdgeMap.put(vertex, incomingEdge);
                    }
                }
            }
		}

		UserDataUtils.cleanup(mGraph.getVertices(), DISTANCE_KEY);

		mDistanceMap.put(source, currentSourceSPMap);
		mIncomingEdgeMap.put(source, currentSourceEdgeMap);
	}
    
    /**
     * Clears all stored distances for this instance.  
     * Should be called whenever the graph is modified (edge weights 
     * changed or edges added/removed).  If the user knows that
     * some currently calculated distances are unaffected by a
     * change, <code>reset(Vertex)</code> may be appropriate instead.
     * 
     * @see #reset(Vertex)
     */
    public void reset()
    {
        mDistanceMap = new HashMap(mGraph.numVertices() * 2);
        mIncomingEdgeMap = new HashMap(mGraph.numVertices() * 2);
    }
    
    /**
     * Clears all stored distances for the specified source vertex 
     * <code>source</code>.  Should be called whenever the stored distances
     * from this vertex are invalidated by changes to the graph.
     * 
     * @see #reset()
     */
    public void reset(Vertex v)
    {
        mDistanceMap.put(v, null);
        mIncomingEdgeMap.put(v, null);
    }
}
