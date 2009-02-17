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

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * An interface for classes that return information regarding whether a 
 * given vertex or edge has been selected.
 * 
 * @author danyelf
 */
public interface PickedInfo {

	public boolean isPicked( ArchetypeVertex v );
	public boolean isPicked( ArchetypeEdge e);

}
