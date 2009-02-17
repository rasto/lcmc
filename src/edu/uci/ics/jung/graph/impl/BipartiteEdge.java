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
 * Created on Aug 15, 2003
 *
 */
package edu.uci.ics.jung.graph.impl;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.utils.Pair;

/**
 * A simple extension of the UndirectedSparseEdge, except
 * with careful bounds checking. The constructor throws
 * a FatalException if its vertices are not in two classes
 * of a BipartiteGraph. (In fact, the Vertices must come in
 * the order CLASSA, CLASSB).
 * 
 * @author danyelf
 */
public class BipartiteEdge extends UndirectedSparseEdge {

	/**
	 * The BipartiteEdge constructor.
	 * @param a	a Vertex from a BipartiteGraph in CLASSA
	 * @param b a Vertex from the same BipartiteGraph in CLASSB
	 */
	public BipartiteEdge(BipartiteVertex a, BipartiteVertex b) {
		super(a, b);
		BipartiteGraph g = (BipartiteGraph) a.getGraph();

		boolean aInA = g.getAllVertices(BipartiteGraph.CLASSA).contains(a);
		boolean bInB = g.getAllVertices(BipartiteGraph.CLASSB).contains(b);

		if (!(aInA && bInB))
			throw new FatalException("Tried to create edge that isn't bipartite!");
	}

	public ArchetypeEdge copy(ArchetypeGraph newGraph) {
		if (newGraph == this.getGraph())
			throw new IllegalArgumentException(
				"Source and destination " + "graphs must be different");

		Pair ends = getEndpoints();

		BipartiteVertex eFrom =
			(BipartiteVertex) ends.getFirst();
		BipartiteVertex eTo =
			(BipartiteVertex) ends.getSecond();

		BipartiteVertex from = (BipartiteVertex) eFrom.getEqualVertex(newGraph);
		BipartiteVertex to = (BipartiteVertex) eTo.getEqualVertex(newGraph);

		if (from == null || to == null)
			throw new IllegalArgumentException(
				"Cannot create edge: source edge's incident "
					+ "vertices have no equivalents in target graph");

		if (from.getGraph() != newGraph) {
			throw new FatalException("Unexpected error: 'from' vertex is not in target graph");
		}
		if (to.getGraph() != newGraph) {
			throw new FatalException("Unexpected error: 'to' vertex is not in target graph");
		}
		if (eFrom.getGraph() == from.getGraph()) {
			throw new FatalException("Unexpected error: 'from' and 'to' vertices are not in same graph");
		}

		BipartiteEdge e;
		try {
			e = (BipartiteEdge) this.clone();
		} catch (CloneNotSupportedException cnse) {
			throw new FatalException("Can't copy edge", cnse);
		}
		
		e.m_Graph = null;
		e.mFrom = from;
		e.mTo = to;
		((BipartiteGraph) newGraph).addBipartiteEdge(e);
		e.importUserData(this);
		return e;

	}

}
