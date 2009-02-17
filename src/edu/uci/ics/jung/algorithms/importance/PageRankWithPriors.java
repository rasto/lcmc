/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.importance;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.algorithms.connectivity.BFSDistanceLabeler;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * Algorithm that extends the PageRank algorithm by incorporating root nodes (priors). Whereas in PageRank
 * the importance of a node is implicitly computed relative to all nodes in the graph now importance
 * is computed relative to the specified root nodes.
 * <p>
 * Note: This algorithm uses the same key as PageRank for storing rank sccores
 * <p>
 * A simple example of usage is:
 * <pre>
 * PageRankWithPriors ranker = new PageRankWithPriors(someGraph,0.3,1,rootSet,null);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 * <p>
 * Running time: O(|E|*I) where |E| is the number of edges and I is the number of iterations until convergence
 *
 * @author Scott White
 * @see "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
 */
public class PageRankWithPriors extends PageRank {

    /**
     * Constructs an instance of the ranker.
     * @param graph the graph whose nodes are being ranked
     * @param beta the prior weight to put on the root nodes
     * @param priors the set of root nodes
     * @param edgeWeightKeyName the user datum key associated with any user-defined weights. If there are none,
     * null should be passed in.
     */
    public PageRankWithPriors(DirectedGraph graph, double beta, Set priors, String edgeWeightKeyName) {
        super(graph, beta, edgeWeightKeyName,computeReachableVertices(graph,priors));
        setPriors(priors);
        initializePriorWeights();
    }

    protected void initializePriorWeights() {
        Set allVertices = getVertices();

        Set priors = getPriors();
        double numPriors = priors.size();

        Set nonPriors = new HashSet();
        nonPriors.addAll(allVertices);
        nonPriors.removeAll(priors);

        for (Iterator vIt = nonPriors.iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            setPriorRankScore(currentVertex, 0.0);
        }

        for (Iterator vIt = getPriors().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            setPriorRankScore(currentVertex, 1.0 / numPriors);
        }
    }

    private static Pair computeReachableVertices(Graph g, Set priors) {

        BFSDistanceLabeler labeler = new BFSDistanceLabeler("DISTANCE");
        labeler.labelDistances(g, priors);
        labeler.removeDecorations(g);
        Pair p = new Pair(new HashSet(labeler.getVerticesInOrderVisited()),
                          new HashSet(labeler.getUnivistedVertices()));

        return p;
    }

    protected void reinitialize() {
        super.reinitialize();
        initializePriorWeights();
    }
}
