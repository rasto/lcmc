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

import java.util.*;

import edu.uci.ics.jung.graph.Graph;

/**
 * This is a generic filter that takes at least two other filters
 * and runs them seially. That is, it filters first on F1,
 * and then on F2. Note that it stores links to the filters;
 * mutable filters therefore are at risk of causing odd side effects:
 * <code>
 * <pre>
 * 		Filter f1 = new HeightFilter( "tall" );
 * 		Filter f2 = new ColorFilter("Green");
 * 		Filter serial = new SerialFilter( f1, f2 );
 * 		Graph tallGreen = serial.filter( graph ).assemble();
 * 		// this contains all tall, green things
 * 
 * 		f1.setHeight("short")
 *		// careful, f1 is stored in serial!
 * 		Graph otherGreen = serial.filter( graph ).assemble();
 * 		// this now contains all short green things.
 * </pre>
 * </code>
 * 
 * @author danyelf
 */
public class SerialFilter implements Filter, EfficientFilter {

	private List filters;

	/**
	 * Returns the name of the serial filter.
	 */
	public String getName() {
		String rv = "Serial[";
		boolean first = true;
		for (Iterator iter = filters.iterator(); iter.hasNext();) {			
			Filter f = (Filter) iter.next();
			rv += (first ? "" : ",") + f.getName();
			first = false;
		}
		rv += "]";
		return rv;
	}

	/**
	 * Small constructor for two filters. Simply adds them in sequence.
	 * The first filter will be run on the graph before the second one.
	 * @param f1	The first filter.
	 * @param f2	The second filter.
	 */
	public SerialFilter(Filter f1, Filter f2) {
		filters = new ArrayList();
		filters.add(f1);
		filters.add(f2);
	}


	/**
	 * Constructor for an arbitrary list of filters. When <tt>filter()</tt> is called,
	 * will run through them from first to last.
	 */
	public SerialFilter(List filters) {
		if (filters == null) {
			throw new IllegalArgumentException("List can not be null.");
		}
		this.filters = new LinkedList(filters);
	}
	
	/**
	 * Creates an empty list of filters. By default, if <tt>filter</tt>
	 * is called, will return the original graph.
	 * 
	 * @see TrivialFilter
	 */
	public SerialFilter() {
		this.filters = new LinkedList();
	}

	/**
	 * Adds a filter to the end of the sequence of filters. Thus, appended
	 * filters will be processed last in the sequence.
	 * @param f  Adds the filter to the end of the list.
	 */
	public void append(Filter f) {
		filters.add(f);
	}

	/**
	 * Runs through the sequence of filters, one at a time. Starts with the
	 * first filter in sequence and works forward. When the filter is able to
	 * be efficient, it does so.
	 */
	public UnassembledGraph filter(Graph g) {
		UnassembledGraph ug = TrivialFilter.getInstance().filter(g);
		return filter(ug);
	}

	/**
	 * Runs through the sequence of filters, one at a time. Starts with the
	 * first filter in sequence and works forward. When the filter is able to
	 * be efficient, it does so.
	 */
	public UnassembledGraph filter(UnassembledGraph g) {
		UnassembledGraph ug = g;
		for (Iterator iter = filters.iterator(); iter.hasNext();) {
			Filter f = (Filter) iter.next();

			// we have a current UG. Can we process it efficiently?
			if (f instanceof EfficientFilter) {
				// yes: process it directly
				EfficientFilter nfe = (EfficientFilter) f;
				ug = nfe.filter(ug);
			} else {
				// no: assemble it, then process
				Graph prev = ug.assemble();
				ug = f.filter(prev);
			}
		}
		return ug;
	}
}
