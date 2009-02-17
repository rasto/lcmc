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

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * An implementation of <code>DirectedEdge</code> that resides in a 
 * directed graph.
 * 
 * @author Scott D. White
 * @author Joshua O'Madadhain
 * @author Danyel Fisher
 *
 * @see DirectedSparseVertex
 * @see DirectedSparseGraph
 */
public class DirectedSparseEdge extends AbstractSparseEdge implements DirectedEdge {

    /**
     * Creates a directed edge whose source is <code>from</code> and whose
     * destination is <code>to</code>.
     */
	public DirectedSparseEdge(Vertex from, Vertex to) {
		super(from, to);
	}

    /**
     * @see DirectedEdge#getSource()
     */
	public Vertex getSource() {
        return (Vertex)getEndpoints().getFirst();
	}

    /**
     * @see DirectedEdge#getDest()
     */
	public Vertex getDest() {
        return (Vertex)getEndpoints().getSecond();
	}
}
