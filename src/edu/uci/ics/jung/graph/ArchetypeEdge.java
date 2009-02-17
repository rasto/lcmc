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
 * A interface for edge implementations in generalized graphs. 
 * <P>
 * Edges are added to graphs in a two-stage process.  First
 * the user calls the edge constructor, yielding an instance, <code>e</code>, of
 * <code>ArchetypeEdge</code>.  Then the user calls 
 * <code>g.addEdge(e)</code>, where <code>g</code> is an implementation of
 * <code>ArchetypeGraph</code>.  
 * <P>
 * The two-stage nature of this process makes it possible to create "orphaned" 
 * edges that are not part of a graph.  
 * This was done as a compromise between common practices in Java APIs 
 * regarding the side effects of constructors, and the semantics of graphs.
 * However, the behavior of <code>ArchetypeEdge</code> methods, with the 
 * exception of <code>getGraph()</code>, is unspecified on orphaned edges.  
 * The JUNG Project implementations will never create orphaned edges,
 * and we strongly recommend that users follow this practice
 * by nesting the constructor call inside the call to <code>addEdge()</code>,
 * as in the following example:
 * <P>
 * <code>g.addEdge(new UndirectedSparseEdge(v1, v2));</code>
 * <P> 
 * where <code>v1</code> and <code>v2</code> are vertices in <code>g</code>
 * that this new edge is connecting.
 * <P>
 * An <i>ill-formed</i> edge is one that is incident to the wrong
 * number of vertices.  JUNG implementations will never create 
 * ill-formed edges, and will prune such edges if they are generated
 * (for example, by a vertex removal).
 * 
 * 
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * @author Scott White
 * 
 * @see ArchetypeGraph
 * @see ArchetypeVertex
 */
public interface ArchetypeEdge extends Element {

    /**
     * Returns the set of vertices which are incident to this edge.
     * Each of the vertices returned should implement 
     * <code>ArchetypeVertex</code>.
     * For example, returns the source and destination vertices of a 
     * directed edge. 
     *  
     * @return      the vertices incident to this edge
     */
    public Set getIncidentVertices();

    /**
     * Returns the edge in graph <code>g</code>, if any, 
     * that is equivalent to this edge.  
     * Two edges are equivalent if one of them is an ancestor 
     * (via <code>copy()</code>) of the other.
     * 
     * @see #copy(ArchetypeGraph)
     * @see ArchetypeVertex#getEqualVertex(ArchetypeGraph g)
     */
    ArchetypeEdge getEqualEdge(ArchetypeGraph g);

    /**
     * @deprecated As of version 1.4, renamed to getEqualEdge(g).
     */
    ArchetypeEdge getEquivalentEdge(ArchetypeGraph g);
    
    /**
     * Returns the number of vertices which are incident to this edge. 
     */
    int numVertices();

    /**
     * Returns <code>true</code> if the specified vertex <code>v</code> 
     * is incident to this edge, and <code>false</code> otherwise.
     * 
     * The behavior of this method is undefined if <code>v</code> is not
     * an element of this edge's graph.
     */
	boolean isIncident(ArchetypeVertex v);
    
    /**
     * Creates a copy of this edge in graph <code>g</code>.  The edge created 
     * will be equivalent to this edge: given <code>e = this.copy(g)</code>,
     * then <code>this.getEquivalentEdge(g) == e</code>,
     * and <code>this.equals(e) == true</code>.  
     * <P>
     * Given the set
     * of vertices S that are incident to this edge, the copied edge will be 
     * made incident to the set of vertices S' in <code>g</code> that are 
     * equivalent to S.  S must be copied into <code>g</code> before 
     * this edge can be copied into <code>g</code>.  If there is no 
     * such set of vertices in <code>g</code>,
     * this method throws <code>IllegalArgumentException</code>.
     * <P>
     * Thus, for example, given the following code:
     * 
     * <pre>
     *      Graph g1 = new Graph();
     *      Vertex v1 = g1.addVertex(new DirectedSparseVertex());
     *      Vertex v2 = g1.addVertex(new DirectedSparseVertex());
     *      ... 
     *      Edge e = g1.addEdge(new DirectedSparseEdge(v1, v2));
     *      Vertex v3 = v1.getEquivalentVertex(g2);
     *      Vertex v4 = v2.getEquivalentVertex(g2);
     * </pre>
     * 
     * then <code>e.copy(g2)</code> will create a directed edge 
     * connecting <code>v3</code> to <code>v4</code>
     * in <code>g2</code>.
     * 
     * @param g     the graph in which the copied edge will be placed
     * @return      the edge created
     * 
     * @see #getEqualEdge(ArchetypeGraph)
     * @see ArchetypeVertex#getEqualVertex(ArchetypeGraph)
     * 
     * @throws IllegalArgumentException
     */
    public ArchetypeEdge copy(ArchetypeGraph g);
    
}
