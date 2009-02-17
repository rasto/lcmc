/*
 * Created on Jul 9, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.algorithms.shortestpath;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Hypervertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.ConstantEdgeValue;
import edu.uci.ics.jung.graph.decorators.NumberEdgeValue;
import edu.uci.ics.jung.utils.MapBinaryHeap;
import edu.uci.ics.jung.utils.Pair;

/**
 * <p>Calculates distances in a specified graph, using  
 * Dijkstra's single-source-shortest-path algorithm.  All edge weights
 * in the graph must be nonnegative; if any edge with negative weight is 
 * found in the course of calculating distances, an 
 * <code>IllegalArgumentException</code> will be thrown.
 * (Note: this exception will only be thrown when such an edge would be
 * used to update a given tentative distance;
 * the algorithm does not check for negative-weight edges "up front".)
 * 
 * <p>Distances and partial results are optionally cached (by this instance)
 * for later reference.  Thus, if the 10 closest vertices to a specified source 
 * vertex are known, calculating the 20 closest vertices does not require 
 * starting Dijkstra's algorithm over from scratch.</p>
 * 
 * <p>Distances are stored as double-precision values.  
 * If a vertex is not reachable from the specified source vertex, no 
 * distance is stored.  <b>This is new behavior with version 1.4</b>;
 * the previous behavior was to store a value of 
 * <code>Double.POSITIVE_INFINITY</code>.  This change gives the algorithm
 * an approximate complexity of O(kD log k), where k is either the number of
 * requested targets or the number of reachable vertices (whichever is smaller),
 * and D is the average degree of a vertex.</p>
 * 
 * <p> The elements in the maps returned by <code>getDistanceMap</code> 
 * are ordered (that is, returned 
 * by the iterator) by nondecreasing distance from <code>source</code>.</p>
 * 
 * <p>Users are cautioned that distances calculated should be assumed to
 * be invalidated by changes to the graph, and should invoke <code>reset()</code>
 * when appropriate so that the distances can be recalculated.</p>
 * 
 * @author Joshua O'Madadhain
 */
public class DijkstraDistance implements Distance
{
    protected ArchetypeGraph g;
    protected NumberEdgeValue nev;
    protected Map sourceMap;   // a map of source vertices to an instance of SourceData
    protected boolean cached;
    protected static final NumberEdgeValue dev = new ConstantEdgeValue(new Integer(1));
    protected double max_distance;
    protected int max_targets;
    
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
    public DijkstraDistance(ArchetypeGraph g, NumberEdgeValue nev, boolean cached)
    {
        this.g = g;
        this.nev = nev;
        this.sourceMap = new HashMap();
        this.cached = cached;
        this.max_distance = Double.POSITIVE_INFINITY;
        this.max_targets = Integer.MAX_VALUE;
    }
    
    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified graph and the specified method of extracting weights 
     * from edges, which caches results locally.
     * 
     * @param g     the graph on which distances will be calculated
     * @param nev   the class responsible for returning weights for edges
     */
    public DijkstraDistance(ArchetypeGraph g, NumberEdgeValue nev)
    {
        this(g, nev, true);
    }
    
    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified unweighted graph (that is, all weights 1) which
     * caches results locally.
     * 
     * @param g     the graph on which distances will be calculated
     */ 
    public DijkstraDistance(ArchetypeGraph g)
    {
        this(g, dev, true);
    }

    /**
     * <p>Creates an instance of <code>DijkstraShortestPath</code> for 
     * the specified unweighted graph (that is, all weights 1) which
     * caches results locally.
     * 
     * @param g     the graph on which distances will be calculated
     * @param cached    specifies whether the results are to be cached
     */ 
    public DijkstraDistance(ArchetypeGraph g, boolean cached)
    {
        this(g, dev, cached);
    }
    
