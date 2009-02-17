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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.UserData;

/**
 * Algorithm variant of <code>PageRankWithPriors</code> that computes the importance of a node based upon taking fixed-length random
 * walks out from the root set and then computing the stationary probability of being at each node. Specifically, it computes
 * the relative probability that the markov chain will spend at any particular node, given that it start in the root
 * set and ends after k steps.
 * <p>
 * A simple example of usage is:
 * <pre>
 * KStepMarkov ranker = new KStepMarkov(someGraph,rootSet,6,null);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 * <p>
 *
 * @author Scott White
 * @see "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
 */
public class KStepMarkov extends RelativeAuthorityRanker {
    public final static String RANK_SCORE = "jung.algorithms.importance.KStepMarkovExperimental.RankScore";
    private final static String CURRENT_RANK = "jung.algorithms.importance.KStepMarkovExperimental.CurrentRank";
    private int mNumSteps;
    HashMap mPreviousRankingsMap;

    /**
     * Construct the algorihm instance and initializes the algorithm.
     * @param graph the graph to be analyzed
     * @param priors the set of root nodes
     * @param k positive integer parameter which controls the relative tradeoff between a distribution "biased" towards
     * R and the steady-state distribution which is independent of where the Markov-process started. Generally values
     * between 4-8 are reasonable
     * @param edgeWeightKeyName
     */
    public KStepMarkov(DirectedGraph graph, Set priors, int k, String edgeWeightKeyName) {
        super.initialize(graph,true,false);
        mNumSteps = k;
        setPriors(priors);
        initializeRankings();
        if (edgeWeightKeyName == null) {
            assignDefaultEdgeTransitionWeights();
        } else {
            setUserDefinedEdgeWeightKey(edgeWeightKeyName);
        }
        normalizeEdgeTransitionWeights();
    }

    /**
     * The user datum key used to store the rank scores.
     * @return the key
     */
    public String getRankScoreKey() {
        return RANK_SCORE;
    }

    protected void incrementRankScore(Element v, double rankValue) {
        MutableDouble value = (MutableDouble) v.getUserDatum(RANK_SCORE);

        value.add(rankValue);
    }

    protected double getCurrentRankScore(Element v) {
        return ((MutableDouble) v.getUserDatum(CURRENT_RANK)).doubleValue();
    }

    protected void setCurrentRankScore(Element v, double rankValue) {
        MutableDouble value = (MutableDouble) v.getUserDatum(CURRENT_RANK);

        if (value == null) {
            v.setUserDatum(CURRENT_RANK,new MutableDouble(rankValue),UserData.SHARED);
        } else {
            value.setDoubleValue(rankValue);
        }
    }

    protected void initializeRankings() {
         mPreviousRankingsMap = new HashMap();
         for (Iterator vIt = getVertices().iterator();vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            Set priors = getPriors();
            double numPriors = priors.size();

            if (getPriors().contains(currentVertex)) {
                setRankScore(currentVertex, 1.0/ numPriors);
                setCurrentRankScore(currentVertex, 1.0/ numPriors);
                mPreviousRankingsMap.put(currentVertex,new MutableDouble(1.0/numPriors));
            } else {
                setRankScore(currentVertex, 0);
                setCurrentRankScore(currentVertex, 0);
                mPreviousRankingsMap.put(currentVertex,new MutableDouble(0));

            }
        }
     }
    protected double evaluateIteration() {

        for (int i=0;i<mNumSteps;i++) {
            updateRankings();
            for (Iterator vIt = getVertices().iterator(); vIt.hasNext();) {
                Vertex currentVertex = (Vertex) vIt.next();
                double currentRankScore = getCurrentRankScore(currentVertex);
                MutableDouble previousRankScore = (MutableDouble) mPreviousRankingsMap.get(currentVertex);
                incrementRankScore(currentVertex,currentRankScore);
                previousRankScore.setDoubleValue(currentRankScore);
            }
        }

        normalizeRankings();

        return 0;
    }

    protected void onFinalize(Element udc) {
        udc.removeUserDatum(CURRENT_RANK);

    }

    protected void updateRankings() {

        for (Iterator vIt = getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();

//            Set incomingEdges = null;
//            if (getGraph().isDirected()) {
//                incomingEdges = currentVertex.getInEdges();
//            } else {
//                incomingEdges = currentVertex.getIncidentEdges();
//            }
            Set incomingEdges = currentVertex.getInEdges();

            double currentPageRankSum = 0;
            for (Iterator edgeIt = incomingEdges.iterator(); edgeIt.hasNext();) {
                Edge incomingEdge = (Edge) edgeIt.next();
                double currentWeight = getEdgeWeight(incomingEdge);
                currentPageRankSum += ((MutableDouble) mPreviousRankingsMap.get(incomingEdge.getOpposite(currentVertex))).doubleValue()*currentWeight;
            }

            setCurrentRankScore(currentVertex,currentPageRankSum);
        }

    }
}
