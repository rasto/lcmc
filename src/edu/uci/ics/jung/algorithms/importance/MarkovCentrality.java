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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix1D;
import edu.uci.ics.jung.algorithms.GraphMatrixOperations;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.UserData;

/**
 * @author Scott White and Joshua O'Madadhain
 * @see "Algorithms for Estimating Relative Importance in Graphs by Scott White and Padhraic Smyth, 2003"
 */
public class MarkovCentrality extends RelativeAuthorityRanker {
    public final static String MEAN_FIRST_PASSAGE_TIME = "jung.algorithms.importance.mean_first_passage_time";
    private DoubleMatrix1D mRankings;
    private Indexer mIndexer;

    public MarkovCentrality(DirectedGraph graph, Set rootNodes) {
        this(graph,rootNodes,null);
    }

    public MarkovCentrality(DirectedGraph graph, Set rootNodes, String edgeWeightKey) {
        super.initialize(graph, true, false);
        setPriors(rootNodes);
        if (edgeWeightKey == null)
            assignDefaultEdgeTransitionWeights();
        else
            setUserDefinedEdgeWeightKey(edgeWeightKey);
        normalizeEdgeTransitionWeights();

        mIndexer = Indexer.getIndexer(graph);
        mRankings = new SparseDoubleMatrix1D(graph.numVertices());
    }

    /**
     * @see edu.uci.ics.jung.algorithms.importance.AbstractRanker#getRankScoreKey()
     */
    public String getRankScoreKey() {
        return MEAN_FIRST_PASSAGE_TIME;
    }

    /**
     * @see edu.uci.ics.jung.algorithms.importance.AbstractRanker#getRankScore(edu.uci.ics.jung.graph.Element)
     */
    public double getRankScore(Element vert) {
        ArchetypeVertex v = (ArchetypeVertex) vert;
        return mRankings.get(mIndexer.getIndex(v));
    }

    /**
     * @see edu.uci.ics.jung.algorithms.importance.AbstractRanker#setRankScore(edu.uci.ics.jung.graph.Element, double)
     */
    protected void setRankScore(Element v, double rankValue) {
        v.setUserDatum(getRankScoreKey(), new MutableDouble(rankValue), UserData.SHARED);
    }

    /**
     * @see edu.uci.ics.jung.algorithms.IterativeProcess#evaluateIteration()
     */
    protected double evaluateIteration() {
        DoubleMatrix2D mFPTMatrix = GraphMatrixOperations.computeMeanFirstPassageMatrix(getGraph(), getEdgeWeightKeyName(), getStationaryDistribution());

        mRankings.assign(0);

        for (Iterator p_iter = getPriors().iterator(); p_iter.hasNext();) {
            Vertex p = (Vertex) p_iter.next();
            int p_id = mIndexer.getIndex(p);
            for (Iterator v_iter = getVertices().iterator(); v_iter.hasNext();) {
                Vertex v = (Vertex) v_iter.next();
                int v_id = mIndexer.getIndex(v);
                mRankings.set(v_id, mRankings.get(v_id) + mFPTMatrix.get(p_id, v_id));
            }
        }

        for (Iterator v_iter = getVertices().iterator(); v_iter.hasNext();) {
            Vertex v = (Vertex) v_iter.next();
            int v_id = mIndexer.getIndex(v);
            mRankings.set(v_id, 1 / (mRankings.get(v_id) / getPriors().size()));
        }

        double total = mRankings.zSum();

        for (Iterator v_iter = getVertices().iterator(); v_iter.hasNext();) {
            Vertex v = (Vertex) v_iter.next();
            int v_id = mIndexer.getIndex(v);
            mRankings.set(v_id, mRankings.get(v_id) / total);
        }

        return 0;
    }


    /**
     * Loads the stationary distribution into a vector if it was passed in,
     * or calculates it if not.
     *
     * @return DoubleMatrix1D
     */
    private DoubleMatrix1D getStationaryDistribution() {
        DoubleMatrix1D piVector = new DenseDoubleMatrix1D(getVertices().size());
        PageRank pageRank = new PageRank((DirectedGraph) getGraph(), 0, getEdgeWeightKeyName());
        pageRank.evaluate();
        List rankings = pageRank.getRankings();

        for (Iterator r_iter = rankings.iterator(); r_iter.hasNext();) {
            NodeRanking rank = (NodeRanking) r_iter.next();
            piVector.set(mIndexer.getIndex(rank.vertex), rank.rankScore);
        }
        return piVector;
    }

}