    /**
     * Implements Dijkstra's single-source shortest-path algorithm for
     * weighted graphs.  Uses a <code>MapBinaryHeap</code> as the priority queue, 
     * which gives this algorithm a time complexity of O(m lg n) (m = # of edges, n = 
     * # of vertices).
     * This algorithm will terminate when any of the following have occurred (in order
     * of priority):
     * <ul>
     * <li> the distance to the specified target (if any) has been found
     * <li/> no more vertices are reachable 
     * <li> the specified # of distances have been found
     * <li> all distances have been found
     * </ul>
     * 
     * @param source    the vertex from which distances are to be measured
     * @param numDests  the number of distances to measure
     * @param targets   the set of vertices to which distances are to be measured
     */
    protected LinkedHashMap singleSourceShortestPath(ArchetypeVertex source, Set targets, int numDests)
    {
        SourceData sd = getSourceData(source);

        Set to_get = new HashSet();
        if (targets != null)
        {
            to_get.addAll(targets);
            Set existing_dists = sd.distances.keySet();
            for (Iterator iter = targets.iterator(); iter.hasNext(); )
            {
                Object o = iter.next();
                if (existing_dists.contains(o))
                    to_get.remove(o);
            }
        }
        
        // if we've exceeded the max distance or max # of distances we're willing to calculate, or
        // if we already have all the distances we need, 
        // terminate
        if (sd.reached_max ||
                (targets != null && to_get.isEmpty()) ||
                (sd.distances.size() >= numDests))
        {
            return sd.distances;
        }
        
        while (!sd.unknownVertices.isEmpty() && (sd.distances.size() < numDests || !to_get.isEmpty()))
        {
            Pair p = sd.getNextVertex();
            ArchetypeVertex v = (ArchetypeVertex)p.getFirst();
            double v_dist = ((Double)p.getSecond()).doubleValue();
            sd.dist_reached = v_dist;
            to_get.remove(v);
            if ((sd.dist_reached >= this.max_distance) || (sd.distances.size() >= max_targets))
            {
                sd.reached_max = true;
                break;
            }
            
            for (Iterator out_iter = getIncidentEdges(v).iterator(); out_iter.hasNext(); )
            {
                ArchetypeEdge e = (ArchetypeEdge)out_iter.next();
//              Vertex w = e.getOpposite(v);
                for (Iterator e_iter = e.getIncidentVertices().iterator(); e_iter.hasNext(); )
                {
                    ArchetypeVertex w = (ArchetypeVertex)e_iter.next();
                    if (!sd.distances.containsKey(w))
                    {
                        double edge_weight = nev.getNumber(e).doubleValue();
                        if (edge_weight < 0)
                            throw new IllegalArgumentException("Edge weights must be non-negative");
                        double new_dist = v_dist + edge_weight;
                        if (!sd.estimatedDistances.containsKey(w))
                        {
                            sd.createRecord(w, e, new_dist);
                        }
                        else
                        {
                            double w_dist = ((Double)sd.estimatedDistances.get(w)).doubleValue();
                            if (new_dist < w_dist) // update tentative distance & path for w
                                sd.update(w, e, new_dist);
                        }
                    }
                }
            }
//            // if we have calculated the distance to the target, stop
//            if (v == target)
//                break;

        }
        return sd.distances;
    }

    protected SourceData getSourceData(ArchetypeVertex source)
    {
        SourceData sd = (SourceData)sourceMap.get(source);
        if (sd == null)
            sd = new SourceData(source);
        return sd;
    }
    
    /**
     * Returns the set of edges incident to <code>v</code> that should be tested.
     * By default, this is the set of outgoing edges for instances of <code>Vertex</code>,
     * the set of incident edges for instances of <code>Hypervertex</code>,
     * and is otherwise undefined.
     */
    protected Set getIncidentEdges(ArchetypeVertex v)
    {
        if (v instanceof Vertex)
            return ((Vertex)v).getOutEdges();
        else if (v instanceof Hypervertex)
            return v.getIncidentEdges();
        else
            throw new IllegalArgumentException("Unrecognized vertex type: " + v.getClass().getName());
    }

    
    /**
     * Returns the length of a shortest path from the source to the target vertex,
     * or null if the target is not reachable from the source.
     * If either vertex is not in the graph for which this instance
     * was created, throws <code>IllegalArgumentException</code>.
     * 
     * @see #getDistanceMap(ArchetypeVertex)
     * @see #getDistanceMap(ArchetypeVertex,int)
     */
    public Number getDistance(ArchetypeVertex source, ArchetypeVertex target)
    {
        if (target.getGraph() != g)
            throw new IllegalArgumentException("Specified target vertex " + 
                    target + " is not part of graph " + g);
        
        Set targets = new HashSet();
        targets.add(target);
        Map distanceMap = getDistanceMap(source, targets);
        return (Double)distanceMap.get(target);
    }
    

