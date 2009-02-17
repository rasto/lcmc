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
 * Created on Apr 23, 2003
 */
package edu.uci.ics.jung.graph.filters.impl;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.decorators.EdgeWeightLabeller;
import edu.uci.ics.jung.graph.filters.EfficientFilter;
import edu.uci.ics.jung.graph.filters.GeneralEdgeAcceptFilter;
import edu.uci.ics.jung.graph.filters.LevelFilter;

/**
 * This simple filter accepts Edges if their EdgeWeightLabeller turns out
 * to be greater than the input value.
 * @author danyelf
 */
public class WeightedEdgeGraphFilter
	extends GeneralEdgeAcceptFilter
	implements LevelFilter, EfficientFilter {

	public WeightedEdgeGraphFilter(int threshold, EdgeWeightLabeller el) {
		setValue(threshold);
		this.labels = el;
	}

	/** (non-Javadoc)
	 * @see edu.uci.ics.jung.graph.filters.Filter#getName()
	 */
	public String getName() {
		return "WeightedGraph(" + threshold + ")";
	}

	private int threshold;
	private EdgeWeightLabeller labels;

	public void setValue(int threshold) {
		this.threshold = threshold;
	}

	public int getValue() {
		return threshold;
	}

	public boolean acceptEdge(Edge e) {
//		Edge edge = GraphUtils.getCorrespondingEdge( labels.getGraph(), e );
        Edge edge = (Edge)e.getEqualEdge(labels.getGraph());
		int val = labels.getWeight( edge );
		//				((Integer) e.getUserDatum(WEIGHT_KEY)).intValue();
		if (val < threshold) {
			//			System.out.println("Rejected something!");
		}
		return (val >= threshold);
	}

}
