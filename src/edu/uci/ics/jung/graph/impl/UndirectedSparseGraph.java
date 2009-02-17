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

import edu.uci.ics.jung.graph.UndirectedGraph;

/**
 * An implementation of <code>Graph</code> that consists of a 
 * <code>Vertex</code> set and an <code>UndirectedEdge</code> set.
 * <code>SimpleUndirectedSparseVertex</code> is an appropriate vertex type
 * for this graph.
 * 
 * <p>Edge constraints imposed by this class: UNDIRECTED_EDGE,
 * NOT_PARALLEL_EDGE</p>
 * 
 * @author Joshua O'Madadhain
 * @author Scott White
 * @author Danyel Fisher
 *
 * @see SimpleUndirectedSparseVertex
 * @see UndirectedSparseEdge
 */
public class UndirectedSparseGraph extends SparseGraph
	implements UndirectedGraph 
{

    /**
     * Creates an instance of a sparse undirected graph.
     */
	public UndirectedSparseGraph() 
    {
		super();
//        system_edge_requirements.add(UNDIRECTED_EDGE);
//        user_edge_requirements.add(NOT_PARALLEL_EDGE);
        Collection edge_predicates = 
            getEdgeConstraints();
        edge_predicates.add(UNDIRECTED_EDGE);
        edge_predicates.add(NOT_PARALLEL_EDGE);
	}
}