    public Map getDistanceMap(ArchetypeVertex source, Set targets)
    {
        if (source.getGraph() != g)
            throw new IllegalArgumentException("Specified source vertex " + 
                    source + " is not part of graph " + g);

        if (targets.size() > max_targets)
            throw new IllegalArgumentException("size of target set exceeds maximum " +
                    "number of targets allowed: " + this.max_targets);
        
        Map distanceMap = singleSourceShortestPath(source, targets, (int)Math.min(g.numVertices(), max_targets));
        
        if (!cached)
            reset(source);
        
        return distanceMap;
    }
    
    /**
     * <p>Returns a <code>LinkedHashMap</code> which maps each vertex 
     * in the graph (including the <code>source</code> vertex) 
     * to its distance from the <code>source</code> vertex.
     * The map's iterator will return the elements in order of 
     * increasing distance from <code>source</code>.</p>
     * 
     * <p>The size of the map returned will be the number of 
     * vertices reachable from <code>source</code>.</p>
     * 
     * @see #getDistanceMap(ArchetypeVertex,int)
     * @see #getDistance(ArchetypeVertex,ArchetypeVertex)
     * @param source    the vertex from which distances are measured
     */
    public Map getDistanceMap(ArchetypeVertex source)
    {
        return getDistanceMap(source, (int)Math.min(g.numVertices(), max_targets));
    }
    


    /**
     * <p>Returns a <code>LinkedHashMap</code> which maps each of the closest 
     * <code>numDist</code> vertices to the <code>source</code> vertex 
     * in the graph (including the <code>source</code> vertex) 
     * to its distance from the <code>source</code> vertex.  Throws 
     * an <code>IllegalArgumentException</code> if <code>source</code>
     * is not in this instance's graph, or if <code>numDests</code> is 
     * either less than 1 or greater than the number of vertices in the 
     * graph.</p>
     * 
     * <p>The size of the map returned will be the smaller of 
     * <code>numDests</code> and the number of vertices reachable from
     * <code>source</code>. 
     * 
     * @see #getDistanceMap(ArchetypeVertex)
     * @see #getDistance(ArchetypeVertex,ArchetypeVertex)
     * @param source    the vertex from which distances are measured
     * @param numDests  the number of vertices for which to measure distances
     */
    public LinkedHashMap getDistanceMap(ArchetypeVertex source, int numDests)
    {
        if (source.getGraph() != g)
            throw new IllegalArgumentException("Specified source vertex " + 
                source + " is not part of graph " + g);

        if (numDests < 1 || numDests > g.numVertices())
            throw new IllegalArgumentException("numDests must be >= 1 " + 
                "and <= g.numVertices()");

        if (numDests > max_targets)
            throw new IllegalArgumentException("numDests must be <= the maximum " +
                    "number of targets allowed: " + this.max_targets);
            
        LinkedHashMap distanceMap = singleSourceShortestPath(source, null, numDests);
                
        if (!cached)
            reset(source);
        
        return distanceMap;        
    }
    
    /**
     * Allows the user to specify the maximum distance that this instance will calculate.
     * Any vertices past this distance will effectively be unreachable from the source, in
     * the sense that the algorithm will not calculate the distance to any vertices which
     * are farther away than this distance.  A negative value for <code>max_dist</code> 
     * will ensure that no further distances are calculated.
     * 
     * <p>This can be useful for limiting the amount of time and space used by this algorithm
     * if the graph is very large.</p>
     * 
     * <p>Note: if this instance has already calculated distances greater than <code>max_dist</code>,
     * and the results are cached, those results will still be valid and available; this limit
     * applies only to subsequent distance calculations.</p>
     * @see #setMaxTargets(double)
     */
    public void setMaxDistance(double max_dist)
    {
        this.max_distance = max_dist;
        for (Iterator iter = sourceMap.keySet().iterator(); iter.hasNext(); )
        {
            SourceData sd = (SourceData)sourceMap.get(iter.next());
            sd.reached_max = (this.max_distance <= sd.dist_reached) || (sd.distances.size() >= max_targets);
        }
    }
       
