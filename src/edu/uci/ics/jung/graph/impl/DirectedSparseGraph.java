/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.impl;

import java.util.Collection;

import edu.uci.ics.jung.graph.DirectedGraph;

/**
 * An implementation of <code>Graph</code> that consists of a 
 * <code>Vertex</code> set and a <code>DirectedEdge</code> set.
 * This implementation does NOT ALLOW parallel edges. 
 * <code>SimpleDirectedSparseVertex</code> is the most efficient
 * vertex for this graph type.
 * 
 * <p>Edge constraints imposed by this class: DIRECTED_EDGE, NOT_PARALLEL_EDGE
 * 
 * <p>For additional system and user constraints defined for
 * this class, see the superclasses of this class.</p>
 *
 * @author Scott D. White
 * @author Danyel Fisher
 * @author Joshua O'Madadhain
 * 
 * @see DirectedSparseVertex
 * @see DirectedSparseEdge
 */
public class DirectedSparseGraph extends SparseGraph
	implements DirectedGraph {

	/**
	 * Creates an instance of a sparse directed graph.
	 */
	public DirectedSparseGraph() {
		super();
//        system_edge_requirements.add(DIRECTED_EDGE);
//        user_edge_requirements.add(NOT_PARALLEL_EDGE);
        Collection edge_predicates = 
            getEdgeConstraints();
        edge_predicates.add(DIRECTED_EDGE);
        edge_predicates.add(NOT_PARALLEL_EDGE);
	}
}
