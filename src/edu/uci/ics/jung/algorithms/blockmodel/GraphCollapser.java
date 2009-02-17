/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.blockmodel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.utils.PredicateUtils;

/**
 * This is a skeleton class for collapsing graphs. In particular, it takes in a Graph g
 * and a set of vertices to be collapsed into one, "rootset". It then returns a variant of 
 * the graph in which the root set has been merged into one vertex (of class 
 * CollapsedVertex). The user has the opportunity to override a number of these functions
 * (thus, the need for instantiation only exists for overriding). 
 * 
 * There are several issues to be resolved:
 * <ul>
 * <li>What sort of Collapsed vertex should be created?
 * 		<code>getCollapsedVertex(Set vertices)</code></li>
 * <li>What userdata goes on the collapsed vertex?
 * 		<code>annotateVertex(Vertex collapsedVertex, Set original vertices)</code></li>
 * <li>Should an edge be connected to a given vertex, given a set of vertices?
 * 		<code>	protected boolean shouldAddEdge(
 *					Vertex opposite,
 *					Set rootSet,
 *					Collection edges) </code></li>
 * <li>How do I add, or annotate, an edge from the super vertex to a given vertex?
 * <code> 	public void addDirectedEdges(
		Graph graph, 
		Vertex superVertex,
		Vertex opposite,
		Set relevantEdges) 
	protected void addUndirectedEdge(Vertex opposite, Vertex superVertex, Set relevantEdges) {
	protected boolean shouldAddEdge(
		Vertex opposite,
		Set rootSet,
		Collection edges) {
 * </code></li>
 * </ul>
 * 
 * @author Danyel Fisher
 */
public class GraphCollapser {

	protected static GraphCollapser instance = null;
	
	public static GraphCollapser getInstance() {
		if ( instance == null )
			instance = new GraphCollapser();
		return instance;
	}
	
	protected GraphCollapser() {
	}

	/**
	 * This version collects sets of vertices in an equivalence relation into a single CollapsedVertex.
	 * @param equivalence	A properly-defined EquivalenceRelation representing DISJOINT sets of vertices from the Graph.
	 * @return a copy of the original graph where vertices are replaced by their collapsed equivalents
	 */
	public Graph getCollapsedGraph(EquivalenceRelation equivalence) {
		Graph g = equivalence.getGraph();

		// first, we copy the original graph
		Graph copy = (Graph) g.copy();

		// map FROM set of vertices TO supervertex
		Map superVertices = new HashMap();

		replaceEquivalencesWithCollapsedVertices(
			equivalence,
			copy,
			superVertices);

		// copy currently has NONE of the edges (ooh!) running from
		// ANY of the root sets to any other

		// need to characterise all edges as:
		// A) from outside to outside	
		//          -- already set
		// B) from CollapsdVertex to CollapsedVertex
		//          -- may summarize several edges into one
		// C) from CollapsedVertex to outside
		//          -- may summarize several edges into one
		// we've already taken care of (B); this takes care of (C)

		// so let's go through each CollapsedVertex and classify each by the next
		Set coveredCV = new HashSet();

		for (Iterator iter = superVertices.values().iterator();
			iter.hasNext();
			) {
			CollapsedVertex cv = (CollapsedVertex) iter.next();

			// collect all edges and vertices connected to this SuperVertex
			MultiMap vertices_to_edges =
				findEdgesAndVerticesConnectedToRootSet(cv.getRootSet());

			// ok, at this point we've got a map from all adjacent vertices to edges
			collapseVerticesIntoSuperVertices(
				equivalence,
				superVertices,
				vertices_to_edges);

			//			for (Iterator iterator = vertices_to_edges.keySet().iterator();
			//				iterator.hasNext();
			//				) {
			//				Vertex v = (Vertex) iterator.next();
			//				if (equivalence.getEquivalenceRelationContaining(v) != null) {
			//					System.out.println("Panic " + v);
			//					throw new FatalException("Damn.");
			//				}
			//			}

			createEdgesCorrespondingToMap(
				copy,
				cv,
				vertices_to_edges,
				coveredCV);

			coveredCV.add(cv);

		}

		return copy;
	}

