/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Abstract class that implements a generic filter for accepting arbitrary 
 * vertices (and all edges). To use it, subclass this and override 
 * <tt>acceptEdge</tt>. This is compatible with both <tt>EfficientFilter</tt>;
 * in order to use it as such, make sure to label your class as an 
 * <tt>EfficientFilter</tt> with
 * <tt>implements EfficientFilter</tt>.
 * <p>
 * See sample code at <tt>{@link GeneralEdgeAcceptFilter GeneralEdgeAcceptFilter}</tt>
 * @author danyelf
 */
public abstract class GeneralVertexAcceptFilter implements Filter {

	public abstract boolean acceptVertex(Vertex vert);

	/**
	 * This method does the actual filtering of the the graph.
	 * It walks through the set of accepted vertices, and 
	 * examines each in turn to see whether it is accepted by
	 * acceptVertex. If so, it adds it to the set; if not, it
	 * discards it. This set of filtered vertices is then sent
	 * to the FilteredGraph class.
	 */
	public UnassembledGraph filter (Graph g) {
		Set vertices = chooseGoodVertices( g.getVertices() );
		Set edges = g.getEdges();

		return new UnassembledGraph( this, vertices, edges, g );
	}

	/**
	 * Returns an <tt>UnassembledGraph</tt> with the subset
	 * of vertices that pass <tt>acceptEdge</tt>. This method
	 * is used only if this class implements <tt>EfficientFilter</tt>,
	 * and, in fact, it contains a runtime check to ensure that the
	 * subclass has been labelled correctly.
	 * @param g	An <tt>UnassembledGraph</tt> containing a subset of
	 * vertices and edges from an original graph.
	 * @return	An UnassembledGraph containing the subset of <tt>ug</tt>
	 * that pass the filter.
	 * 
	 * @see EfficientFilter#filter(UnassembledGraph)
	 */
	 public UnassembledGraph filter (UnassembledGraph g) {
		if (! (this instanceof EfficientFilter)) 
			throw new FatalException("Do not call non-efficient filters with UnassembledGraphs.");
		Set vertices = chooseGoodVertices( g.getUntouchedVertices() );
		Set edges = g.getUntouchedEdges();
				
		return new UnassembledGraph( this, vertices, edges, g );
	}
	
	
	private Set chooseGoodVertices( Set vertices ) {
		Set newVertices = new HashSet();
		for (Iterator iter = vertices.iterator(); iter.hasNext();) {
			Vertex e = (Vertex) iter.next();
			if (acceptVertex(e)) {
				newVertices.add(e);
			}
		}
		return newVertices;
	}

	
}
