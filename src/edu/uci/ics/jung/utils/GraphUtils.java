/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Jun 25, 2003
 */
package edu.uci.ics.jung.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import cern.colt.list.DoubleArrayList;
import edu.uci.ics.jung.algorithms.transformation.DirectionTransformer;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Hyperedge;
import edu.uci.ics.jung.graph.Hypergraph;
import edu.uci.ics.jung.graph.Hypervertex;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.decorators.NumberVertexValue;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.StringLabeller.UniqueLabelException;
import edu.uci.ics.jung.graph.filters.UnassembledGraph;
import edu.uci.ics.jung.graph.filters.impl.DropSoloNodesFilter;
import edu.uci.ics.jung.graph.impl.AbstractSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.DirectedSparseVertex;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;

/**
 * 
 * A series of helpful utility methods. All methods in GraphUtils can be
 * accomplished with public members of other code; these are simply
 * combinations that we found useful.
 * 
 * @author Danyel Fisher, Scott White, Joshua O'Madadhain
 */
public class GraphUtils
{

	/**
	 * Adds an appropriate edge between two vertices. Specifically, this method
	 * confirms that both vertices are from the same graph, and then checks
	 * whether the Graph is directed or not. If
	 * so, it creates a new
	 * {@link  edu.uci.ics.jung.graph.impl.DirectedSparseEdge DirectedSparseEdge},
	 * otherwise a new
	 * {@link  edu.uci.ics.jung.graph.impl.UndirectedSparseEdge UndirectedSparseEdge}.
	 * This is a convenience method; one might instead just call <code> g.addEdge( new XXSparseEdge(v1, v2))) </code>.
	 * <p>
	 * The input vertices must be of type {@link edu.uci.ics.jung.graph.Vertex},
	 * or the method will throw a <code>ClassCastException</code>.
	 * 
	 * @throws ClassCastException
	 *             if the input aren't Vertices
	 * @throws IllegalArgumentException
	 *             if the vertices don't belong to the same graph
	 * 
	 * @return the edge that was added
	 * 
	 * @see edu.uci.ics.jung.graph.Graph#addEdge
	 * @see edu.uci.ics.jung.graph.impl.AbstractSparseGraph#addEdge
	 */
	public static Edge addEdge(Graph g, Vertex v1, Vertex v2)
	{
		if (v1.getGraph() != g || v2.getGraph() != g)
			throw new IllegalArgumentException("Vertices not in this graph!");
		if (PredicateUtils.enforcesEdgeConstraint(g, Graph.DIRECTED_EDGE))
		{
			return (AbstractSparseEdge) g.addEdge(
				new DirectedSparseEdge(v1, v2));
		}
        else if (PredicateUtils.enforcesEdgeConstraint(g, Graph.UNDIRECTED_EDGE))
		{
			return (AbstractSparseEdge) g.addEdge(
				new UndirectedSparseEdge(v1, v2));
		}
        else
            throw new IllegalArgumentException("Behavior not specified " +
                    "for mixed (directed/undirected) graphs");
	}

	/**
	 * Adds <code>count</code> vertices into a graph. This is a convenience
	 * method; one might instead just call <code> g.addVertex( new SparseVertex())) </code>
	 * <code>count</code>
	 * times.
	 * <p>
	 * The input graph must be one that can accept a series of
	 * {@link edu.uci.ics.jung.graph.impl.SparseVertex directed vertices}.
	 * 
	 * @param g
	 *            A graph to add the vertices to
	 * @param count
	 *            how many vertices to add
	 * 
	 * @see edu.uci.ics.jung.graph.impl.AbstractSparseGraph#addVertex
	 */
	public static void addVertices(Graph g, int count)
	{
		for (int i = 0; i < count; i++)
			g.addVertex(new SparseVertex());
	}

	/**
	 * Adds <code>count</code> directed vertices into a graph. This is a
	 * convenience method; one might instead just call <code> g.addVertex( new DirectedSparseVertex())) </code>
	 * <code>count</code>
	 * times.
	 * <p>
	 * The input graph must be one that can accept a series of
	 * {@link edu.uci.ics.jung.graph.impl.DirectedSparseVertex directed vertices}.
	 * 
	 * @param g
	 *            A graph to add the vertices to
	 * @param count
	 *            how many vertices to add
	 * 
	 * @see edu.uci.ics.jung.graph.impl.AbstractSparseGraph#addVertex
	 * @deprecated As of version 1.2, replaced by {@link #addVertices}.
	 */
	public static void addDirectedVertices(Graph g, int count)
	{
		for (int i = 0; i < count; i++)
		{
			g.addVertex(new DirectedSparseVertex());
		}
	}