	/**
	 * INTERNAL METHOD
	 */
	protected  void createEdgesCorrespondingToMap(
		Graph copy,
		CollapsedVertex cv,
		MultiMap vertices_to_edges,
		Set coveredCV) {
		for (Iterator iter = vertices_to_edges.keySet().iterator();
			iter.hasNext();
			) {

			Vertex edgeDestination = (Vertex) iter.next();

			// this line does nothing for CVs, but is useful for other vertices
			// opposite is either a CollapsedVertex, or it's a copy of the origial
			// if we've already seen it, we should skip it; we've already done those edges
			if (coveredCV.contains(edgeDestination))
				continue;

			Set relevantEdges =
				new HashSet(
					(Collection) vertices_to_edges.get(edgeDestination));

			edgeDestination =
				(Vertex) edgeDestination.getEqualVertex(copy);

			if (shouldAddEdge(edgeDestination,
				cv.getRootSet(),
				relevantEdges)) {

				if (PredicateUtils.enforcesEdgeConstraint(copy, Graph.DIRECTED_EDGE)) 
					createDirectedEdges(copy, cv, edgeDestination, relevantEdges);
				else if (PredicateUtils.enforcesEdgeConstraint(copy, Graph.UNDIRECTED_EDGE))
					createUndirectedEdge(copy, cv, edgeDestination, relevantEdges);
                else
                    throw new IllegalArgumentException("Mixed (directed/undirected) " +
                        "graphs not currently supported");
			}
		}

	}

	/**
	 * Internal method for collapsing a set of vertexes.
	 * 
	 * @param er
	 * @param superVertices
	 * @param vertices_to_edges
	 */
	protected  void collapseVerticesIntoSuperVertices(
		EquivalenceRelation er,
		Map superVertices,
		MultiMap vertices_to_edges) {

		Set vertices = new HashSet(vertices_to_edges.keySet());
		// some of these vertices may be parts of one or another root set
		for (Iterator destinations = vertices.iterator();
			destinations.hasNext();
			) {
			Vertex dest = (Vertex) destinations.next();
			Set destSet = er.getEquivalenceRelationContaining(dest);
			if (destSet != null) {
				CollapsedVertex superV =
					(CollapsedVertex) superVertices.get(destSet);
				replaceWith(vertices_to_edges, dest, superV);
			}
		}
	}

	/**
	 * INTERNAL (undocumented) method
	 * @param m
	 * @param dest
	 * @param superV
	 */
	protected  void replaceWith(MultiMap m, Vertex dest, CollapsedVertex superV) {
		Collection c = (Collection) m.get(dest);
		for (Iterator iter = c.iterator(); iter.hasNext();) {
			m.put(superV, iter.next());
		}
		m.remove(dest);
	}

	/**
	 * INTERNAL (undocumented) method.
	 * @param er
	 * @param copy
	 * @param superVertices
	 */
	protected void replaceEquivalencesWithCollapsedVertices(
		EquivalenceRelation er,
		Graph copy,
		Map superVertices) {
		// and remove our set to merge
		for (Iterator iter = er.getAllEquivalences(); iter.hasNext();) {
			Set rootSet = (Set) iter.next();
			CollapsedVertex superVertex = createCollapsedVertex(copy, rootSet);
			for (Iterator iter2 = rootSet.iterator(); iter2.hasNext();) {
				Vertex v = (Vertex) iter2.next();
				copy.removeVertex((Vertex) v.getEqualVertex(copy));
			}
			annotateVertex(superVertex, rootSet);
			superVertices.put(rootSet, superVertex);
		}
	}

	/**
	 * This function collapses a series of vertices in one
	 * EquivalenceSet into one
	 * CollapsedVertex. 
	 * @param g		A graph to collapse vertices from
	 * @param rootSet	A set of vertice to collapse into one CollapsedVertex
	 * @return		A graph with rootset.size()-1 fewer vertices.
	 */
	public Graph getCollapsedGraph(Graph g, Set rootSet) {

		// first, we copy the original graph
		Graph copy = (Graph) g.copy();

		// and remove our set to merge
		for (Iterator iter = rootSet.iterator(); iter.hasNext();) {
			Vertex v = (Vertex) iter.next();
			copy.removeVertex((Vertex) v.getEqualVertex(copy));
		}

		// and create one new vertex
		CollapsedVertex superVertex = createCollapsedVertex(copy, rootSet);
		annotateVertex(superVertex, rootSet);

		MultiMap vertices_to_edges =
			findEdgesAndVerticesConnectedToRootSet(superVertex.getRootSet());

		for (Iterator iter = vertices_to_edges.keySet().iterator();
			iter.hasNext();
			) {
			Vertex opposite = (Vertex) iter.next();
			opposite = (Vertex) opposite.getEqualVertex(copy);
			Set relevantEdges =
				new HashSet((Collection) vertices_to_edges.get(opposite));

			if (shouldAddEdge(opposite,
				superVertex.getRootSet(),
				relevantEdges)) {

				if (PredicateUtils.enforcesEdgeConstraint(g, Graph.DIRECTED_EDGE)) {
					createDirectedEdges(
						copy,
						superVertex,
						opposite,
						relevantEdges);
				} else if (PredicateUtils.enforcesEdgeConstraint(g, Graph.UNDIRECTED_EDGE)){
					createUndirectedEdge(
						copy,
						superVertex,
						opposite,
						relevantEdges);
				}
                else 
                    throw new IllegalArgumentException("Mixed (directed/undirected" + 
                        " graphs not currently supported");
			}
		}

		return copy;
	}

