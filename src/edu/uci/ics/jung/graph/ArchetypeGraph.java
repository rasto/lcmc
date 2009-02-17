/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph;

import java.util.Collection;
import java.util.Set;

import edu.uci.ics.jung.graph.event.GraphEventListener;
import edu.uci.ics.jung.graph.event.GraphEventType;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * A generalized graph which consists of an <code>ArchetypeVertex</code>
 * set and an <code>ArchetypeEdge</code> set.
 * <P>
 * This interface permits, but does not enforce, any of the following 
 * common variations of graphs:
 * <ul>
 * <li> binary edges (edges which connect exactly two vertices)
 * <li> hyperedges (edges which connect one or more vertices)
 * <li> directed and undirected edges
 * <li> vertices and edges with attributes (for example, weighted edges)
 * <li> vertices and edges of different types (for example, bipartite 
 *      or multimodal graphs)
 * <li> parallel edges (multiple edges which connect a single set of vertices)
 * <li> representations as matrices or as adjacency lists
 * </ul> 
 * Extensions or implementations of <code>ArchetypeGraph</code> 
 * may enforce or disallow any or all of these variations.
 *<P>
 * A graph consists of a set of vertices, and a set of edges. Each edge 
 * connects a set of vertices. 
 * For details on the process of adding vertices and edges to a graph,
 * see the documentation for <code>ArchetypeVertex</code> and 
 * <code>ArchetypeEdge</code>.
 * <P>
 * The implementations of the graph, vertex and edge classes are responsible
 * for ensuring that their individual bookkeeping information is kept mutually 
 * consistent.  (For instance, a sparse graph implementation may have separate
 * references to a single edge stored in each of its incident vertices and
 * in the graph itself.  If the edge is removed from the graph, each of these
 * references must also be removed to maintain consistency.)
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @author Scott White
 * 
 * @see ArchetypeEdge
 * @see ArchetypeVertex
 * 
 */
public interface ArchetypeGraph extends UserDataContainer 
{
    public static final String SUBSET_MANAGER = 
        "edu.ics.uci.jung.graph.ArchetypeGraph:SubsetManager";

    /**
     * Returns a graph of the same type as the graph on which this 
     * method is invoked.
     *
     * @return ArchetypeGraph
     */
    public ArchetypeGraph newInstance();

	/**
     * Returns a Set view of all vertices in this graph. In general, this
     * obeys the java.util.Set contract, and therefore makes no guarantees 
     * about the ordering of the vertices within the set.
	 */
	public Set getVertices();

	/**
	 * Returns a Set view of all edges in this graph. In general, this
	 * obeys the java.util.Set contract, and therefore makes no guarantees 
     * about the ordering of the edges within the set.
	 */
	public Set getEdges();

    /**
     * Returns the number of vertices in this graph.
     */
    public int numVertices();

	/**
	 * Returns the number of edges in this graph.
	 */
	public int numEdges();

	/**
	 * Removes all elements of <code>vertices</code> from this graph.
     * If any element of <code>vertices</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any vertices that may have been removed already 
     * are not guaranteed to be restored to the graph.  Prunes any resultant 
     * ill-formed edges.
     * 
	 * @param vertices     the set of vertices to be removed
     * @deprecated As of version 1.7, replaced by <code>GraphUtils.removeVertices(graph, vertices)</code>.
	 */
	public void removeVertices(Set vertices);

	/**
     * Removes all elements of <code>edges</code> from this graph.
     * If any element of <code>edges</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any edges that may have been removed already 
     * are not guaranteed to be restored to the graph.  
     * @deprecated As of version 1.7, replaced by <code>GraphUtils.removeEdges(graph, edges)</code>.
	 */
	public void removeEdges(Set edges);

    /**
     * Removes all edges from this graph, leaving the vertices intact.
     * Equivalent to <code>removeEdges(getEdges())</code>.
     */
    public void removeAllEdges();

    /**
     * Removes all vertices (and, therefore, edges) from this graph.
     * Equivalent to <code>removeVertices(getVertices())</code>.
     */
    public void removeAllVertices();

    /**
     * Performs a deep copy of the graph and its contents.
     */
    public ArchetypeGraph copy();

    /**
     * Tells the graph to add gel as a listener for changes in the graph structure
     * @param gel the graph event listener
     * @param get the type of graph events the listeners wants to listen for
     */
    public void addListener(GraphEventListener gel, GraphEventType get);

    /**
     * Tells the graph to remove gel as a listener for changes in the graph structure
     * @param gel the graph event listener
     * @param get the type of graph events the listeners wants to not listen for
     */
	public void removeListener(GraphEventListener gel, GraphEventType get);
    
    /**
     * Returns the <code>Collection</code> of constraints that each vertex
     * must satisfy when it is added to this graph.  This collection may 
     * be viewed and modified by the user to add or remove constraints.
     */
    public Collection getVertexConstraints();
    
    /**
     * Returns the <code>Collection</code> of requirements that each edge
     * must satisfy when it is added to this graph.  This collection may 
     * be viewed and modified by the user to add or remove requirements.
     */
    public Collection getEdgeConstraints();
        
}
