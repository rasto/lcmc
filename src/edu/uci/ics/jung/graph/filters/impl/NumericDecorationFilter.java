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
import edu.uci.ics.jung.graph.filters.GeneralVertexAcceptFilter;

/**
 * This simple filter accepts vertices if their UserData at the given key is
 * over a threshold value. Note that this depends on user data that
 * is attached to the vertex.
 * 
 * @author Scott White
 */
public class NumericDecorationFilter extends GeneralVertexAcceptFilter {
	private double mThreshold;
	private String mDecorationKey;

	public boolean acceptVertex(Vertex vertex) {
		Number n = (Number) vertex.getUserDatum(mDecorationKey);

		if (n.doubleValue() > mThreshold) {
			return true;
		}
		return false;
	}

	public String getName() {
		return "NumericDecoration";
	}

	public String getDecorationKey() {
		return mDecorationKey;
	}

	public void setDecorationKey(String decorationKey) {
		this.mDecorationKey = decorationKey;
	}

	public double getThreshold() {
		return mThreshold;
	}

	public void setThreshold(double threshold) {
		this.mThreshold = threshold;
	}
}
