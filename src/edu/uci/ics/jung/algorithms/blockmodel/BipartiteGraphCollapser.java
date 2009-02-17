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
 * Created on Feb 8, 2004
 */
package edu.uci.ics.jung.algorithms.blockmodel;

import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.BipartiteEdge;
import edu.uci.ics.jung.graph.impl.BipartiteGraph;
import edu.uci.ics.jung.graph.impl.BipartiteVertex;

/**
 * A variant of the GraphCollapser that overrides two
 * minor functions and defines CollapsedBipartiteEdge and
 * CollapsedBipartiteVertex. This models the basic procedure
 * for tweaking the GraphCollapser to your own (nefarious)
 * purposes.
 * 
 * created Feb 8, 2004
 * @author danyelf
 */
public class BipartiteGraphCollapser extends GraphCollapser {

	public class CollapsedBipartiteEdge
		extends BipartiteEdge
		implements CollapsedEdge {

		private Set relevantEdges;

		public CollapsedBipartiteEdge(
			BipartiteVertex a,
			BipartiteVertex b,
			Set edges) {
			super(a, b);
			this.relevantEdges = edges;
		}

		public Set getRelevantEdges() {
			return relevantEdges;
		}

	}

	public class CollapsedBipartiteVertex
		extends BipartiteVertex
		implements CollapsedVertex {

		private Set rootSet;

		public CollapsedBipartiteVertex(Set rootSet) {
			this.rootSet = rootSet;
		}

		public Set getRootSet() {
			return rootSet;
		}

	}
	
	/**
	 * @see edu.uci.ics.jung.graph.algorithms.blockmodel.GraphCollapser#addUndirectedEdge(edu.uci.ics.jung.graph.Graph, edu.uci.ics.jung.graph.Vertex, edu.uci.ics.jung.graph.Vertex, java.util.Set)
	 */
	protected void createUndirectedEdge(
		Graph g,
		CollapsedVertex superVertex,
		Vertex opposite,
		Set relevantEdges) {

		BipartiteGraph bpg = (BipartiteGraph) g;
		BipartiteVertex op = (BipartiteVertex) opposite,
			su = (BipartiteVertex) superVertex;

		CollapsedBipartiteEdge bpe;
		// order is important.
		if (bpg.getPartition(su) == BipartiteGraph.CLASSA) {
			bpe =
				(CollapsedBipartiteEdge) bpg.addBipartiteEdge(
					new CollapsedBipartiteEdge(su, op, relevantEdges));
		} else {
			bpe =
				(CollapsedBipartiteEdge) bpg.addBipartiteEdge(
					new CollapsedBipartiteEdge(op, su, relevantEdges));
		}
		annotateEdge(bpe, relevantEdges);
	}

	/**
	 *  
	 * It must be the case that all members of rootSet are in the same partition.
	 * @see edu.uci.ics.jung.graph.algorithms.blockmodel.GraphCollapser#getCollapsedVertex(edu.uci.ics.jung.graph.Graph, java.util.Set)
	 */
	protected CollapsedVertex createCollapsedVertex(Graph g, Set rootSet) {
		BipartiteGraph.Choice choice = null;
		BipartiteGraph bpg = (BipartiteGraph) g;
		for (Iterator iter = rootSet.iterator(); iter.hasNext();) {
			BipartiteVertex v = (BipartiteVertex) iter.next();
			if (choice == null) {
				choice = bpg.getPartition(v);
			} else {
				if (choice != bpg.getPartition(v)) {
					throw new FatalException("All vertices must be in the same partition");
				}
			}
		}

		CollapsedBipartiteVertex cbv = new CollapsedBipartiteVertex(rootSet);
		bpg.addVertex(cbv, choice);
		return cbv;
	}

}
