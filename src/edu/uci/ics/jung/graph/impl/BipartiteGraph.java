/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Aug 6, 2003
 */
package edu.uci.ics.jung.graph.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.UserData;

/**
 * A Bipartite graph is divided into A vertices and B vertices. Edges
 * only connect A vertices to B vertices, and vice versa. This class
 * extends <i>UndirectedSparseGraph</i>; thus, the Graph is made up of
 * <i>UndirectedSparseVertices</i>. <p>
 * Vertices can only be added to the graph with a flag that says
 * which class they will be added to (using <i>BipartiteGraph.Choice</i> );
 * edges must be of type <i>BipartiteGraph</i>, which must consist of two 
 * vertices, one each from CLASSA and CLASSB. 
 * <pre>
 * BipartiteGraph bpg = new BipartiteGraph;
 * Vertex va = bpg.addVertex( new UndirectedSparseVertex(), BipartiteGraph.CLASSA );
 * Vertex vb = bpg.addVertex( new UndirectedSparseVertex(), BipartiteGraph.CLASSB );
 * bpg.addBipartiteEdge( new BipartiteEdge( va, vb ));
 * </pre>
 * Note that the traditional <i>addVertex()</i> and <i>addEdge()</i> will
 * both throw a <i>FatalException</i>.<p>
 * The function <i>fold</i> creates an <i>UndirectedGraph</i>
 * based on finding vertices that share a common neighbor.
 * 
 * @author danyelf
 * @since 1.0.1
 */
public class BipartiteGraph extends UndirectedSparseGraph {

	private Set aSet = new HashSet();
	private Set bSet = new HashSet();

	public void initialize() {
		super.initialize();
		aSet = new HashSet();
		bSet = new HashSet();	
	}

	/**
	 * Returns the set of all vertices from that class. 
	 * All vertices in the return set will be of class
	 * A or class B, depending on the parameter.
	 */
	public Set getAllVertices( Choice choice ) {
		if (choice == CLASSA ) {
			return Collections.unmodifiableSet(aSet);
		} else if (choice == CLASSB ) {
			return Collections.unmodifiableSet(bSet);
		} else
            throw new IllegalArgumentException("Invalid partition specification " + choice);
	}

//	public ArchetypeGraph copy() {
//		System.out.println( this.getClass() );
//		ArchetypeGraph c = newInstance();
//		for (Iterator iter = getVertices().iterator(); iter.hasNext();) {
//			ArchetypeVertex av = (ArchetypeVertex) iter.next();
//			ArchetypeVertex avNew = av.copy(c);
//		}
//		for (Iterator iter = getEdges().iterator(); iter.hasNext();) {
//			ArchetypeEdge ae = (ArchetypeEdge) iter.next();
//			ae.copy(c);
//		}
//		c.importUserData(this);
//		return c;
//
//	}

    /**
     * Returns the partition for vertex <code>v</code>.
     * @param v
     */
    public Choice getPartition(BipartiteVertex v)
    {
    	if (aSet.contains(v))
            return CLASSA;
        else if (bSet.contains(v))
            return CLASSB;
        else {
        	if ( v.getGraph() == this ) 
        		throw new IllegalArgumentException("Inconsistent state in graph!");
			throw new IllegalArgumentException("Vertex " + v + " is not part of this graph");        	
        }
    }
    
	/**
	 * Adds a single vertex to the graph in the specified partition.
	 * Note that the vertex must be compatible with BipartiteVertex.
	 * 
     * <p>Throws an <code>IllegalArgumentException</code>
     * if <code>v</code> is not an element of either partition.</p>
     * 
	 * @param v			the vertex to be added to the class
	 * @param choice	the class to which the vertex should be added
	 * @return the input vertex
	 */
	public BipartiteVertex addVertex(BipartiteVertex v, Choice choice) {
        String exists = "Specified partition already contains vertex ";
        String dup = "Another partition already contains vertex ";
		if (choice == CLASSA ) 
        {
            if (aSet.contains(v))
                throw new IllegalArgumentException(exists + v);
            if (bSet.contains(v))
                throw new IllegalArgumentException(dup + v);
			aSet.add(v);
		} 
        else if (choice == CLASSB )
        {
            if (bSet.contains(v))
                throw new IllegalArgumentException(exists + v);
            if (aSet.contains(v))
                throw new IllegalArgumentException(dup + v);
			bSet.add(v);			
		}
        else 
            throw new IllegalArgumentException("Invalid partition specification for vertex " + v + ": " + choice);
        super.addVertex(v);
		return v;
	}
	
	/**
	 * Adds a BipartiteEdge to the Graph. This function is simply a 
	 * typed version of addEdge
	 * @param bpe a BipartiteEdge
	 * @return the edge, now a member of the graph.
	 */
	public BipartiteEdge addBipartiteEdge(BipartiteEdge bpe) {
		return (BipartiteEdge) super.addEdge(bpe);
	}