	/**
	 * Adds <code>count</code> undirected vertices into a graph. This is a
	 * convenience method; one might instead just call <code> g.addVertex( new UndirectedSparseVertex())) </code>
	 * <code>count</code>
	 * times.
	 * <p>
	 * The input graph must be one that can accept a series of
	 * {@link edu.uci.ics.jung.graph.impl.UndirectedSparseVertex undirected vertices}.
	 * 
	 * @param g
	 *            A graph to add the vertices to
	 * @param count
	 *            how many vertices to add
	 * 
	 * @see edu.uci.ics.jung.graph.impl.AbstractSparseGraph#addVertex
	 * @deprecated As of version 1.2, replaced by {@link #addVertices}.
	 */
	public static void addUndirectedVertices(Graph g, int count)
	{
		for (int i = 0; i < count; i++)
		{
			g.addVertex(new UndirectedSparseVertex());
		}
	}

	/**
	 * Translates every vertex from the input <code>set</code> into the graph
	 * given. For each vertex, then, it gets the equivalent vertex in <code>g</code>,
	 * and returns the collated set.
	 * 
	 * @param s
	 *            The set of input vertices, not from g
	 * @param g
	 *            The graph which has the corresponding vertices
	 * 
	 * @return a resulting set
	 * 
	 * @see edu.uci.ics.jung.graph.ArchetypeVertex#getEqualVertex
     * @deprecated As of version 1.4, replaced by {@link GraphUtils#getEqualVertices(Set, ArchetypeGraph)}
	 */
	public static Set translateAll(Set s, Graph g)
	{
        return getEqualVertices(s, g);
	}

    /**
     * Returns the set of vertices in <code>g</code> which are equal
     * to the vertices in <code>g</code>.
     * @since 1.4
     */
    public static Set getEqualVertices(Set s, ArchetypeGraph g)
    {
        Set rv = new HashSet();
        for (Iterator iter = s.iterator(); iter.hasNext();)
        {
            ArchetypeVertex v = (ArchetypeVertex) iter.next();
            ArchetypeVertex v_g = v.getEqualVertex(g);
            if (v_g != null)
                rv.add(v_g);
        }
        return rv;
    }
    
	/**
	 * Translates every edge from the input <code>set</code> into the graph
	 * given. For each edge, then, it gets the equivalent edge in <code>g</code>,
	 * and returns the collated set.
	 * 
	 * @param s
	 *            The set of input edges, not from g
	 * @param g
	 *            The graph which has the corresponding edges
	 * 
	 * @return a resulting set
	 * 
	 * @see edu.uci.ics.jung.graph.ArchetypeEdge#getEqualEdge
     * @deprecated As of version 1.4, replaced by {@link GraphUtils#getEqualEdges(Set, ArchetypeGraph)}
	 */
	public static Set translateAllEdges(Set s, Graph g)
	{
        return getEqualEdges(s, g);
	}

    /**
     * Returns the set of edges in <code>g</code> which are equal
     * to the edges in <code>g</code>.
     * @since 1.4
     */
    public static Set getEqualEdges(Set s, ArchetypeGraph g)
    {
        Set rv = new HashSet();
        for (Iterator iter = s.iterator(); iter.hasNext();)
        {
            ArchetypeEdge e = (ArchetypeEdge) iter.next();
            ArchetypeEdge e_g = e.getEqualEdge(g);
            if (e_g != null)
                rv.add(e_g);
        }
        return rv;
    }

	/**
	 * Given a set of vertices, creates a new <tt>Graph</tt> that contains
	 * all of those vertices, and all the edges that connect them. Uses the
	 * <tt>{@link edu.uci.ics.jung.graph.filters.UnassembledGraph UnassembledGraph}</tt>
	 * mechanism to create the graph.
	 * 
	 * @param s
	 *            A set of <tt>Vertex</tt> s that want to be a part of a new
	 *            Graph
	 * @return A graph, created with <tt>{@link edu.uci.ics.jung.graph.Graph#newInstance Graph.newInstance}</tt>,
	 *         containing vertices equivalent to (and that are copies of!) all
	 *         the vertices in the input set. Note that if the input is an
	 *         empty set, <tt>null</tt> is returned.
	 */
	public static Graph vertexSetToGraph(Set s)
	{
		if (s.isEmpty())
			return null;
		Vertex v = (Vertex) s.iterator().next();
		Graph g = (Graph) v.getGraph();
		return new UnassembledGraph("vertexSetToGraph", s, g.getEdges(), g)
			.assemble();
	}

