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
 * A specific type of <code>ArchetypeVertex</code> that can be connected 
 * by instances of <code>Edge</code>.
 * <P>
 * A vertex may be connected to other vertices by directed or undirected
 * edges.  A <code>DirectedEdge</code> has a source <code>Vertex</code> and 
 * a (distinct) destination <code>Vertex</code>.  An 
 * <code>UndirectedEdge</code> treats each incident <code>Vertex</code> as
 * if it were both source and destination for that edge:
 * 
 * <pre>
 *      UndirectedGraph g;
 *      Vertex v1, v2;
 *      ...
 * 
 *      Edge e = g.addEdge(new UndirectedSparseEdge(v1, v2));
 *      g.addEdge(e);
 *      v1.isSource(e);     // evaluates to true
 *      v2.isSource(e);     // evaluates to true
 *      v1.isDest(e);       // evaluates to true
 *      v2.isDest(e);       // evaluates to true
 * </pre>
 * 
 * For this reason,
 * an <code>UndirectedEdge</code> which is incident to a <code>Vertex</code>
 * is considered to be both an incoming and an outgoing edge of that vertex. 
 * Therefore two instances of <code>Vertex</code> which are connected
 * by an <code>UndirectedEdge</code> are mutually successors of and 
 * predecessors of each other:
 * 
 * <pre>
 *      v1.isPredecessorOf(v2);     // evaluates to true
 *      v2.isPredecessorOf(v1);     // evaluates to true
 *      v1.isSuccessorOf(v2);       // evaluates to true
 *      v2.isSuccessorOf(v1);       // evaluates to true
 * </pre>  
 * 
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 * @author Scott White
 *
 * @see Graph
 * @see Edge
 * @see UndirectedEdge
 * @see DirectedEdge
 */
public interface Vertex extends ArchetypeVertex 
{
    /**
     * Returns the set of predecessors of this vertex.  
     * A vertex <code>v</code> is a predecessor of this vertex if and only if
     * <code>v.isPredecessorOf(this)</code> returns <code>true</code>.
     * Each element of the set returned should implement <code>Vertex</code>.
     * 
     * @see ArchetypeVertex#getNeighbors()
     * @see #isPredecessorOf(Vertex)
     * @return  all predecessors of this vertex
     */
    public Set getPredecessors();

    /**
     * Returns the set of successors of this vertex.  
     * A vertex <code>v</code> is a successor of this vertex if and only if
     * <code>v.isSuccessorOf(this)</code> returns <code>true</code>.
     * Each element of the set returned should implement <code>Vertex</code>.
     * 
     * @see ArchetypeVertex#getNeighbors()
     * @see #isSuccessorOf(Vertex)
     * @return  all successors of this vertex
     */
    public Set getSuccessors();

    /**
     * Returns the set of incoming edges of this vertex.  An edge
     * <code>e</code> is an incoming edge of this vertex if and only if
     * <code>this.isDest(e)</code> returns <code>true</code>.
     * Each element of the set returned should implement <code>Edge</code>.
     * 
     * @see ArchetypeVertex#getIncidentEdges()
     * @return  all edges whose destination is this vertex
     */
    public Set getInEdges();

    /**
     * Returns the set of outgoing edges of this vertex.  An edge 
     * <code>e</code> is an outgoing edge of this vertex if and only if
     * <code>this.isSource(e)</code> returns <code>true</code>.
     * Each element of the set returned should implement <code>Edge</code>.
     * 
     * @see ArchetypeVertex#getIncidentEdges()
     * @return  all edges whose source is this vertex
     */
    public Set getOutEdges();

    /**
     * Returns the number of incoming edges that are incident to this
     * vertex.
     * 
     * @see #getInEdges()
     * @see ArchetypeVertex#degree()
     * @return  the number of incoming edges of this vertex
     */
    public int inDegree();

    /**
     * Returns the number of outgoing edges that are incident to this 
     * vertex.
     * 
     * @see #getOutEdges()
     * @see ArchetypeVertex#degree()
     * @return  the number of outgoing edges of this vertex
     */
    public int outDegree();
    
