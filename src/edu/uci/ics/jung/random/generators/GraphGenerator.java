/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.random.generators;

import edu.uci.ics.jung.graph.ArchetypeGraph;

/**
 * An interface for algorithms that generate graphs
 * @author Scott White
 */
public interface GraphGenerator {

    /**
     * Instructs the algorithm to generate the graph
     * @return the generated graph
     */
    public ArchetypeGraph generateGraph();
}