	/**
	 * Given a set of edges, creates a new <tt>Graph</tt> that contains all
	 * of those edges, and at least all the vertices that are attached to them.
	 * Uses <tt>{@link edu.uci.ics.jung.graph.filters.UnassembledGraph UnassembledGraph}</tt>
	 * mechanism to create the graph. The parameter decides what to do with
	 * disconnected vertices: <tt>true</tt> says that they should be
	 * retained, <tt>false</tt> says that they should be discarded (with a
	 * {@link edu.uci.ics.jung.graph.filters.impl.DropSoloNodesFilter DropSoloNodesFilter}).
	 * 
	 * @param edges
	 *            A set of <tt>Edge</tt> s that want to be a part of a new
	 *            <tt>Graph</tt>
	 * @param retain
	 *            Is true if all isolated vertices should be retained; is false if they
	 *            should be discarded.
	 * @return A graph, created with <tt>{@link edu.uci.ics.jung.graph.Graph#newInstance Graph.newInstance}</tt>,
	 *         containing edges equivalent to (and that are copies of!) all the
	 *         edges in the input set. Note that if the input is an empty set,
	 *         <tt>null</tt> is returned.
	 */
	public static Graph edgeSetToGraph(Set edges, boolean retain)
	{
		if (edges.isEmpty())
			return null;
		Edge e = (Edge) edges.iterator().next();
		Graph g = (Graph) e.getGraph();
		Graph retval =
			new UnassembledGraph("edgeSetToGraph", g.getVertices(), edges, g)
				.assemble();
		if (retain)
			return retval;
		else
		{
			return DropSoloNodesFilter.getInstance().filter(retval).assemble();
		}
	}

    /**
     * Returns a graph which consists of the union of the two input graphs.
     * Assumes that both graphs are of a type that can accept the vertices
     * and edges found in both graphs.
     * The resultant graph contains all constraints that are common to both graphs.
     */
    public static ArchetypeGraph union(ArchetypeGraph g1, ArchetypeGraph g2)
    {
        ArchetypeGraph g = g1.newInstance();
//        g.getEdgeConstraints().clear();
        g.getEdgeConstraints().addAll(CollectionUtils.intersection(
                g1.getEdgeConstraints(), g2.getEdgeConstraints()));
        
        Collection vertices = CollectionUtils.union(g1.getVertices(), g2.getVertices());
        Collection edges = CollectionUtils.union(g1.getEdges(), g2.getEdges());
        
        for (Iterator v_iter = vertices.iterator(); v_iter.hasNext(); )
        {
            ArchetypeVertex v = (ArchetypeVertex)v_iter.next();
            v.copy(g);
        }
        
        for (Iterator e_iter = edges.iterator(); e_iter.hasNext(); )
        {
            ArchetypeEdge e = (ArchetypeEdge)e_iter.next();
            e.copy(g);
        }
        return g;
    }
    
	/**
	 * Transforms an (possibly undirected) graph into a directed graph. All user data on
	 * the graph,edges & vertices are copied to their corresponding
	 * counterparts. Returns a new graph with a directed edge from a to b iff a
	 * was a predecessor of b in the original. For an undirected edge, this will create
	 * two new edges.
	 * 
	 * @param uGraph
	 *            the undirected graph to transform
	 * @return the resultant directed graph
     * @deprecated As of version 1.4, replaced by 
     * {@link edu.uci.ics.jung.algorithms.transformation.DirectionTransformer#toDirected(Graph)}
	 */
	public static DirectedGraph transform(Graph uGraph)
	{
	    return DirectionTransformer.toDirected(uGraph);
	}

	/**
	 * Transforms a directed graph into a undirected graph. All user data on
	 * the graph,edges & vertices are copied to their corresponding
	 * counterparts.
	 * 
	 * @param dGraph
	 *            the directed graph to transform
	 * @return the resultant undirected graph
     * @deprecated As of version 1.4, replaced by 
     * {@link edu.uci.ics.jung.algorithms.transformation.DirectionTransformer#toUndirected(Graph)}
	 */
	public static UndirectedGraph transform(DirectedGraph dGraph)
	{
	    return DirectionTransformer.toUndirected(dGraph);
	}

