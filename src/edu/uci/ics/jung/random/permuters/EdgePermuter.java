/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.random.permuters;

import edu.uci.ics.jung.graph.Graph;

/**
 * An interface for algorithms that randomly permute edges according to some distribution or other mechanism
 * @author Scott White
 */
public interface EdgePermuter {

    /**
     * Instructs the algoritm to go ahead and permute the edges for the given graph
     * @param graph the graph whose edges are to be permuted
     */
    void permuteEdges(Graph graph);
}
