/*
 * Created on Sep 24, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import edu.uci.ics.jung.graph.Edge;

/**
 * An interface for a service to access the index of a given edge
 * into the set formed by the given edge and all the other edges it is parallel to.
 * 
 * <p>Note that in current use, this index is assumed to be an integer value in
 * the interval [0,n-1], where n-1 is the number of edges parallel to <code>e</code>.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 */
public interface ParallelEdgeIndexFunction {
    
    int getIndex(Edge e);

}
