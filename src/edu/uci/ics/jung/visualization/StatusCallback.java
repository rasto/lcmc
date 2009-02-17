/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.visualization;

/**
 * This thin interface is used to allow the <tt>GraphDraw</tt> and
 * <tt>_VisualizationViewer</tt> systems to get status reports from
 * <tt>Layout</tt>s.
 * 
 * @author danyelf
 */
public interface StatusCallback {
	
	public void callBack( String status );

}
