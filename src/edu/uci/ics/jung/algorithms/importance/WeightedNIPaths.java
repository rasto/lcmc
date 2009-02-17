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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.UserData;


/**
 * This algorithm measures the importance of nodes based upon both the number and length of disjoint paths that lead
 * to a given node from each of the nodes in the root set. Specifically the formula for measuring the importance of a
 * node is given by: I(t|R) = sum_i=1_|P(r,t)|_{alpha^|p_i|} where alpha is the path decay coefficient, p_i is path i
 * and P(r,t) is a set of maximum-sized node-disjoint paths from r to t.
 * <p>
 * This algorithm uses heuristic breadth-first search to try and find the maximum-sized set of node-disjoint paths
 * between two nodes. As such, it is not guaranteed to give exact answers.
 * <p>
 * A simple example of usage is:
 * <pre>
 * WeightedNIPaths ranker = new WeightedNIPaths(someGraph,2.0,6,rootSet);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 * 
 * @author Scott White
 * @see "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
 */
public class WeightedNIPaths extends AbstractRanker {
    public final static String WEIGHTED_NIPATHS_KEY = "jung.algorithms.importance.WEIGHTED_NIPATHS_KEY";
    private double mAlpha;
    private int mMaxDepth;
    private Set mPriors;

    /**
     * Constructs and initializes the algorithm.
     * @param graph the graph whose nodes are being measured for their importance
     * @param alpha the path decay coefficient (>= 1); 2 is recommended
     * @param maxDepth the maximal depth to search out from the root set
     * @param priors the root set (starting vertices)
     */
    public WeightedNIPaths(DirectedGraph graph, double alpha, int maxDepth, Set priors) {
        super.initialize(graph, true,false);
        mAlpha = alpha;
        mMaxDepth = maxDepth;
        mPriors = priors;
        for (Iterator vIt = graph.getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            currentVertex.setUserDatum(WEIGHTED_NIPATHS_KEY, new MutableDouble(0), UserData.SHARED);
        }
    }

    /**
     * Given a node, returns the corresponding rank score. This implementation of <code>getRankScore</code> assumes
     * the decoration representing the rank score is of type <code>MutableDouble</code>.
     * @return  the rank score for this node
     */
    public String getRankScoreKey() {
        return WEIGHTED_NIPATHS_KEY;
    }

    protected void incrementRankScore(Element v, double rankValue) {
        setRankScore(v, getRankScore(v) + rankValue);
    }

    protected void computeWeightedPathsFromSource(Vertex root, int depth) {

        int pathIdx = 1;
        for (Iterator rootEdgeIt = root.getOutEdges().iterator(); rootEdgeIt.hasNext();) {
            DirectedEdge currentEdge = (DirectedEdge) rootEdgeIt.next();
            Integer pathIdxValue = new Integer(pathIdx);
            currentEdge.setUserDatum(PATH_INDEX_KEY, pathIdxValue, UserData.REMOVE);
            currentEdge.setUserDatum(ROOT_KEY, root, UserData.REMOVE);
            newVertexEncountered(pathIdxValue, currentEdge.getDest(), root);
            pathIdx++;
        }

        List edges = new ArrayList();

        Vertex virtualNode = getGraph().addVertex(new SparseVertex());
        Edge virtualSinkEdge = GraphUtils.addEdge(getGraph(), virtualNode, root);
        edges.add(virtualSinkEdge);

        int currentDepth = 0;
        while (currentDepth <= depth) {

            double currentWeight = Math.pow(mAlpha, -1.0 * currentDepth);

            for (Iterator it = edges.iterator(); it.hasNext();) {
                DirectedEdge currentEdge = (DirectedEdge) it.next();
                incrementRankScore(currentEdge.getDest(), currentWeight);
            }

            if ((currentDepth == depth) || (edges.size() == 0)) break;

            List newEdges = new ArrayList();

            for (Iterator sourceEdgeIt = edges.iterator(); sourceEdgeIt.hasNext();) {
                DirectedEdge currentSourceEdge = (DirectedEdge) sourceEdgeIt.next();
                Integer sourcePathIndex = (Integer) currentSourceEdge.getUserDatum(PATH_INDEX_KEY);

                for (Iterator edgeIt = currentSourceEdge.getDest().getOutEdges().iterator(); edgeIt.hasNext();) {
                    DirectedEdge currentDestEdge = (DirectedEdge) edgeIt.next();
                    Vertex destEdgeRoot = (Vertex) currentDestEdge.getUserDatum(ROOT_KEY);
                    Vertex destEdgeDest = currentDestEdge.getDest();

                    if (currentSourceEdge == virtualSinkEdge) {
                        newEdges.add(currentDestEdge);
                        continue;
                    }
                    if (destEdgeRoot == root) {
                        continue;
                    }
                    if (destEdgeDest == currentSourceEdge.getSource()) {
                        continue;
                    }

                    Set pathsSeen = (Set) destEdgeDest.getUserDatum(PATHS_SEEN_KEY);

                    /*
                    Set pathsSeen = new HashSet();
        pathsSeen.add(sourcePathIndex);
        dest.setUserDatum(PATHS_SEEN_KEY, pathsSeen, UserData.REMOVE);
        dest.setUserDatum(ROOT_KEY, root, UserData.REMOVE);
        */

                    if (pathsSeen == null) {
                        newVertexEncountered(sourcePathIndex, destEdgeDest, root);
                    } else if (destEdgeDest.getUserDatum(ROOT_KEY) != root) {
                        destEdgeDest.setUserDatum(ROOT_KEY, root, UserData.REMOVE);
                        pathsSeen.clear();
                        pathsSeen.add(sourcePathIndex);
                    } else if (!pathsSeen.contains(sourcePathIndex)) {
                        pathsSeen.add(sourcePathIndex);
                    } else {
                        continue;
                    }

                    currentDestEdge.setUserDatum(PATH_INDEX_KEY, sourcePathIndex, UserData.REMOVE);
                    currentDestEdge.setUserDatum(ROOT_KEY, root, UserData.REMOVE);
                    newEdges.add(currentDestEdge);
                }
            }

            edges = newEdges;
            currentDepth++;
        }

        getGraph().removeVertex(virtualNode);
    }

    private void newVertexEncountered(Integer sourcePathIndex, Vertex dest, Vertex root) {
        Set pathsSeen = new HashSet();
        pathsSeen.add(sourcePathIndex);
        dest.setUserDatum(PATHS_SEEN_KEY, pathsSeen, UserData.REMOVE);
        dest.setUserDatum(ROOT_KEY, root, UserData.REMOVE);
    }

    protected double evaluateIteration() {
        for (Iterator it = mPriors.iterator(); it.hasNext();) {
            computeWeightedPathsFromSource((Vertex) it.next(), mMaxDepth);
        }

        normalizeRankings();
        return 0;
    }

    protected void onFinalize(Element udc) {
        udc.removeUserDatum(PATH_INDEX_KEY);
        udc.removeUserDatum(ROOT_KEY);
        udc.removeUserDatum(PATHS_SEEN_KEY);
    }

    private static final String PATH_INDEX_KEY = "WeightedNIPathsII.PathIndexKey";
    private static final String ROOT_KEY = "WeightedNIPathsII.RootKey";
    private static final String PATHS_SEEN_KEY = "WeightedNIPathsII.PathsSeenKey";
}
