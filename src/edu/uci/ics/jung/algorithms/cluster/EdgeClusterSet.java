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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * A ClusterSet where each cluster is a set of edge
 * @author Scott White
 */
public class EdgeClusterSet extends ClusterSet {

    /**
     * Constructs and initializes the set
     * @param underlyingGraph
     */
    public EdgeClusterSet(Graph underlyingGraph) {
        super(underlyingGraph);
    }

    /**
     * Constructs a new graph from the given cluster
     * @param index the position index of the cluster in the collection
     * @return a new graph representing the cluster
     */
    public Graph getClusterAsNewSubGraph(int index) {
        return GraphUtils.edgeSetToGraph(getCluster(index),true);
    }

    /**
     * Creates a new cluster set where each edge and cluster in the new cluster set correspond 1-to-1 with those in
     * the original graph
     * @param anotherGraph a new graph whose edges are equivalent to those in the original graph
     * @return a new cluster set for the specified graph
     */
    public ClusterSet createEquivalentClusterSet(Graph anotherGraph) {
        ClusterSet newClusterSet = new EdgeClusterSet(anotherGraph);
        for (Iterator cIt=iterator();cIt.hasNext();) {
            Set cluster = (Set) cIt.next();
            Set newCluster = new HashSet();
            for (Iterator udcIt=cluster.iterator();udcIt.hasNext();) {
                ArchetypeEdge edge = (ArchetypeEdge) udcIt.next();
                ArchetypeEdge equivalentEdge = edge.getEqualEdge(anotherGraph);
                if (equivalentEdge == null) {
                    throw new IllegalArgumentException("Can not create equivalent cluster set because equivalent edges could not be found in the other graph.");
                }
                newCluster.add(equivalentEdge);
            }
            newClusterSet.addCluster(newCluster);
        }
        return newClusterSet;

    }
}
