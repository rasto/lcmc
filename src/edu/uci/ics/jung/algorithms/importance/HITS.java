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
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.UserData;

/**
 * Calculates the "hubs-and-authorities" importance measures for each node in a graph.
 * These measures are defined recursively as follows:
 *
 * <ul>
 * <li>The *hubness* of a node is the degree to which a node links to other important authorities</li>
 * <li>The *authoritativeness* of a node is the degree to which a node is pointed to by important hubs</li>
 * <p>
 * Note: This algorithm uses the same key as HITSWithPriors for storing rank sccores.
 * <p>
 * A simple example of usage is:
 * <pre>
 * HITS ranker = new HITS(someGraph);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 * <p>
 * Running time: O(|V|*I) where |V| is the number of vertices and I is the number of iterations until convergence
 *
 * @author Scott White
 * @see "Authoritative sources in a hyperlinked environment by Jon Kleinberg, 1997"
 */
public class HITS extends AbstractRanker {
    protected static final String AUTHORITY_KEY = "jung.algorithms.importance.AUTHORITY";
    protected static final String HUB_KEY = "jung.algorithms.importance.HUB";
    private String mKeyToUseForRanking;
    private Map mPreviousAuthorityScores;
    private Map mPreviousHubScores;

    /**
     * Constructs an instance of the ranker where the type of importance that is associated with the
     * rank score is the node's importance as an authority.
     * @param graph the graph whose nodes are to be ranked
     * @param useAuthorityForRanking
     */
    public HITS(Graph graph, boolean useAuthorityForRanking) {
        mKeyToUseForRanking = AUTHORITY_KEY;
        if (!useAuthorityForRanking) {
         mKeyToUseForRanking = HUB_KEY;
        }
        initialize(graph);
    }

    /**
     * Constructs an instance of the ranker where the type of importance that is associated with the
     * rank score is the node's importance as an authority.
     * @param graph the graph whose nodes are to be ranked
     */
    public HITS(Graph graph) {
        mKeyToUseForRanking = AUTHORITY_KEY;
        initialize(graph);
    }

    protected void initialize(Graph g) {

        super.initialize(g, true, false);

        mPreviousAuthorityScores = new HashMap();
        mPreviousHubScores = new HashMap();

        for (Iterator vIt = g.getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            setRankScore(currentVertex, 1.0, AUTHORITY_KEY);
            setRankScore(currentVertex, 1.0, HUB_KEY);

            mPreviousAuthorityScores.put(currentVertex, new MutableDouble(0));
            mPreviousHubScores.put(currentVertex, new MutableDouble(0));
        }
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

        //Normalize rankings and test for convergence
        int numVertices = getVertices().size();
        for (Iterator vIt = getVertices().iterator(); vIt.hasNext();) {
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

    private double computeSum(Set neighbors, String key) {
        double sum = 0;
        for (Iterator neighborIt = neighbors.iterator(); neighborIt.hasNext();) {
            Vertex currentNeighbor = (Vertex) neighborIt.next();
            sum += getRankScore(currentNeighbor, key);
        }
        return sum;
    }

    private void normalizeRankings(double normConstant, String key) {
        for (Iterator vertexIt = getVertices().iterator(); vertexIt.hasNext();) {
            Vertex v = (Vertex) vertexIt.next();
            double rankScore = getRankScore(v,key);
            setRankScore(v,rankScore/normConstant,key);
        }
    }

    protected void updateAuthorityRankings() {
        double total = 0;
        //compute authority scores
        for (Iterator vertexIt = getVertices().iterator(); vertexIt.hasNext();) {
            Vertex currentVertex = (Vertex) vertexIt.next();
            double currentHubSum = computeSum(currentVertex.getPredecessors(), HUB_KEY);
            double newAuthorityScore = currentHubSum;
            total += newAuthorityScore;
            setRankScore(currentVertex, newAuthorityScore, AUTHORITY_KEY);
        }


        normalizeRankings(total, AUTHORITY_KEY);
    }

    protected void updateHubRankings() {
        double total = 0;

        //compute hub scores
        for (Iterator vertexIt = getVertices().iterator(); vertexIt.hasNext();) {
            Vertex currentVertex = (Vertex) vertexIt.next();
            double currentAuthoritySum = computeSum(currentVertex.getSuccessors(), AUTHORITY_KEY);
            double newHubScore = currentAuthoritySum;
            total += newHubScore;
            setRankScore(currentVertex, newHubScore, HUB_KEY);
        }
        normalizeRankings(total, HUB_KEY);
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
