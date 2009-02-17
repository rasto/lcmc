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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.utils.Pair;

/**
 * <p>Calculates distances and shortest paths using Dijkstra's   
 * single-source-shortest-path algorithm.  This is a lightweight
 * extension of <code>DijkstraDistance</code> that also stores
 * path information, so that the shortest paths can be reconstructed.</p>
 * 
 * <p> The elements in the maps returned by 
 * <code>getIncomingEdgeMap</code> are ordered (that is, returned 
 * by the iterator) by nondecreasing distance from <code>source</code>.</p>
 * 
 * @author Joshua O'Madadhain
 * @see DijkstraDistance
 */
public class DijkstraShortestPath extends DijkstraDistance implements ShortestPath
{
    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified graph and the specified method of extracting weights 
     * from edges, which caches results locally if and only if 
     * <code>cached</code> is <code>true</code>.
     * 
     * @param g     the graph on which distances will be calculated
     * @param nev   the class responsible for returning weights for edges
     * @param cached    specifies whether the results are to be cached
     */
    public DijkstraShortestPath(ArchetypeGraph g, NumberEdgeValue nev, boolean cached)
    {
        super(g, nev, cached);
    }
    
    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified graph and the specified method of extracting weights 
     * from edges, which caches results locally.
     * 
     * @param g     the graph on which distances will be calculated
     * @param nev   the class responsible for returning weights for edges
     */
    public DijkstraShortestPath(ArchetypeGraph g, NumberEdgeValue nev)
    {
        super(g, nev);
    }
    
    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified unweighted graph (that is, all weights 1) which
     * caches results locally.
     * 
     * @param g     the graph on which distances will be calculated
     */ 
    public DijkstraShortestPath(ArchetypeGraph g)
    {
        super(g);
    }

    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified unweighted graph (that is, all weights 1) which
     * caches results locally.
     * 
     * @param g     the graph on which distances will be calculated
     * @param cached    specifies whether the results are to be cached
     */ 
    public DijkstraShortestPath(ArchetypeGraph g, boolean cached)
    {
        super(g, cached);
    }
    
    protected SourceData getSourceData(ArchetypeVertex source)
    {
        SourceData sd = (SourcePathData)sourceMap.get(source);
        if (sd == null)
            sd = new SourcePathData(source);
        return sd;
    }
    
    /**
     * <p>Returns the last edge on a shortest path from <code>source</code>
     * to <code>target</code>, or null if <code>target</code> is not 
     * reachable from <code>source</code>.</p>
     * 
     * <p>If either vertex is not in the graph for which this instance
     * was created, throws <code>IllegalArgumentException</code>.</p>
     */
	public Edge getIncomingEdge(Vertex source, Vertex target)
	{
        if (source.getGraph() != g)
            throw new IllegalArgumentException("Specified source vertex " + 
                    source + " is not part of graph " + g);

        if (target.getGraph() != g)
            throw new IllegalArgumentException("Specified target vertex " + 
                    target + " is not part of graph " + g);

        Set targets = new HashSet();
        targets.add(target);
        singleSourceShortestPath(source, targets, g.numVertices());
        Map incomingEdgeMap = 
            ((SourcePathData)sourceMap.get(source)).incomingEdges;
        Edge incomingEdge = (Edge)incomingEdgeMap.get(target);
        
        if (!cached)
            reset(source);
        
        return incomingEdge;
	}

    /**
     * <p>Returns a <code>LinkedHashMap</code> which maps each vertex 
     * in the graph (including the <code>source</code> vertex) 
     * to the last edge on the shortest path from the 
     * <code>source</code> vertex.
     * The map's iterator will return the elements in order of 
     * increasing distance from <code>source</code>.</p>
     * 
     * @see DijkstraDistance#getDistanceMap(Vertex,int)
     * @see DijkstraDistance#getDistance(Vertex,Vertex)
     * @param source    the vertex from which distances are measured
     */
    public Map getIncomingEdgeMap(Vertex source)
	{
		return getIncomingEdgeMap(source, g.numVertices());
	}

