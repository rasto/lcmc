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

import java.util.Set;

/**
 * A interface for vertex implementations in generalized graphs. 
 * <P>
 * Vertices are added to graphs in a two-stage process.  First
 * the user calls the vertex constructor, yielding an instance, 
 * <code>v</code>, of <code>ArchetypeVertex</code>.  Then the user
 * calls <code>g.addVertex(v)</code>, where <code>g</code> is an 
 * implementation of <code>ArchetypeGraph</code>.  
 * <P>
 * The two-stage nature of this process makes it possible to create "orphaned" 
 * vertices that are not part of a graph.  
 * This was done as a compromise between common practices in Java APIs 
 * regarding the side effects of constructors, and the semantics of graphs.
 * However, the behavior of all <code>ArchetypeVertex</code> methods, with 
 * the exception of <code>getGraph()</code>, is unspecified on orphaned vertices.  
 * The JUNG Project implementations will never create orphaned 
 * vertices, and we strongly recommend that users follow this practice
 * by nesting the constructor call inside the call to <code>addVertex()</code>,
 * as in the following example:
 * <P>
 * <code>g.addVertex(new VertexType());</code>
 * <P> 
 * where <code>VertexType</code> is the type of vertex that the user wishes
 * to create.
 * <P>
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @author Scott White
 * 
 * @see ArchetypeEdge
 * @see ArchetypeGraph
 */
public interface ArchetypeVertex extends Element {

    /**
     * Returns the set of vertices which are connected to this vertex 
     * via edges; each of these vertices should implement 
     * <code>ArchetypeVertex</code>.
     * If this vertex is connected to itself with a self-loop, then 
     * this vertex will be included in its own neighbor set.
     */
    public Set getNeighbors();
    
    /**
     * Returns the set of edges which are incident to this vertex.
     * Each of these edges should implement <code>ArchetypeEdge</code>.  
     */
    public Set getIncidentEdges();

    /**
     * Returns the number of edges incident to this vertex.  
     * Special cases of interest:
     * <ul>
     * <li> If there is only one edge that connects this vertex to
     * each of its neighbors (and vice versa), then the value returned 
     * will also be 
     * equal to the number of neighbors that this vertex has.
     * <li> If the graph is directed, then the value returned will be 
     * the sum of this vertex's indegree (the number of edges whose 
     * destination is this vertex) and its outdegree (the number
     * of edges whose source is this vertex).
     * </ul>
     * 
     * @return int  the degree of this node
     * @see ArchetypeVertex#numNeighbors
     */
    public int degree();

    /**
     * Returns the number of neighbors that this vertex has.
     * If the graph is directed, the value returned will be the
     * sum of the number of predecessors and the number of 
     * successors that this vertex has.
     * 
     * @since 1.1.1
     * @see ArchetypeVertex#degree
     */
    public int numNeighbors();

    /**
     * Returns the vertex in graph <code>g</code>, if any, that is 
     * equal to this vertex. Otherwise, returns null.
     * Two vertices are equal if one of them is an ancestor (via 
     * <code>copy()</code>) of the other.
     *  
     * @see #copy(ArchetypeGraph)
     * @see ArchetypeEdge#getEqualEdge(ArchetypeGraph)
     */
    public ArchetypeVertex getEqualVertex(ArchetypeGraph g);

    /**
     * @deprecated As of version 1.4, renamed to getEqualVertex(g).
     */
    public ArchetypeVertex getEquivalentVertex(ArchetypeGraph g);
    
    /**
     * Returns <code>true</code> if the specified vertex <code>v</code> and
     * this vertex are each incident
     * to one or more of the same edges, and <code>false</code> otherwise.
     * 
     * The behavior of this method is undefined if <code>v</code> is not
     * an element of this vertex's graph.
     */
    public boolean isNeighborOf(ArchetypeVertex v);

    /**
     * Returns <code>true</code> if the specified edge <code>e</code> is 
     * incident to this vertex, and <code>false</code> otherwise.
     * 
     * The behavior of this method is undefined if <code>e</code> is not
     * an element of this vertex's graph.
     */
    public boolean isIncident(ArchetypeEdge e); 
    

    /**
     * Creates a copy of this vertex in graph <code>g</code>.  The vertex 
     * created will be equivalent to this vertex: given 
     * <code>v = this.copy(g)</code>, then 
     * <code>this.getEquivalentVertex(g) == v</code>, and
     * <code>this.equals(v) == true</code>.  
     * 
     * @param g     the graph in which the copied vertex will be placed
     * @return      the vertex created
     */
    public ArchetypeVertex copy(ArchetypeGraph g);
    
    /**
     * Returns an edge that connects this vertex to <code>v</code>.
     * If this edge is not uniquely
     * defined (that is, if the graph contains more than one edge connecting 
     * this vertex to <code>v</code>), any of these edges 
     * <code>v</code> may be returned.  <code>findEdgeSet(v)</code> may be 
     * used to return all such edges.
     * If <code>v</code> is not connected to this vertex, returns 
     * <code>null</code>.
     * 
     * @see ArchetypeVertex#findEdgeSet(ArchetypeVertex) 
     */
    public ArchetypeEdge findEdge(ArchetypeVertex v);
    
    /**
     * Returns the set of all edges that connect this vertex
     * with the specified vertex <code>v</code>.  
     * <code>findEdge(v)</code> may be used to return
     * a single (arbitrary) element of this set.
     * If <code>v</code>
     * is not connected to this vertex, returns an empty <code>Set</code>.
     * 
     * @see ArchetypeVertex#findEdge(ArchetypeVertex)
     */
    public Set findEdgeSet(ArchetypeVertex v);    

}
