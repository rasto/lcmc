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

import cern.colt.matrix.DoubleMatrix2D;
import edu.uci.ics.jung.algorithms.GraphMatrixOperations;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;

/**
 * /**
 * Computes s-t betweenness centrality for each vertex in the graph. The betweenness values in this case
 * are based on random walks, measuring the expected number of times a node is traversed by a random walk
 * from s to t. The result is that each vertex has a UserData element of type
 * MutableDouble whose key is 'centrality.RandomWalkBetweennessCentrality'
 *
 * A simple example of usage is:  <br>
 * RandomWalkSTBetweenness ranker = new RandomWalkBetweenness(someGraph,someSource,someTarget);   <br>
 * ranker.evaluate(); <br>
 * ranker.printRankings(); <p>
 *
 * Running time is: O(n^3).
 * @see "Mark Newman: A measure of betweenness centrality based on random walks, 2002."

 * @author Scott White
 */
public class RandomWalkSTBetweenness extends AbstractRanker {

    public static final String CENTRALITY = "centrality.RandomWalkSTBetweennessCentrality";
    private DoubleMatrix2D mVoltageMatrix;
    private Indexer mIndexer;
    Vertex mSource;
    Vertex mTarget;

   /**
    * Constructor which initializes the algorithm
    * @param g the graph whose nodes are to be analyzed
    * @param s the source vertex
    * @param t the target vertex
    */
    public RandomWalkSTBetweenness(UndirectedGraph g, Vertex s, Vertex t) {
        initialize(g, true, false);
        mSource = s;
        mTarget = t;
    }

    protected Indexer getIndexer() {
        return mIndexer;
    }

    protected DoubleMatrix2D getVoltageMatrix() {
        return mVoltageMatrix;
    }

    protected void setUp() {
        mVoltageMatrix = GraphMatrixOperations.computeVoltagePotentialMatrix((UndirectedGraph) getGraph());
        mIndexer = Indexer.getIndexer(getGraph());
    }

    protected void computeBetweenness() {
        setUp();

        for (Iterator iIt = getGraph().getVertices().iterator();iIt.hasNext();) {
            Vertex ithVertex = (Vertex) iIt.next();

            setRankScore(ithVertex,computeSTBetweenness(ithVertex,mSource, mTarget));
        }
    }

    public double computeSTBetweenness(Vertex ithVertex, Vertex source, Vertex target) {
        if (ithVertex == source || ithVertex == target) return 1;
        if (mVoltageMatrix == null) {
            setUp();
        }
        int i = mIndexer.getIndex(ithVertex);
        int s = mIndexer.getIndex(source);
        int t = mIndexer.getIndex(target);

        double betweenness = 0;
        for (Iterator vIt = ithVertex.getSuccessors().iterator();vIt.hasNext();) {
            Vertex jthVertex = (Vertex) vIt.next();
            int j = mIndexer.getIndex(jthVertex);
            double currentFlow = 0;
            currentFlow += mVoltageMatrix.get(i,s);
            currentFlow -= mVoltageMatrix.get(i,t);
            currentFlow -= mVoltageMatrix.get(j,s);
            currentFlow += mVoltageMatrix.get(j,t);
            betweenness += Math.abs(currentFlow);
        }

        return betweenness/2.0;

    }

    /**
     * the user datum key used to store the rank scores
     * @return the key
     */
    public String getRankScoreKey() {
        return CENTRALITY;
    }

    protected double evaluateIteration() {
        computeBetweenness();
        return 0;
    }
}
