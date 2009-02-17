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
 * Created on Dec 28, 2003
 */
package edu.uci.ics.jung.graph.impl;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * 
 * created Dec 28, 2003
 * 
 * @author danyelf
 */
public class BipartiteVertex extends SparseVertex {

	/**
	 * Specialized copy function for copy FROM BipartiteGraph TO BipartiteGraph
	 */
	public ArchetypeVertex copy(ArchetypeGraph newGraph) {
		if (!(newGraph instanceof BipartiteGraph
			&& this.getGraph() instanceof BipartiteGraph)) {
			return super.copy(newGraph);
		}
		BipartiteGraph bpg = (BipartiteGraph) newGraph;

		if (newGraph == this.getGraph())
			throw new IllegalArgumentException(
				"Source and destination graphs " + "must be different");

		try {
			BipartiteVertex v = (BipartiteVertex) clone();
			v.initialize();
			v.importUserData(this);
			BipartiteGraph thisGraph = (BipartiteGraph) this.getGraph();
			bpg.addVertex(v, thisGraph.getPartition(this));
			return v;
		} catch (CloneNotSupportedException cne) {
			throw new FatalException("Can't copy vertex ", cne);
		}
	}

}
