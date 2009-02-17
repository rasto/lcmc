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

import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Computes betweenness centrality for each vertex in the graph. The betweenness values in this case
 * are based on random walks, measuring the expected number of times a node is traversed by a random walk
 * averaged over all pairs of nodes. The result is that each vertex has a UserData element of type
 * MutableDouble whose key is 'centrality.RandomWalkBetweennessCentrality'
 *
 * A simple example of usage is:  <br>
 * RandomWalkBetweenness ranker = new RandomWalkBetweenness(someGraph);   <br>
 * ranker.evaluate(); <br>
 * ranker.printRankings(); <p>
 *
 * Running time is: O((m+n)*n^2).
 * @see "Mark Newman: A measure of betweenness centrality based on random walks, 2002."

 * @author Scott White
 */
public class RandomWalkBetweenness extends RandomWalkSTBetweenness {

    public static final String CENTRALITY = "centrality.RandomWalkBetweennessCentrality";

    /**
     * Constructor which initializes the algorithm
     * @param g the graph whose nodes are to be analyzed
     */
    public RandomWalkBetweenness(UndirectedGraph g) {
       super(g,null,null);
    }

    protected void computeBetweenness() {
        setUp();

        int numVertices = getGraph().numVertices();
        double normalizingConstant = numVertices*(numVertices-1)/2.0;

        for (Iterator iIt = getGraph().getVertices().iterator();iIt.hasNext();) {
            Vertex ithVertex = (Vertex) iIt.next();

            double ithBetweenness = 0;
            for (int t=0;t<numVertices;t++) {
                for (int s=0;s<t;s++) {
                    Vertex sthVertex = (Vertex) getIndexer().getVertex(s);
                    Vertex tthVertex = (Vertex) getIndexer().getVertex(t);
                    ithBetweenness += computeSTBetweenness(ithVertex,sthVertex, tthVertex);
                }
            }
            setRankScore(ithVertex,ithBetweenness/normalizingConstant);
        }
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
