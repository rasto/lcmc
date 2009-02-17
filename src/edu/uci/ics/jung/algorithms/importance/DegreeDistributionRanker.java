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

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.PredicateUtils;

/**
 * A simple node importance ranker based on the degree of the node. The user can specify whether s/he wants
 * to use the indegree or the outdegree as the metric. If the graph is undirected this option is effectively
 * ignored. So for example, if the graph is directed and the user chooses to use in-degree, nodes with the highest
 * in-degree will be ranked highest and similarly nodes with the lowest in-degree will be ranked lowest.
 * <p>
 * A simple example of usage is:
 * <pre>
 * DegreeDistributionRanker ranker = new DegreeDistributionRanker(someGraph);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 * 
 * @author Scott White
 */
public class DegreeDistributionRanker extends AbstractRanker {
    public final static String KEY = "jung.algorithms.importance.DegreeDistributionRanker.RankScore";

    private boolean mUseInDegree;
    private boolean directed;
    
    /**
     * Default constructor which assumes if the graph is directed the indegree is to be used.
     * @param graph the graph whose nodes are to be ranked based on indegree
     */
    public DegreeDistributionRanker(Graph graph) {
        this(graph, true);
    }

    /**
     * This constructor allows you to specify whether to use indegree or outdegree.
     * @param graph the graph whose nodes are to be ranked based
     * @param useInDegree if <code>true</code>, indicates indegree is to be used, if <code>false</code> outdegree
     */
    public DegreeDistributionRanker(Graph graph,boolean useInDegree) {
        initialize(graph,true,false);
        mUseInDegree = useInDegree;
        directed = PredicateUtils.enforcesEdgeConstraint(getGraph(), Graph.DIRECTED_EDGE);
    }


    protected double evaluateIteration() {
        Vertex currentVertex = null;
        for (Iterator it = getVertices().iterator(); it.hasNext();) {
            currentVertex = (Vertex) it.next();
            if (directed)
            {
                if (mUseInDegree)
                    setRankScore(currentVertex,(currentVertex).inDegree());
                else
                    setRankScore(currentVertex,(currentVertex).outDegree());
            }
            else
                setRankScore(currentVertex,currentVertex.degree());
        }
        normalizeRankings();

        return 0;
    }

    public String getRankScoreKey() {
        return KEY;
    }
}
