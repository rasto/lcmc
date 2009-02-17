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
 * (c) 2002 Danyel Fisher, Paul Dourish, 
 * and the Regents of the University of California
 *
 * Created on Jul 2, 2003
 *
 */
package edu.uci.ics.jung.graph.filters.impl;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.filters.EfficientFilter;
import edu.uci.ics.jung.graph.filters.GeneralVertexAcceptFilter;

/**
 * A small example filter that accepts vertices that are alphabetically
 * past the input value.
 * 
 * @author danyelf
 */
public class AlphabeticVertexFilter extends GeneralVertexAcceptFilter implements EfficientFilter {

	public AlphabeticVertexFilter(
		String threshhold,
		StringLabeller sl,
		boolean acceptAboveThreshold) {
			
		this.stringLabeller = sl;
		this.threshhold = threshhold;
		this.acceptThoseAboveThreshhold = acceptAboveThreshold;

	}

	private StringLabeller stringLabeller;
	private String threshhold;
	private boolean acceptThoseAboveThreshhold;

	/**
	 * Passes the vertex if its StringLabeller value compares over
	 * (or under) the threshold.
	 */
	public boolean acceptVertex(Vertex vert) {
//		String comp = GraphUtils.getLabel( stringLabeller, vert);
        String comp = stringLabeller.getLabel(vert);
		if ((comp.compareTo(threshhold) > 0) && acceptThoseAboveThreshhold)
			return true;
		return false;
	}

	/**
	 * Returns a name for this:
	 * <tt>AlphabeticFilter(>"TestString")</tt>
	 */
	public String getName() {
		String label = acceptThoseAboveThreshhold ? ">" : "<=";
		return "Alphabetic filter(" + label + "\"" + threshhold + "\")";
	}

	/**
	 * Returns the current direction of the comparison:
	 * True if this accepts only strings above the
	 * threshhold, otherwise, accepts only strings at or
	 * below the threshhold.
	 * 
	 * @return boolean
	 */
	public boolean isAcceptThoseAboveThreshhold() {
		return acceptThoseAboveThreshhold;
	}

	/**
	 * @return String	the current comparison value
	 */
	public String getThreshhold() {
		return threshhold;
	}

	/**
	 * Sets the acceptThoseAboveThreshhold: if true, this accepts values over
	 * the threshhold; if false, this accepts values up to and including the
	 * threshhold, but not above it.
	 * @param acceptThoseAboveThreshhold 
	 */
	public void setAcceptThoseAboveThreshhold(boolean acceptThoseAboveThreshhold) {
		this.acceptThoseAboveThreshhold = acceptThoseAboveThreshhold;
	}

	/**
	 * Sets the threshhold.
	 * @param threshhold The threshhold to set
	 */
	public void setThreshhold(String threshhold) {
		this.threshhold = threshhold;
	}

	/**
	 * Returns the String Labeller being used by this filter to check vertices with.
	 * @return StringLabeller
	 */
	public StringLabeller getStringLabeller() {
		return stringLabeller;
	}

	/**
	 * Sets the stringLabeller.
	 * @param stringLabeller The stringLabeller to set
	 */
	public void setStringLabeller(StringLabeller stringLabeller) {
		this.stringLabeller = stringLabeller;
	}

}