	/**
	 * DO NOT USE THIS METHOD. Contractually required, but merely throws a FatalException. 
	 * @see edu.uci.ics.jung.graph.impl.UndirectedSparseGraph#addEdge(edu.uci.ics.jung.graph.Edge)
	 * @deprecated Use addBipartiteEdge
	 */
	public Edge addEdge(Edge ae) {
		throw new FatalException("Only add BipartiteEdges");
	}

	/**
	 * DO NOT USE THIS METHOD. Contractually required, but merely throws a FatalException. 
	 * @see edu.uci.ics.jung.graph.impl.UndirectedSparseGraph#addVertex(edu.uci.ics.jung.graph.Vertex)
	 * @deprecated Use addBipartiteVertex
	 */
	public Vertex addVertex(Vertex av) {
		throw new FatalException("Use addVertexX to add vertices to a BipartiteGraph ");
	}
	
	/**
	 * This small enumerated type merely forces a user to pick class "A"
	 * or "B" when adding a Vertex to a BipartiteGraph.
	 */
	public static final class Choice {
	}
	public static final Choice CLASSA = new Choice();
	public static final Choice CLASSB = new Choice();

	/**
	 * Creates a one-part graph from a bipartite graph by folding
	 * Vertices from one class into a second class. This function
	 * creates a new UndirectedGraph (with vertex set V') in which: <br>
	 * <ul>
	 * <li> each vertex in V' has an equivalent V in bpg.getAllVertices( class ) </li>
	 * <li> an edge E' joins V'1 and V'2 iff there is a path of length 2 from
	 * V1 to V2 (by way of some third vertex in the other class, VXs) </li>
	 * <li> each edge E' is annotated with the set of vertices VXs </li>
	 * </ul>
	 * 
	 * In social network analysis and related fields, this operation transforms
	 * an actor-by-event chart into an actor-by-actor chart.
	 * 
	 * @param bpg		The bipartite graph to be folded
	 * @param vertexSet		Chooses the set of vertices to be brought into
	 * 					the new Graph.
	 * @return	an UndirectedSparseGraph.
	 */
	public static Graph fold(BipartiteGraph bpg, Choice vertexSet) {
		Graph newGraph = new UndirectedSparseGraph();
		Set vertices = bpg.getAllVertices( vertexSet );
		for (Iterator iter = vertices.iterator(); iter.hasNext();) {
			BipartiteVertex v = (BipartiteVertex) iter.next();
			v.copy(newGraph);
		}
		
		Set coveredNodes = new HashSet();
		
		for (Iterator iter = vertices.iterator(); iter.hasNext();) {
			BipartiteVertex v = (BipartiteVertex) iter.next();
			coveredNodes.add( v );

			// the set of all Bs that touch this A
			Set hyperEdges = v.getNeighbors();

			// this will ultimately contain a mapping from
			// the next adjacent "A" to the list of "B"s that support that
			// connection (that is, all Bs that run between this A and its neighbor
			MultiMap mm = new MultiHashMap();
			for (Iterator iterator = hyperEdges.iterator();
				iterator.hasNext();
				) {
				Vertex hyperEdge = (Vertex) iterator.next();
				addAll(mm, hyperEdge.getNeighbors(), hyperEdge);
			}
			for (Iterator iterator = mm.keySet().iterator();
				iterator.hasNext();
				) {
				Vertex aVertex = (Vertex) iterator.next();

				if ( coveredNodes.contains( aVertex )) continue;

				Edge newEdge = GraphUtils.addEdge(
					newGraph,
					(Vertex)v.getEqualVertex(newGraph),
					(Vertex)aVertex.getEqualVertex(newGraph));
			    newEdge.addUserDatum(BIPARTITE_USER_TAG, mm.get(aVertex), UserData.SHARED);
			}
		}
		return newGraph;
	}

	/**
	 * The tag for the UserData attached to a single Edge.
	 */
	public static final Object BIPARTITE_USER_TAG = "BipartiteUserTag";

	/**
	 * Adds all pairs (key, value) to the multimap from
	 * the initial set keySet.
	 * @param set
	 * @param hyperEdge
	 */
	private static void addAll(MultiMap mm, Set keyset, Object value) {
		for (Iterator iter = keyset.iterator(); iter.hasNext();) {
			Object key = iter.next();
			mm.put(key, value);
		}
	}

	/* (non-Javadoc)
	 * @see edu.uci.ics.jung.graph.Graph#removeVertex(edu.uci.ics.jung.graph.Vertex)
	 */
	public void removeVertex(Vertex v) {
		super.removeVertex(v);
		if ( aSet.contains(v)) {
			aSet.remove( v);
		} else {
			bSet.remove( v);			
		}
	}

}