    /**
     * Allows the user to specify the maximum number of target vertices per source vertex 
     * for which this instance will calculate distances.  Once this threshold is reached, 
     * any further vertices will effectively be unreachable from the source, in
     * the sense that the algorithm will not calculate the distance to any more vertices.  
     * A negative value for <code>max_targets</code> will ensure that no further distances are calculated.
     * 
     * <p>This can be useful for limiting the amount of time and space used by this algorithm
     * if the graph is very large.</p>
     * 
     * <p>Note: if this instance has already calculated distances to a greater number of 
     * targets than <code>max_targets</code>, and the results are cached, those results 
     * will still be valid and available; this limit applies only to subsequent distance 
     * calculations.</p>
     * @see #setMaxDistance(double)
     */
    public void setMaxTargets(int max_targets)
    {
        this.max_targets = max_targets;
        for (Iterator iter = sourceMap.keySet().iterator(); iter.hasNext(); )
        {
            SourceData sd = (SourceData)sourceMap.get(iter.next());
            sd.reached_max = (this.max_distance <= sd.dist_reached) || (sd.distances.size() >= max_targets);
        }
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
        sourceMap = new HashMap();
    }
        
    /**
     * Specifies whether or not this instance of <code>DijkstraShortestPath</code>
     * should cache its results (final and partial) for future reference.
     * 
     * @param enable    <code>true</code> if the results are to be cached, and
     *                  <code>false</code> otherwise
     */
    public void enableCaching(boolean enable)
    {
        this.cached = enable;
    }
    
    /**
     * Clears all stored distances for the specified source vertex 
     * <code>source</code>.  Should be called whenever the stored distances
     * from this vertex are invalidated by changes to the graph.
     * 
     * @see #reset()
     */
    public void reset(ArchetypeVertex source)
    {
        sourceMap.put(source, null);
    }

    /**
     * Compares according to distances, so that the BinaryHeap knows how to 
     * order the tree.  
     */
    protected class VertexComparator implements Comparator
    {
        private Map distances;
        
        public VertexComparator(Map distances)
        {
            this.distances = distances;
        }

        public int compare(Object o1, Object o2)
        {
            return ((Comparable)distances.get(o1)).compareTo(distances.get(o2));
        }
    }
    
    /**
     * For a given source vertex, holds the estimated and final distances, 
     * tentative and final assignments of incoming edges on the shortest path from
     * the source vertex, and a priority queue (ordered by estimaed distance)
     * of the vertices for which distances are unknown.
     * 
     * @author Joshua O'Madadhain
     */
    protected class SourceData
    {
        public LinkedHashMap distances;
        public Map estimatedDistances;
        public MapBinaryHeap unknownVertices;
        public boolean reached_max = false;
        public double dist_reached = 0;

        public SourceData(ArchetypeVertex source)
        {
            distances = new LinkedHashMap();
            estimatedDistances = new HashMap();
            unknownVertices = new MapBinaryHeap(new VertexComparator(estimatedDistances));
            
            sourceMap.put(source, this);
            
            // initialize priority queue
            estimatedDistances.put(source, new Double(0)); // distance from source to itself is 0
            unknownVertices.add(source);
            reached_max = false;
            dist_reached = 0;
        }
        
        public Pair getNextVertex()
        {
            ArchetypeVertex v = (ArchetypeVertex)unknownVertices.pop();
            Double dist = (Double)estimatedDistances.remove(v);
            distances.put(v, dist);
            return new Pair(v, dist);
        }
        
        public void update(ArchetypeVertex dest, ArchetypeEdge tentative_edge, double new_dist)
        {
            estimatedDistances.put(dest, new Double(new_dist));
            unknownVertices.update(dest);
        }
        
        public void createRecord(ArchetypeVertex w, ArchetypeEdge e, double new_dist)
        {
            estimatedDistances.put(w, new Double(new_dist));
            unknownVertices.add(w);
        }
    }
}