    /**
     * Returns a <code>List</code> of the edges on the shortest path from 
     * <code>source</code> to <code>target</code>, in order of their
     * occurrence on this path.  
     * If either vertex is not in the graph for which this instance
     * was created, throws <code>IllegalArgumentException</code>.
     */
	public List getPath(Vertex source, Vertex target)
	{
        if (source.getGraph() != g)
            throw new IllegalArgumentException("Specified source vertex " + 
                    source + " is not part of graph " + g);

        if (target.getGraph() != g)
            throw new IllegalArgumentException("Specified target vertex " + 
                    target + " is not part of graph " + g);
        
        LinkedList path = new LinkedList();

        // collect path data; must use internal method rather than
        // calling getIncomingEdge() because getIncomingEdge() may
        // wipe out results if results are not cached
        Set targets = new HashSet();
        targets.add(target);
        singleSourceShortestPath(source, targets, g.numVertices());
        Map incomingEdges = 
            ((SourcePathData)sourceMap.get(source)).incomingEdges;
        
        if (incomingEdges.isEmpty() || incomingEdges.get(target) == null)
            return path;
        Vertex current = target;
        while (current != source)
        {
            Edge incoming = (Edge)incomingEdges.get(current);
            path.addFirst(incoming);
            current = incoming.getOpposite(current);
        }
		return path;
	}

    
    /**
     * <p>Returns a <code>LinkedHashMap</code> which maps each of the closest 
     * <code>numDist</code> vertices to the <code>source</code> vertex 
     * in the graph (including the <code>source</code> vertex) 
     * to the incoming edge along the path from that vertex.  Throws 
     * an <code>IllegalArgumentException</code> if <code>source</code>
     * is not in this instance's graph, or if <code>numDests</code> is 
     * either less than 1 or greater than the number of vertices in the 
     * graph.
     * 
     * @see #getIncomingEdgeMap(Vertex)
     * @see #getPath(Vertex,Vertex)
     * @param source    the vertex from which distances are measured
     * @param numDests  the number of vertics for which to measure distances
     */
	public LinkedHashMap getIncomingEdgeMap(Vertex source, int numDests)
	{
        if (source.getGraph() != g)
            throw new IllegalArgumentException("Specified source vertex " + 
                    source + " is not part of graph " + g);

        if (numDests < 1 || numDests > g.numVertices())
            throw new IllegalArgumentException("numDests must be >= 1 " + 
            "and <= g.numVertices()");

        singleSourceShortestPath(source, null, numDests);
        
        LinkedHashMap incomingEdgeMap = 
            ((SourcePathData)sourceMap.get(source)).incomingEdges;
        
        if (!cached)
            reset(source);
        
        return incomingEdgeMap;        
	}
     
    
    /**
     * For a given source vertex, holds the estimated and final distances, 
     * tentative and final assignments of incoming edges on the shortest path from
     * the source vertex, and a priority queue (ordered by estimaed distance)
     * of the vertices for which distances are unknown.
     * 
     * @author Joshua O'Madadhain
     */
    protected class SourcePathData extends SourceData
    {
        public Map tentativeIncomingEdges;
		public LinkedHashMap incomingEdges;

		public SourcePathData(ArchetypeVertex source)
		{
            super(source);
            incomingEdges = new LinkedHashMap();
            tentativeIncomingEdges = new HashMap();
		}
        
        public void update(ArchetypeVertex dest, ArchetypeEdge tentative_edge, double new_dist)
        {
            super.update(dest, tentative_edge, new_dist);
            tentativeIncomingEdges.put(dest, tentative_edge);
        }
        
        public Pair getNextVertex()
        {
            Pair p = super.getNextVertex();
            ArchetypeVertex v = (ArchetypeVertex)p.getFirst();
            Edge incoming = (Edge)tentativeIncomingEdges.remove(v);
            incomingEdges.put(v, incoming);
            return p;
        }
        
        public void createRecord(ArchetypeVertex w, ArchetypeEdge e, double new_dist)
        {
            super.createRecord(w, e, new_dist);
            tentativeIncomingEdges.put(w, e);
        }
       
    }

}
