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

import edu.uci.ics.jung.graph.filters.Filter;

/**
 * A generally useful template for Filters that have a settable
 * value. Filters like this can be used for slider bars to dynamically
 * vary graphs.
 * @author danyelf
 *
 */
public interface LevelFilter extends Filter {
	
	void setValue( int i );
	int getValue();

}
