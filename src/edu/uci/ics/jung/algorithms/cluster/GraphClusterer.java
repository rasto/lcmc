/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.cluster;

import edu.uci.ics.jung.graph.ArchetypeGraph;

/**
 * Interface for finding clusters (sets of possibly overlapping vertices) in graphs.
 * @author Scott White
 */
public interface GraphClusterer {

    /**
     * Extracts the clusters from a graph.
     * @param graph the graph
     * @return the set of clusters
     */
    public ClusterSet extract(ArchetypeGraph graph);
}
