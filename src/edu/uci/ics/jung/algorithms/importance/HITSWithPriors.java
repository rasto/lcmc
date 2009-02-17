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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.NumericalPrecision;
import edu.uci.ics.jung.utils.UserData;

/**
 * Algorithm that extends the HITS algorithm by incorporating root nodes (priors). Whereas in HITS
 * the importance of a node is implicitly computed relative to all nodes in the graph, now importance
 * is computed relative to the specified root nodes.
 * <p>
 * A simple example of usage is:
 * <pre>
 * HITSWithPriors ranker = new HITSWithPriors(someGraph,0.3,rootSet);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 * <p>
 * Running time: O(|V|*I) where |V| is the number of vertices and I is the number of iterations until convergence
 *
 * @author Scott White
 * @see "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
 */
public class HITSWithPriors extends RelativeAuthorityRanker {
    protected static final String AUTHORITY_KEY = "jung.algorithms.importance.AUTHORITY";
    protected static final String HUB_KEY = "jung.algorithms.importance.HUB";
    private static final String IN_EDGE_WEIGHT = "IN_EDGE_WEIGHT";
    private String mKeyToUseForRanking;
    private Map mPreviousAuthorityScores;
    private Map mPreviousHubScores;
    private double mBeta;
    Set mReachableVertices;
    private Set mLeafNodes;

    /**
     * Constructs an instance of the ranker where the type of importance that is associated with the
     * rank score is the node's importance as an authority.
     * @param graph the graph whose nodes are to be ranked
     * @param bias the weight that should be placed on the root nodes (between 0 and 1)
     * @param priors the set of root nodes
     */
    public HITSWithPriors(Graph graph, double bias, Set priors) {
        mKeyToUseForRanking = AUTHORITY_KEY;
        mBeta = bias;
        setPriors(priors);
        initialize(graph, null);
    }

    /**
     * More specialized constructor where the type of importance can be specified.
     * @param graph the graph whose nodes are to be ranked
     * @param useAuthorityForRanking
     * @param bias the weight that should be placed on the root nodes (between 0 and 1)
     * @param priors the set of root nodes
     */
    public HITSWithPriors(Graph graph, boolean useAuthorityForRanking, double bias, Set priors, String edgeWeightKey) {
        setUseAuthorityForRanking(useAuthorityForRanking);
        mBeta = bias;
        setPriors(priors);
        initialize(graph, edgeWeightKey);
    }

    protected void initialize(Graph g, String edgeWeightKeyName) {

        super.initialize(g, true, false);

        mPreviousAuthorityScores = new HashMap();
        mPreviousHubScores = new HashMap();


//        int numVertices = getVertices().size();
        for (Iterator vIt = g.getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();

            mPreviousAuthorityScores.put(currentVertex, new MutableDouble(0));
            mPreviousHubScores.put(currentVertex, new MutableDouble(0));

            setRankScore(currentVertex, 0, AUTHORITY_KEY);
            setRankScore(currentVertex, 0, HUB_KEY);

            setPriorRankScore(currentVertex, 0);
        }

        WeakComponentClusterer wcExtractor = new WeakComponentClusterer();
        ClusterSet clusters = wcExtractor.extract(g);
        mReachableVertices = new HashSet();

        double numPriors = getPriors().size();
        for (Iterator it = getPriors().iterator(); it.hasNext();) {
            Vertex currentVertex = (Vertex) it.next();
            setPriorRankScore(currentVertex, 1.0 / numPriors);
            for (Iterator cIt = clusters.iterator(); cIt.hasNext();) {
                Set members = (Set) cIt.next();
                if (members.contains(currentVertex)) {
                    mReachableVertices.addAll(members);
                }
            }
        }

        mLeafNodes = new HashSet();
        int numReachableVertices = mReachableVertices.size();
        for (Iterator vIt = mReachableVertices.iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            setRankScore(currentVertex, 1.0 / numReachableVertices, AUTHORITY_KEY);
            setRankScore(currentVertex, 1.0 / numReachableVertices, HUB_KEY);
            if (currentVertex.outDegree() == 0) {
                mLeafNodes.add(currentVertex);
            }
        }

        if (edgeWeightKeyName == null) {
            assignDefaultEdgeTransitionWeights();
        } else {
            setUserDefinedEdgeWeightKey(edgeWeightKeyName);
            normalizeEdgeTransitionWeights();
        }
        assignInlinkEdgeTransitionWeights();

    }

