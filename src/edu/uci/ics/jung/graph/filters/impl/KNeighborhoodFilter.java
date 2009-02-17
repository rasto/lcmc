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
 * Created on Dec 26, 2001
 *
 */
package edu.uci.ics.jung.graph.filters.impl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.filters.Filter;
import edu.uci.ics.jung.graph.filters.UnassembledGraph;
/**
 * A filter used to extract the k-neighborhood around one or more root node(s)
 * @author Danyel Fisher
 *
 */
public class KNeighborhoodFilter implements Filter {
	public static final int IN_OUT = 0;
	public static final int IN = 1;
	public static final int OUT = 2;
	private Set rootNodes;
	private int radiusK;
	private int edgeType;
	
	/**
	 * Constructs a new instance of the filter
	 * @param rootNodes the set of root nodes
	 * @param radiusK the neighborhood radius around the root set
	 * @param edgeType 0 for in/out edges, 1 for in-edges, 2  for out-edges
	 */
	public KNeighborhoodFilter(Set rootNodes, int radiusK, int edgeType) {
		this.rootNodes = rootNodes;
		this.radiusK = radiusK;
		this.edgeType = edgeType;
	}
	/**
	 * Constructs a new instance of the filter
	 * @param rootNode the root node
	 * @param radiusK the neighborhood radius around the root set
	 * @param edgeType 0 for in/out edges, 1 for in-edges, 2  for out-edges
	 */
	public KNeighborhoodFilter(Vertex rootNode, int radiusK, int edgeType) {
		this.rootNodes = new HashSet();
		this.rootNodes.add(rootNode);
		this.radiusK = radiusK;
		this.edgeType = edgeType;
	}
	
	public String getName() {
		return "KNeighborhood(" + radiusK + "," + edgeType + ")";
	}
	
	/**
	 * Constructs an unassembled graph containing the k-neighbhood around the root node(s)
	 */
	public UnassembledGraph filter(Graph graph) {
		// generate a Set of Vertices we want
		// add all to the UG
		int currentDepth = 0;
		ArrayList currentVertices = new ArrayList();
		HashSet visitedVertices = new HashSet();
		HashSet visitedEdges = new HashSet();
		Set acceptedVertices = new HashSet();
		//Copy, mark, and add all the root nodes to the new subgraph
		for (Iterator rootIt = rootNodes.iterator(); rootIt.hasNext();) {
			Vertex currentRoot = (Vertex) rootIt.next();
			visitedVertices.add(currentRoot);
			acceptedVertices.add(currentRoot);
			currentVertices.add(currentRoot);
		}
		ArrayList newVertices = null;
		//Use BFS to locate the neighborhood around the root nodes within distance k
		while (currentDepth < radiusK) {
			newVertices = new ArrayList();
			for (Iterator vertexIt = currentVertices.iterator();
				vertexIt.hasNext();
				) {
				Vertex currentVertex = (Vertex) vertexIt.next();
				Set edges = null;
				switch (edgeType) {
					case IN_OUT :
						edges = currentVertex.getIncidentEdges();
						break;
					case IN :
						edges = currentVertex.getInEdges();
						break;
					case OUT :
						edges = currentVertex.getOutEdges();
						break;
				}
				for (Iterator neighboringEdgeIt = edges.iterator();
					neighboringEdgeIt.hasNext();
					) {
					Edge currentEdge = (Edge) neighboringEdgeIt.next();
					Vertex currentNeighbor =
						currentEdge.getOpposite(currentVertex);
					if (!visitedEdges.contains(currentEdge)) {
						visitedEdges.add(currentEdge);
						if (!visitedVertices.contains(currentNeighbor)) {
							visitedVertices.add(currentNeighbor);
							acceptedVertices.add(currentNeighbor);
							newVertices.add(currentNeighbor);
						}
					}
				}
			}
			currentVertices = newVertices;
			currentDepth++;
		}
		UnassembledGraph ug =
			new UnassembledGraph(
				this,
				acceptedVertices,
				graph.getEdges(),
				graph);
		return ug;
	}
}
