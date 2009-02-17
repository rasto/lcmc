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
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;

/**
 * Abstract class that implements a generic filter for accepting arbitrary 
 * edges (and all vertices). To use it, subclass this and override 
 * <tt>acceptEdge</tt>. This is compatible with both <tt>EfficientFilter</tt>;
 * in order to use it as such, make sure to label your class as an 
 * <tt>EfficientFilter</tt> with
 * <tt>implements EfficientFilter</tt>.
 * <p>
 * <h2>Sample code</h2>
 * <pre>
 * 	// Returns a version of the graph that only has blue edges.
 * 	class OnlyBlueEdgeFilter extends GeneralEdgeAcceptFilter 
 * 		implements EfficientFilter {
 * 
 * 		// BlueChecker is a helper class that I've implemented somewhere else
 * 		boolean acceptEdge( Edge e ) {
 * 			return BlueChecker.checkBlue( e );
 * 		}
 * }
 * </pre>
 * 
 * @author danyelf
 */
public abstract class GeneralEdgeAcceptFilter implements Filter {

	/**
	 * Determines whether the current edge should be accepted
	 * into the Graph. User should override this method.
	 * @param edge	the input edge that is being evaluated.
	 * @return	whether the edge should be accepted or not
	 */
	public abstract boolean acceptEdge(Edge edge);

	/**
	 * Returns an <tt>UnassembledGraph</tt> with the subset
	 * of edges that pass <tt>acceptEdge</tt>. 
	 * @param g	A <tt>Graph</tt> to be filtered.
	 * @return	An UnassembledGraph containing the subset of <tt>g</tt>
	 * that pass the filter.
	 * 
	 * @see Filter#filter(Graph)
	 */
	public UnassembledGraph filter(Graph g) {
		Set vertices = g.getVertices();
		Set edges = g.getEdges();

		Set newEdges = chooseGoodEdges( edges );
		return new UnassembledGraph(this, vertices, newEdges, g);
	}

	/**
	 * Returns an <tt>UnassembledGraph</tt> with the subset
	 * of edges that pass <tt>acceptEdge</tt>. This method
	 * is used only if this class implements <tt>EfficientFilter</tt>,
	 * and, in fact, it contains a runtime check to ensure that the
	 * subclass has been labelled correctly.
	 * @param ug	An <tt>UnassembledGraph</tt> containing a subset of
	 * vertices and edges from an original graph.
	 * @return	An UnassembledGraph containing the subset of <tt>ug</tt>
	 * that pass the filter.
	 * 
	 * @see EfficientFilter#filter(UnassembledGraph)
	 */
	public UnassembledGraph filter(UnassembledGraph ug) {

		if (! (this instanceof EfficientFilter)) 
			throw new FatalException("Do not call non-efficient filters with UnassembledGraphs.");

		Set vertices = null;
		Set edges = null;

		vertices = ug.getUntouchedVertices();
		edges = ug.getUntouchedEdges();
		if (vertices == null) {
			vertices = ug.getOriginalGraph().getVertices();
		}
		if (edges == null) {
			edges = ug.getOriginalGraph().getEdges();
		}
		Set newEdges = chooseGoodEdges( edges );
		return new UnassembledGraph(this, vertices, newEdges, ug);
	}

	private Set chooseGoodEdges( Set edges ) {
		Set newEdges = new HashSet();
		for (Iterator iter = edges.iterator(); iter.hasNext();) {
			Edge e = (Edge) iter.next();
			if (acceptEdge(e)) {
				newEdges.add(e);
			}
		}
		return newEdges;
	}

}