    protected void finalizeIterations() {
        super.finalizeIterations();
        for (Iterator it = getVertices().iterator(); it.hasNext();) {
            Vertex currentVertex = (Vertex) it.next();
            if (mKeyToUseForRanking.equals(AUTHORITY_KEY)) {
                currentVertex.removeUserDatum(HUB_KEY);
            } else {
                currentVertex.removeUserDatum(AUTHORITY_KEY);
            }
        }

    }

    protected double getInEdgeWeight(Edge e) {
        MutableDouble value = (MutableDouble) e.getUserDatum(IN_EDGE_WEIGHT);
        return value.doubleValue();
    }

    protected void setInEdgeWeight(Edge e, double weight) {
        MutableDouble value = (MutableDouble) e.getUserDatum(IN_EDGE_WEIGHT);
        if (value == null) {
            e.setUserDatum(IN_EDGE_WEIGHT, new MutableDouble(weight), UserData.SHARED);
        } else {
            value.setDoubleValue(weight);
        }
    }

    private void assignInlinkEdgeTransitionWeights() {

        for (Iterator vIt = getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();

//            Set incomingEdges = null;
//            if (getGraph().isDirected()) {
//                incomingEdges = currentVertex.getInEdges();
//            } else {
//                incomingEdges = currentVertex.getIncidentEdges();
//            }
            Set incomingEdges = currentVertex.getInEdges();

            double total = 0;
            for (Iterator edgeIt = incomingEdges.iterator(); edgeIt.hasNext();) {
                Edge currentEdge = (Edge) edgeIt.next();
                total += getEdgeWeight(currentEdge);
            }

            for (Iterator edgeIt = incomingEdges.iterator(); edgeIt.hasNext();) {
                Edge currentEdge = (Edge) edgeIt.next();
                double weight = getEdgeWeight(currentEdge);
                setInEdgeWeight(currentEdge, weight / total);
            }
        }
    }


    /**
     * the user datum key used to store the rank scores
     * @return the key
     */
    public String getRankScoreKey() {
        return mKeyToUseForRanking;
    }

    /**
     * Given a node, returns the corresponding rank score. This implementation of <code>getRankScore</code> assumes
     * the decoration representing the rank score is of type <code>MutableDouble</code>.
     * @return the rank score for this node
     */
    public double getRankScore(Element v) {
        return getRankScore(v, mKeyToUseForRanking);
    }

    /**
     * Given a node and a key, returns the corresponding rank score. Assumes the decoration representing the rank score
     * is of type <code>MutableDouble</code>.
     * @param v the node in question
     * @param key the user datum key that indexes the rank score value
     * @return the rank score for this node
     */
    protected double getRankScore(Element v, String key) {
        return ((MutableDouble) v.getUserDatum(key)).doubleValue();
    }

    protected double getPreviousAuthorityScore(Element v) {
        return ((MutableDouble) mPreviousAuthorityScores.get(v)).doubleValue();
    }

    protected double getPreviousHubScore(Element v) {
        return ((MutableDouble) mPreviousHubScores.get(v)).doubleValue();
    }

    protected void setRankScore(Element v, double rankValue, String key) {
        MutableDouble value = (MutableDouble) v.getUserDatum(key);

        if (value == null) {
            v.setUserDatum(key, new MutableDouble(rankValue), UserData.SHARED);
        } else {
            value.setDoubleValue(rankValue);
        }
    }

    protected void setRankScore(Element v, double rankValue) {
        setRankScore(v, rankValue, mKeyToUseForRanking);
    }

    protected double evaluateIteration() {
        updatePreviousScores();

        //Perform 2 update steps
        updateAuthorityRankings();
        updateHubRankings();

        double hubMSE = 0;
        double authorityMSE = 0;

        //Test for convergence
        int numVertices = mReachableVertices.size();
        for (Iterator vIt = mReachableVertices.iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();

            double currentAuthorityScore = getRankScore(currentVertex, AUTHORITY_KEY);
            double currentHubScore = getRankScore(currentVertex, HUB_KEY);

            double previousAuthorityScore = getPreviousAuthorityScore(currentVertex);
            double previousHubScore = getPreviousHubScore(currentVertex);

            hubMSE += Math.pow(currentHubScore - previousHubScore, 2);
            authorityMSE += Math.pow(currentAuthorityScore - previousAuthorityScore, 2);
        }

        hubMSE = Math.pow(hubMSE / numVertices, 0.5);
        authorityMSE = Math.pow(authorityMSE / numVertices, 0.5);

        return hubMSE + authorityMSE;
    }