	/**
	 * Overridable method annotates the new collapsed vertex with userdata
	 * from the rootset. By default, does nothing.
	 * @param superVertex a new CollapsedVertex 
	 * @param rootSet a set of Vertexes from the old graph.
	 */
	protected void annotateVertex(CollapsedVertex superVertex, Set rootSet) {
	}
	
	/**
	 * Overridable method annotates the new collapsed edge with userdata
	 * from the original set. By default, does nothing.
	 * @param newEdge a new CollapsedEdge
	 * @param edgesFromWhichWeMightDeriveData 
	 */
	protected void annotateEdge(
			CollapsedEdge newEdge,
			Collection edgesFromWhichWeMightDeriveData) {
	}
	

	/**
	 * Overridable method to create a single vertex representing a set of vertices in the
	 * graph.
	 * @param g	The input graph 
	 * @param rootSet	The set of vertices which should be represented by the
	 * new vertex.
	 * @return a new CollapsedVertex
	 */
	protected CollapsedVertex createCollapsedVertex(Graph g, Set rootSet) {
		return (CollapsedVertex) g.addVertex(new CollapsedSparseVertex(rootSet));
	}

	/**
	 * Overridable method to create a single undirected edge that represents the data in its parameters. 
	 * Should call annotateEdge with the new edge.
	 * @param g		The graph in which this edge should be added
	 * @param opposite	The vertex at the far end of this edge
	 * @param superVertex The vertex at the near end of this edge. (For an undirecte
	 * graph, it doesn't really matter).
	 * @param relevantEdges The set of edges that this edge is meant to represent.
	 */
	protected void createUndirectedEdge(
		Graph g,
		CollapsedVertex superVertex,
		Vertex opposite,
		Set relevantEdges) {
		CollapsedEdge newEdge =
			(CollapsedEdge) g.addEdge(
				new UndirectedCollapsedEdge(
					opposite,
					superVertex,
					relevantEdges));
			annotateEdge(newEdge, relevantEdges);
	}

	/**
	 * Overridable method to create a up to two directed edges that represents the data in its parameters. 
	 * This method, by default, creates one edge in each direction that there is an edge in
	 * relevantEdges. 
	 * Should call annotateEdge with the new edge.
	 * @param g		The graph in which this edge should be added
	 * @param opposite	The vertex at the far end of this edge
	 * @param superVertex The vertex at the near end of this edge. (For an undirecte
	 * graph, it doesn't really matter).
	 * @param relevantEdges The set of edges that this edge is meant to represent.
	 */
	protected void createDirectedEdges(
		Graph graph,
		CollapsedVertex superVertex,
		Vertex opposite,
		Set relevantEdges) {
		
//		System.out.println("Creating " + superVertex + " " + opposite );
//		System.out.println( relevantEdges );
		
		// sort edges by directionality
		Set oppositeToSup = new HashSet(), supToOpposite = new HashSet();
		// from here to there, from there to here, funny things are everyhere! -- Seuss

		for (Iterator iterator = relevantEdges.iterator();
			iterator.hasNext();
			) {
			DirectedEdge de = (DirectedEdge) iterator.next();
			if (superVertex.getRootSet().contains( de.getSource() )) {
				supToOpposite.add(de);
			} else {
				oppositeToSup.add(de);
			}
		}


		// is there an edge from HERE to THERE?
		if (oppositeToSup.size() > 0) {
			CollapsedEdge newEdge =
				(CollapsedEdge) graph.addEdge(
					new DirectedCollapsedEdge(
						opposite,
						superVertex,
						oppositeToSup));
			annotateEdge(newEdge, oppositeToSup);
//			System.out.println("  [1]" + newEdge);
		}
		// is there an edge from THERE to HERE?					
		if (supToOpposite.size() > 0) {
			CollapsedEdge newEdge =
				(CollapsedEdge) graph.addEdge(
					new DirectedCollapsedEdge(
						superVertex,
						opposite,
						supToOpposite));
			annotateEdge(newEdge, supToOpposite);
//			System.out.println("  [2]" + newEdge);
		}
	}

