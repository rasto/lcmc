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

import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.InstanceofPredicate;
import org.apache.commons.collections.functors.NotPredicate;

import edu.uci.ics.jung.graph.predicates.ParallelEdgePredicate;
import edu.uci.ics.jung.graph.predicates.SimpleEdgePredicate;

/**
 * A specific type of <code>ArchetypeGraph</code> which consists of 
 * a <code>Vertex</code> set and an <code>Edge</code> set.
 * Instances of <code>Graph</code> may contain either directed or undirected
 * edges, but not both.
 * 
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 * @author Scott White
 * 
 * @see Edge
 * @see Vertex
 */
public interface Graph extends ArchetypeGraph 
{
    // commonly used global predicates
    public static final Predicate DIRECTED_EDGE = 
        InstanceofPredicate.getInstance(DirectedEdge.class);
    public static final Predicate UNDIRECTED_EDGE = 
        InstanceofPredicate.getInstance(UndirectedEdge.class);
    public static final Predicate NOT_PARALLEL_EDGE = 
        NotPredicate.getInstance(ParallelEdgePredicate.getInstance());
    public static final Predicate SIMPLE_EDGE = 
        SimpleEdgePredicate.getInstance();
	
    /**
     * Returns <code>true</code> if each edge of this graph is directed, 
     * and <code>false</code> if each edge of this graph is undirected.
     * If some edges are directed and some are not, throws 
     * <code>FatalException</code>.
     * 
     * @deprecated As of version 1.4, replaced by 
     * {@link edu.uci.ics.jung.graph.utils.PredicateUtils#enforcesDirected(Graph)}
     * and {@link edu.uci.ics.jung.graph.utils.PredicateUtils#enforcesUndirected(Graph)}.
     */
    public boolean isDirected();

    /**
     * Adds <code>v</code> to this graph, and returns a reference to the 
     * added vertex.
     * @param v    the vertex to be added
     */
    public Vertex addVertex(Vertex v);
    
    /**
     * Adds <code>e</code> to this graph, and returns a reference to the
     * added vertex.
     * 
     * @param e     the edge to be added
     */
    public Edge addEdge(Edge e);


    /**
     * Removes <code>v</code> from this graph.  Any edges incident to 
     * <code>v</code> which become ill-formed (as defined in the documentation
     * for <code>ArchetypeEdge</code>)
     * as a result of removing <code>v</code>
     * are also removed from this graph.  Throws 
     * <code>IllegalArgumentException</code> if <code>v</code> is not 
     * in this graph.
     */
    public void removeVertex(Vertex v);
    
    /**
     * Removes <code>e</code> from this graph.  Throws 
     * <code>IllegalArgumentException</code> if <code>e</code> is not 
     * in this graph.
     */
    public void removeEdge(Edge e);

}
