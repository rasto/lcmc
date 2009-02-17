/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.filters.impl;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.filters.Filter;
import edu.uci.ics.jung.graph.filters.GeneralVertexAcceptFilter;

/**
 * Accepts only nodes that have at least one edge--that is, nodes that
 * are connected to one other node. This removes isolates, usually in order
 * to clean up visualizations. This is NOT an EfficientFilter. 
 * <p>Because there
 * are no settable parameters, this is set as a factory method--to
 * get a <tt>DropSoloNodesFilter<tt>, just invoke
 * <pre>
 * 	Filter f = DropSoloNodesFilter.getInstance();
 * </pre>
 * @author danyelf
 */
public class DropSoloNodesFilter extends GeneralVertexAcceptFilter {

	public boolean acceptVertex(Vertex vert) {
		boolean b = (vert.getIncidentEdges().size() > 0);
//		if (vert.getIncidentEdges().size() == 1) {
//			if (vert.getNeighbors().contains( vert )) {
//				System.out.println("loop!");
//			}
//		}
//		if ( !b ) {
//			System.out.println( "Drop " + vert.toString() );
//		}
		return b;
	}
	
	public String getName() {
		return "DropSoloNodes";
	}

	
	static DropSoloNodesFilter dsn = null; 

	public static Filter getInstance() {
		if ( dsn == null )
			dsn = new DropSoloNodesFilter();
		return dsn;			
	}
	
	protected DropSoloNodesFilter() {		
	}

}