	/**
	 * INTERNAL METHOD.
	 * For a set of vertices, finds all the edges connected to them, indexed (in a MultiMap)
	 * to the vertices to which they connect. Thus, in the graph with edges (A-C, A-D, B-C), 
	 * with input (A, B), the result will be ( C {A-C, B-C}; D {A-D} )
	 * @param rootSet
	 * @return
	 */
	protected MultiMap findEdgesAndVerticesConnectedToRootSet(Set rootSet) {
		// now, let's get a candidate set of edges
		MultiMap vertices_to_edges = new MultiHashMap();

		for (Iterator iter = rootSet.iterator(); iter.hasNext();) {
			Vertex v = (Vertex) iter.next();
			for (Iterator iterator = v.getIncidentEdges().iterator();
				iterator.hasNext();
				) {
				Edge e = (Edge) iterator.next();
				Vertex other = e.getOpposite(v);
				if (rootSet.contains(other))
					continue;
				vertices_to_edges.put(other, e);
			}
		}
		return vertices_to_edges;
	}


	/**
	 * Overridable method checks whether an edge representing
	 * the given set of edges should be created. The edge will
	 * run from a new CollapsedVertex representing the rootSet
	 * to opposite; it will replace all the edges in the collection
	 * edges. By default, this method returns true.
	 * @param opposite
	 * @param rootSet a set of vertices that currently are one end
	 * of these edges.
	 * @param edges	a non-empty collection of edges that may be
	 * replaced
	 * @return	a boolean value. If true, the system will replace
	 * these edges with one new collapsededge; if false, the system
	 * will remove all these edges from the graph.
	 */
	protected boolean shouldAddEdge(
		Vertex opposite,
		Set rootSet,
		Collection edges) {
		return true;
	}
	
/**
 * This interface represents a vertex that holds a set of objects in some other graph.
 */
	public interface CollapsedVertex extends Vertex {
		public Set getRootSet();		
	}

	/**
	 * A CollapsedSparseVertex extends CollapsedVertex.
	 */
	public static class CollapsedSparseVertex extends SparseVertex implements CollapsedVertex {

		private Set rootSet;

		/**
		 * @param rootSet
		 */
		public CollapsedSparseVertex (Set rootSet) {
			this.rootSet = rootSet;
		}

		public String toString() {
			return super.toString() + ":" + rootSet;
		}

		public Set getRootSet() {
			return rootSet;
		}

	}

	/**
	 * The CollapsedEdge interface represents a set of edges
	 * in some other graph.
	 */
	public interface CollapsedEdge extends Edge {
		public Set getRelevantEdges();
	}

	/**
	 * This class represents a Collapsed Undirected edge,
	 * and extends UndirectedSparseEdge.
	 */
	public static class UndirectedCollapsedEdge
		extends UndirectedSparseEdge
		implements CollapsedEdge {

		private Set relevantEdges;

		public UndirectedCollapsedEdge(
			Vertex opposite,
			Vertex superVertex,
			Set relevantEdges) {
			super(opposite, superVertex);
			this.relevantEdges = relevantEdges;
		}

		public Set getRelevantEdges() {
			return relevantEdges;
		}

	}

	/**
	 * This class represents a Collapsed Directed edge,
	 * and extends DirectedSparseEdge.
	 */
	public static class DirectedCollapsedEdge
		extends DirectedSparseEdge
		implements CollapsedEdge {
		private Set relevantEdges;

		public DirectedCollapsedEdge(
			Vertex opposite,
			Vertex superVertex,
			Set relevantEdges) {
			super(opposite, superVertex);
			this.relevantEdges = relevantEdges;
		}

		public Set getRelevantEdges() {
			return relevantEdges;
		}

	}

}