    /**
     * Returns the number of predecessors of this vertex.
     * 
     * @see #getPredecessors()
     * @see ArchetypeVertex#numNeighbors()
     * @since 1.1.1
     */
    public int numPredecessors();
    
    /**
     * Returns the number of successors of this vertex.
     * 
     * @see #getSuccessors()
     * @see ArchetypeVertex#numNeighbors()
     * @since 1.1.1
     */
    public int numSuccessors();
    
    /**
     * Returns <code>true</code> if this vertex is a successor of
     * the specified vertex <code>v</code>, and <code>false</code> otherwise.
     * This vertex is a successor of <code>v</code> if and only if 
     * there exists an edge <code>e</code> such that 
     * <code>v.isSource(e) == true</code> and 
     * <code>this.isDest(e) == true</code>.
     * 
     * The behavior of this method is undefined if <code>v</code> is not
     * an element of this vertex's graph.
     * 
     * @see ArchetypeVertex#isNeighborOf(ArchetypeVertex)
     * @see #getSuccessors()
     */
    public boolean isSuccessorOf(Vertex v);

    /**
     * Returns <code>true</code> if this vertex is a predecessor of
     * the specified vertex <code>v</code>, and <code>false</code> otherwise.
     * This vertex is a predecessor of <code>v</code> if and only if 
     * there exists an edge <code>e</code> such that 
     * <code>this.isSource(e) == true</code> and 
     * <code>v.isDest(e) == true</code>.
     * 
     * The behavior of this method is undefined if <code>v</code> is not
     * an element of this vertex's graph.
     * 
     * @see ArchetypeVertex#isNeighborOf(ArchetypeVertex)
     * @see #getPredecessors()
     */
    public boolean isPredecessorOf(Vertex v);

    /**
     * Returns <code>true</code> if this vertex is a source of
     * the specified edge <code>e</code>, and <code>false</code> otherwise.
     * A vertex <code>v</code> is a source of <code>e</code> if <code>e</code>
     * is an outgoing edge of <code>v</code>.
     * 
     * The behavior of this method is undefined if <code>e</code> is not
     * an element of this vertex's graph.
     * 
     * @see DirectedEdge#getSource()
     * @see ArchetypeVertex#isIncident(ArchetypeEdge)
     */
    public boolean isSource(Edge e);

    /**
     * Returns <code>true</code> if this vertex is a destination of
     * the specified edge <code>e</code>, and <code>false</code> otherwise.
     * A vertex <code>v</code> is a destination of <code>e</code> 
     * if <code>e</code> is an incoming edge of <code>v</code>.
     * 
     * The behavior of this method is undefined if <code>e</code> is not
     * an element of this vertex's graph.
     * 
     * @see DirectedEdge#getDest()
     * @see ArchetypeVertex#isIncident(ArchetypeEdge)
     */
    public boolean isDest(Edge e);

    /**
     * Returns a directed outgoing edge from this vertex to <code>v</code>,
     * or an undirected edge that connects this vertex to <code>v</code>.  
     * (Note that a directed incoming edge from <code>v</code> to this vertex
     * will <b>not</b> be returned: only elements of the edge set returned by 
     * <code>getOutEdges()</code> will be returned by this method.)
     * If this edge is not uniquely
     * defined (that is, if the graph contains more than one edge connecting 
     * this vertex to <code>v</code>), any of these edges 
     * <code>v</code> may be returned.  <code>findEdgeSet(v)</code> may be 
     * used to return all such edges.
     * If <code>v</code> is not connected to this vertex, returns 
     * <code>null</code>.
     * 
     * @see Vertex#findEdgeSet(Vertex)
     */
    public Edge findEdge(Vertex v);

    /**
     * Returns the set of all edges that connect this vertex
     * with the specified vertex <code>v</code>.  Each edge in this set
     * will be either a directed outgoing edge from this vertex to 
     * <code>v</code>, or an undirected edge connecting this vertex to 
     * <code>v</code>.  <code>findEdge(v)</code> may be used to return
     * a single (arbitrary) element of this set.
     * If <code>v</code>
     * is not connected to this vertex, returns an empty <code>Set</code>.
     * 
     * @see Vertex#findEdge(Vertex)
     */
    public Set findEdgeSet(Vertex v);
}