	/**
	 * Copies the labels of vertices from one StringLabeller to another. Only
	 * the labels of vertices that are equivalent are copied.
	 * 
	 * @param source
	 *            the source StringLabeller
	 * @param target
	 *            the target StringLabeller
	 */
	public static void copyLabels(StringLabeller source, StringLabeller target)
		throws UniqueLabelException
	{
		Graph g1 = source.getGraph();
		Graph g2 = target.getGraph();
		Set s1 = g1.getVertices();
		Set s2 = g2.getVertices();

		for (Iterator iter = s1.iterator(); iter.hasNext();)
		{
			Vertex v = (Vertex) iter.next();
			if (s2.contains(v))
			{
				target.setLabel(
					(Vertex) v.getEqualVertex(g2),
					source.getLabel(v));
			}
		}
	}

	/**
	 * Returns true if <code>g1</code> and <code>g2</code> have equivalent
	 * vertex and edge sets (that is, if each vertex and edge in <code>g1</code>
	 * has an equivalent in <code>g2</code>, and vice versa), and false
	 * otherwise.
	 */
	public static boolean areEquivalent(ArchetypeGraph g1, ArchetypeGraph g2)
	{
		return (
			(g1 == g2)
				|| (g1.getVertices().equals(g2.getVertices())
					&& g1.getEdges().equals(g2.getEdges())));
	}

	/**
	 * For every vertex in s, prints sl.get(s). S must be made up of only
	 * vertices.
	 * 
	 * @param s
	 * @param sl
	 */
	public static String printVertices(Collection s, StringLabeller sl)
	{
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		sb.append("[");
		for (Iterator iter = s.iterator(); iter.hasNext();)
		{
			Vertex v = (Vertex) iter.next();
			if (!first)
				sb.append(", ");
			else
				first = false;
			sb.append( sl.getLabel(v) );

		}
		sb.append("]");
		return sb.toString();
	}
    
    
    /**
     * Copies, for each vertex <code>v</code> in <code>g</code>, 
     * <code>source</code>'s value to <code>dest</code>.
     * @param g         the graph from which the vertices are taken
     * @param source    the <code>NumberVertexValue</code> from which values are to be copied
     * @param dest      the <code>NumberVertexValue</code> into which values are to be copied
     */
    public static void copyValues(ArchetypeGraph g, NumberVertexValue source, NumberVertexValue dest)
    {
        for (Iterator iter = g.getVertices().iterator(); iter.hasNext(); )
        {
            ArchetypeVertex v = (ArchetypeVertex)iter.next();
            dest.setNumber(v, source.getNumber(v));
        }
    }
    
    /**
     * Returns the <code>VertexGenerator</code>, if any, stored in <code>g</code>'s
     * user data at the standardized location specified by the VG interface: <code>VertexGenerator.TAG</code>.
     */
    public static VertexGenerator getVertexGenerator(ArchetypeGraph g)
    {
        return (VertexGenerator)g.getUserDatum(VertexGenerator.TAG);
    }
    
    /**
     * Converts <code>vertex_values</code> (a Map of vertex to Number values)
     * to a DoubleArrayList, using <code>indexer</code> to determine the location
     * of each vertex's value in the DAL.  
     * <b>Note</b>: assumes that <code>indexer</code> is 0-based and covers
     * all the vertices in <code>vertex_values</code>, and that 
     * <code>vertex_values</code> contains <code>Number</code> instances.
     * @param vertex_values a map of vertices to <code>Number</code> instances
     * @param indexer a 0-based index of the vertices
     * @return
     */
    public static DoubleArrayList vertexMapToDAL(Map vertex_values, Indexer indexer)
    {
        DoubleArrayList dal = new DoubleArrayList(vertex_values.size());
        // fill dal with "blank" elements
        dal.setSize(vertex_values.size());
        	
        for (Iterator iter = vertex_values.keySet().iterator(); iter.hasNext(); )
        {
            ArchetypeVertex av = (ArchetypeVertex)iter.next();
            double value = ((Number)vertex_values.get(av)).doubleValue();
            dal.set(indexer.getIndex(av), value);
        }
        
        return dal;
    }

