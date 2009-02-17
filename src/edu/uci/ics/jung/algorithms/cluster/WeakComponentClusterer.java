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

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;


/**
 * Finds all weak components in a graph where a weak component is defined as
 * a maximal subgraph in which all pairs of vertices in the subgraph are reachable from one
 * another in the underlying undirected subgraph.
 * <p>
 * Running time: O(|V| + |E|) where |V| is the number of vertices and |E| is the number of edges.
 * @author Scott White
 */
public class WeakComponentClusterer implements GraphClusterer {

    /**
     * Extracts the weak components from a graph.
     * @param aGraph the graph whose weak components are to be extracted
     * @return the list of weak components
     */
    public ClusterSet extract(ArchetypeGraph aGraph) {

        ClusterSet clusterSet = new VertexClusterSet(aGraph);

        HashSet unvisitedVertices = new HashSet();
        for (Iterator vIt=aGraph.getVertices().iterator(); vIt.hasNext();) {
            unvisitedVertices.add(vIt.next());
        }

        while (!unvisitedVertices.isEmpty()) {
            Set weakComponentSet = new HashSet();
            ArchetypeVertex root = (ArchetypeVertex) unvisitedVertices.iterator().next();
            unvisitedVertices.remove(root);
            weakComponentSet.add(root);

            Buffer queue = new UnboundedFifoBuffer();
            queue.add(root);

            while (!queue.isEmpty()) {
                ArchetypeVertex currentVertex = (ArchetypeVertex) queue.remove();
                Set neighbors = currentVertex.getNeighbors();

                for (Iterator nIt = neighbors.iterator(); nIt.hasNext();) {
                    ArchetypeVertex neighbor = (ArchetypeVertex) nIt.next();
                    if (unvisitedVertices.contains(neighbor)) {
                        queue.add(neighbor);
                        unvisitedVertices.remove(neighbor);
                        weakComponentSet.add(neighbor);
                    }
                }
            }
            clusterSet.addCluster(weakComponentSet);
        }

        return clusterSet;
    }

}
