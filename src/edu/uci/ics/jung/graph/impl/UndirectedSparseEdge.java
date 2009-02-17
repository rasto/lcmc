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

import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * An implementation of <code>UndirectedEdge</code> that resides
 * in an undirected graph.
 * 
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 * @author Scott White
 * 
 * @see UndirectedSparseVertex
 * @see UndirectedSparseGraph
 */
public class UndirectedSparseEdge extends AbstractSparseEdge
	implements UndirectedEdge 
{
    /**
     * Creates an undirected edge that connects vertex <code>from</code>
     * to vertex <code>to</code> (and vice versa).
     */
	public UndirectedSparseEdge(Vertex from, Vertex to) 
    {
		super( from, to );
	}
}