    /**
     * Adds all vertices in the specified set to <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.addVertex</code> on all elements
     * of the set.
     * If any element of <code>vertices</code> may not be legally added
     * to this graph, throws an exception: <code>ClassCastException</code> if
     * the type is inappropriate, and <code>IllegalArgumentException</code>  
     * otherwise.  If an exception is thrown, any vertices that may 
     * already have been added are not guaranteed to be retained.
     */
    public static void addVertices(Graph g, Set vertices)
    {
        for (Iterator iter = vertices.iterator(); iter.hasNext(); )
            g.addVertex((Vertex)iter.next());
    }
    
    /**
     * Adds all vertices in the specified set to <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.addVertex</code> on all elements
     * of the set.
     * If any element of <code>vertices</code> may not be legally added
     * to this graph, throws an exception: <code>ClassCastException</code> if
     * the type is inappropriate, and <code>IllegalArgumentException</code>  
     * otherwise.  If an exception is thrown, any vertices that may 
     * already have been added are not guaranteed to be retained.
     */
    public static void addVertices(Hypergraph g, Set vertices)
    {
        for (Iterator iter = vertices.iterator(); iter.hasNext(); )
            g.addVertex((Hypervertex)iter.next());
    }
    
    /**
     * Adds all edges in the specified set to <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.addEdge</code> on all elements
     * of the set.
     * If any element of <code>edges</code> may not be legally added
     * to this graph, throws an exception: <code>ClassCastException</code> if
     * the type is inappropriate, and <code>IllegalArgumentException</code>  
     * otherwise.  If an exception is thrown, any edges that may 
     * already have been added are not guaranteed to be retained.
     */
    public static void addEdges(Graph g, Set edges)
    {
        for (Iterator iter = edges.iterator(); iter.hasNext(); )
            g.addEdge((Edge)iter.next());
    }
    
    /**
     * Adds all edges in the specified set to <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.addEdge</code> on all elements
     * of the set.
     * If any element of <code>edges</code> may not be legally added
     * to this graph, throws an exception: <code>ClassCastException</code> if
     * the type is inappropriate, and <code>IllegalArgumentException</code>  
     * otherwise.  If an exception is thrown, any edges that may 
     * already have been added are not guaranteed to be retained.
     */
    public static void addEdges(Hypergraph g, Set edges)
    {
        for (Iterator iter = edges.iterator(); iter.hasNext(); )
            g.addEdge((Hyperedge)iter.next());
    }
    
    /**
     * Removes all vertices in the specified set from <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.removeVertex</code> on all elements
     * of the set.
     * If any element of <code>vertices</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any vertices that may have been removed already 
     * are not guaranteed to be restored to the graph.     
     */
    public static void removeVertices(Graph g, Set vertices)
    {
        for (Iterator iter = new LinkedList(vertices).iterator(); iter.hasNext(); )
            g.removeVertex((Vertex)iter.next());
    }

    /**
     * Removes all vertices in the specified set from <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.removeVertex</code> on all elements
     * of the set.
     * If any element of <code>vertices</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any vertices that may have been removed already 
     * are not guaranteed to be restored to the graph.     
     */
    public static void removeVertices(Hypergraph g, Set vertices)
    {
        for (Iterator iter = new LinkedList(vertices).iterator(); iter.hasNext(); )
            g.removeVertex((Hypervertex)iter.next());
    }

    /**
     * Removes all vertices in the specified set from <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.removeVertex</code> on all elements
     * of the set.
     * If any element of <code>edges</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any edges that may have been removed already 
     * are not guaranteed to be restored to the graph.  
     */
    public static void removeEdges(Graph g, Set edges)
    {
        for (Iterator iter = new LinkedList(edges).iterator(); iter.hasNext(); )
            g.removeEdge((Edge)iter.next());
    }

    /**
     * Removes all vertices in the specified set from <code>g</code>. Syntactic
     * sugar for a loop that calls <code>g.removeVertex</code> on all elements
     * of the set.
     * If any element of <code>edges</code> is not part of this graph,
     * then throws <code>IllegalArgumentException</code>.  If this 
     * exception is thrown, any edges that may have been removed already 
     * are not guaranteed to be restored to the graph.  
     */
    public static void removeEdges(Hypergraph g, Set edges)
    {
        for (Iterator iter = new LinkedList(edges).iterator(); iter.hasNext(); )
            g.removeEdge((Hyperedge)iter.next());
    }

}