    /**
     * If <code>evaluate()</code> has not already been called, the user can override the type of importance.
     * (hub or authority) that should be associated with the rank score.
     * @param useAuthorityForRanking if <code>true</code>, authority is used; if <code>false</code>, hub is used
     */
    public void setUseAuthorityForRanking(boolean useAuthorityForRanking) {
        if (useAuthorityForRanking) {
            mKeyToUseForRanking = AUTHORITY_KEY;
        } else {
            mKeyToUseForRanking = HUB_KEY;
        }
    }

    private double computeSum(Vertex v, String key) {

        Set edges = null;
        String oppositeKey = null;
        if (key.equals(HUB_KEY)) {
            edges = v.getOutEdges();
            oppositeKey = AUTHORITY_KEY;
            //leafNodes = mInLeafNodes;
        } else {
            edges = v.getInEdges();
            oppositeKey = HUB_KEY;
        }

        double sum = 0;
        for (Iterator edgeIt = edges.iterator(); edgeIt.hasNext();) {
            Edge e = (Edge) edgeIt.next();
            //if (mUnreachableVertices.contains(incomingEdge.getOpposite(currentVertex)))
            //    continue;

            double currentWeight = 0;
            if (key.equals(AUTHORITY_KEY)) {
                currentWeight = getEdgeWeight(e);
            } else {
                currentWeight = getInEdgeWeight(e);
            }
            sum += getRankScore(e.getOpposite(v), oppositeKey) * currentWeight;
        }

        if (getPriorRankScore(v) > 0) {
            if (key.equals(AUTHORITY_KEY)) {
                for (Iterator leafIt = mLeafNodes.iterator(); leafIt.hasNext();) {
                    Vertex leafNode = (Vertex) leafIt.next();
                    double currentWeight = getPriorRankScore(v);
                    sum += getRankScore(leafNode, oppositeKey) * currentWeight;
                }
            }
        }

        return sum;
    }

    protected void updateAuthorityRankings() {
        double authoritySum = 0;

        //compute authority scores
        for (Iterator vertexIt = mReachableVertices.iterator(); vertexIt.hasNext();) {
            Vertex currentVertex = (Vertex) vertexIt.next();
            double newAuthorityScore = computeSum(currentVertex, AUTHORITY_KEY) * (1.0 - mBeta) + mBeta * getPriorRankScore(currentVertex);
            authoritySum += newAuthorityScore;
            setRankScore(currentVertex, newAuthorityScore, AUTHORITY_KEY);
        }

        if (!NumericalPrecision.equal(authoritySum, 1.0, .1)) {
            System.err.println("HITS With Priors scores can not be generrated because the specified graph is not connected.");
            System.err.println("Authority Sum: " + authoritySum);
        }

    }

    protected void updateHubRankings() {
        double hubSum = 0;
        //compute hub scores
        for (Iterator vertexIt = mReachableVertices.iterator(); vertexIt.hasNext();) {
            Vertex currentVertex = (Vertex) vertexIt.next();
            double newHubScore = computeSum(currentVertex, HUB_KEY) * (1.0 - mBeta) + mBeta * getPriorRankScore(currentVertex);
            hubSum += newHubScore;
            setRankScore(currentVertex, newHubScore, HUB_KEY);
        }

        if (!NumericalPrecision.equal(hubSum, 1.0, .1)) {
            System.err.println("HITS With Priors scores can not be generrated because the specified graph is not connected.");
            System.err.println("Hub Sum: " + hubSum);
        }
    }


    protected void updatePreviousScores() {
        for (Iterator vIt = getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            MutableDouble previousAuthorityScore = (MutableDouble) mPreviousAuthorityScores.get(currentVertex);
            double currentAuthorityScore = getRankScore(currentVertex, AUTHORITY_KEY);
            previousAuthorityScore.setDoubleValue(currentAuthorityScore);

            MutableDouble previousHubScore = (MutableDouble) mPreviousHubScores.get(currentVertex);
            double currentHubScore = getRankScore(currentVertex, HUB_KEY);
            previousHubScore.setDoubleValue(currentHubScore);
        }
    }

}
