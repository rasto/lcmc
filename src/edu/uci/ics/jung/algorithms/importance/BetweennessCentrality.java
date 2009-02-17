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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Element;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Decorator;
import edu.uci.ics.jung.graph.decorators.NumericDecorator;
import edu.uci.ics.jung.utils.MutableDouble;
import edu.uci.ics.jung.utils.PredicateUtils;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.UserDataUtils;

/**
 * Computes betweenness centrality for each vertex and edge in the graph. The result is that each vertex
 * and edge has a UserData element of type MutableDouble whose key is 'centrality.BetweennessCentrality'.
 * Note: Many social network researchers like to normalize the betweenness values by dividing the values by
 * (n-1)(n-2)/2. The values given here are unnormalized.<p>
 *
 * A simple example of usage is:
 * <pre>
 * BetweennessCentrality ranker = new BetweennessCentrality(someGraph);
 * ranker.evaluate();
 * ranker.printRankings();
 * </pre>
 *
 * Running time is: O(n^2 + nm).
 * @see "Ulrik Brandes: A Faster Algorithm for Betweenness Centrality. Journal of Mathematical Sociology 25(2):163-177, 2001."
 * @author Scott White
 */

public class BetweennessCentrality extends AbstractRanker {

    public static final String CENTRALITY = "centrality.BetweennessCentrality";

    /**
     * Constructor which initializes the algorithm
     * @param g the graph whose nodes are to be analyzed
     */
    public BetweennessCentrality(Graph g) {
        initialize(g, true, true);
    }

    public BetweennessCentrality(Graph g, boolean rankNodes) {
        initialize(g, rankNodes, true);
    }

    public BetweennessCentrality(Graph g, boolean rankNodes, boolean rankEdges)
    {
        initialize(g, rankNodes, rankEdges);
    }
    
    protected void computeBetweenness(Graph graph) {

        BetweennessDataDecorator decorator = new BetweennessDataDecorator();
        NumericDecorator bcDecorator = new NumericDecorator(CENTRALITY, UserData.SHARED);

        
        Set vertices = graph.getVertices();
        
        // clean up previous decorations, if any; otherwise the new calculations will 
        // incorporate the old data
        UserDataUtils.cleanup(vertices, getRankScoreKey());
        UserDataUtils.cleanup(graph.getEdges(), getRankScoreKey());

        for (Iterator vIt = vertices.iterator(); vIt.hasNext();) {
            Vertex s = (Vertex) vIt.next();

            initializeData(graph,decorator);

            decorator.data(s).numSPs = 1;
            decorator.data(s).distance = 0;

            Stack stack = new Stack();
            Buffer queue = new UnboundedFifoBuffer();
            queue.add(s);

            while (!queue.isEmpty()) {
                Vertex v = (Vertex) queue.remove();
                stack.push(v);

                for (Iterator nIt = v.getSuccessors().iterator(); nIt.hasNext();) {
                    Vertex w = (Vertex) nIt.next();

                    if (decorator.data(w).distance < 0) {
                        queue.add(w);
                        decorator.data(w).distance = decorator.data(v).distance + 1;
                    }

                    if (decorator.data(w).distance == decorator.data(v).distance + 1) {
                        decorator.data(w).numSPs += decorator.data(v).numSPs;
                        decorator.data(w).predecessors.add(v);
                    }
                }
            }

            while (!stack.isEmpty()) {
                Vertex w = (Vertex) stack.pop();

                for (Iterator v2It = decorator.data(w).predecessors.iterator(); v2It.hasNext();) {
                    Vertex v = (Vertex) v2It.next();
                    double partialDependency = (decorator.data(v).numSPs / decorator.data(w).numSPs);
                    partialDependency *= (1.0 + decorator.data(w).dependency);
                    decorator.data(v).dependency +=  partialDependency;
                    Edge currentEdge = v.findEdge(w);
                    MutableDouble edgeValue = (MutableDouble) bcDecorator.getValue(currentEdge);
                    edgeValue.add(partialDependency);
                }
                if (w != s) {
                    MutableDouble bcValue = (MutableDouble) bcDecorator.getValue(w);
                    bcValue.add(decorator.data(w).dependency);
                }
            }
        }

        if (PredicateUtils.enforcesEdgeConstraint(graph, Graph.UNDIRECTED_EDGE)) {
            for (Iterator v3It = vertices.iterator(); v3It.hasNext();) {
                MutableDouble bcValue = (MutableDouble) bcDecorator.getValue((Vertex) v3It.next());
                bcValue.setDoubleValue(bcValue.doubleValue() / 2.0);
            }
            for (Iterator eIt = graph.getEdges().iterator(); eIt.hasNext();) {
                MutableDouble bcValue = (MutableDouble) bcDecorator.getValue((Edge) eIt.next());
                bcValue.setDoubleValue(bcValue.doubleValue() / 2.0);
            }
        }

        for (Iterator vIt = vertices.iterator(); vIt.hasNext();) {
            Vertex vertex = (Vertex) vIt.next();
            decorator.removeValue(vertex);
        }

    }

    private void initializeData(Graph g, BetweennessDataDecorator decorator) {
        for (Iterator vIt = g.getVertices().iterator(); vIt.hasNext();) {
            Vertex vertex = (Vertex) vIt.next();

            if (vertex.getUserDatum(CENTRALITY) == null) {
                vertex.addUserDatum(CENTRALITY, new MutableDouble(), UserData.SHARED);
            }

            decorator.setData(new BetweennessData(), vertex);
        }
        for (Iterator eIt = g.getEdges().iterator(); eIt.hasNext();) {
            Edge e = (Edge) eIt.next();

            if (e.getUserDatum(CENTRALITY) == null) {
                e.addUserDatum(CENTRALITY, new MutableDouble(), UserData.SHARED);
            }
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
        computeBetweenness(getGraph());
        return 0;
    }

    class BetweennessDataDecorator extends Decorator {
        public BetweennessDataDecorator() {
            super("centrality.BetwennessData", UserData.REMOVE);
        }

        public BetweennessData data(Element udc) {
            return (BetweennessData) udc.getUserDatum(getKey());
        }

        public void setData(BetweennessData value, Element udc) {
            udc.setUserDatum(getKey(), value, getCopyAction());
        }

    }

    class BetweennessData {
        double distance;
        double numSPs;
        List predecessors;
        double dependency;

        BetweennessData() {
            distance = -1;
            numSPs = 0;
            predecessors = new ArrayList();
            dependency = 0;
        }
    }
}
