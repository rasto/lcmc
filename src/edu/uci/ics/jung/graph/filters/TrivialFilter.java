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

import edu.uci.ics.jung.graph.Vertex;

/**
 * A small filter that returns the vertices and edges in the orignal Graph.
 * @author danyelf
 */
public class TrivialFilter extends GeneralVertexAcceptFilter implements EfficientFilter {

	/**
	 * Returns true for all vertices.
	 */
	public boolean acceptVertex(Vertex vert) {
		return true;
	}

	public String getName() {
		return "(Trivial)";
	}

	static private TrivialFilter instance = null;

	/**
	 * This is a factory class; just ask for an instance of
	 * this one.
	 */
	public static TrivialFilter getInstance() {
		if (instance == null) {
			instance = new TrivialFilter();
		}
		return instance;		
	}
	
	protected TrivialFilter() {		
	}

}
