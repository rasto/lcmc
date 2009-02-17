/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.random.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * Graph generator that generates undirected sparse graphs with power-law distributions.
 * @author Scott White
 * @see "A Steady State Model for Graph Power Law by David Eppstein and Joseph Wang"
 */
public class EppsteinPowerLawGenerator implements GraphGenerator {
    private int mNumVertices;
    private int mNumEdges;
    private int mNumIterations;
    private double mMaxDegree;
    private Random mRandom;

    /**
     * Constructor which specifies the parameters of the generator
     * @param numVertices the number of vertices for the generated graph
     * @param numEdges the number of edges the generated graph will have, should be Theta(numVertices)
     * @param r the model parameter. The larger the value for this parameter the better the graph's degree
     * distribution will approximate a power-law.
     */
    public EppsteinPowerLawGenerator(int numVertices, int numEdges,int r) {
        mNumVertices = numVertices;
        mNumEdges = numEdges;
        mNumIterations = r;
        mRandom = new Random();
    }

    protected Graph initializeGraph() {
        Graph graph = null;
        graph = new UndirectedSparseGraph();
        GraphUtils.addVertices(graph,mNumVertices);

        Indexer id = Indexer.getIndexer(graph);

        while (graph.numEdges() < mNumEdges) {
            Vertex u = (Vertex) id.getVertex((int) (mRandom.nextDouble() * mNumVertices));
            Vertex v = (Vertex) id.getVertex((int) (mRandom.nextDouble() * mNumVertices));
            if (!v.isSuccessorOf(u)) {
                GraphUtils.addEdge(graph,u,v);
            }
        }

        double maxDegree = 0;
        for (Iterator vIt=graph.getVertices().iterator(); vIt.hasNext();) {
            Vertex v = (Vertex) vIt.next();
            maxDegree = Math.max(v.degree(),maxDegree);
        }
        mMaxDegree = maxDegree; //(maxDegree+1)*(maxDegree)/2;

        return graph;
    }

    /**
     * Generates a graph whose degree distribution approximates a power-law.
     * @return the generated graph
     */
    public ArchetypeGraph generateGraph() {
        Graph graph = initializeGraph();

        Indexer id = Indexer.getIndexer(graph);
        for (int rIdx = 0; rIdx < mNumIterations; rIdx++) {

            Vertex v = null;
            int degree = 0;
            do {
                v = (Vertex) id.getVertex((int) (mRandom.nextDouble() * mNumVertices));
                degree = v.degree();

            } while (degree == 0);

            List edges = new ArrayList(v.getIncidentEdges());
            Edge randomExistingEdge = (Edge) edges.get((int) (mRandom.nextDouble()*degree));

            Vertex x = (Vertex) id.getVertex((int) (mRandom.nextDouble() * mNumVertices));
            Vertex y = null;
            do {
                y = (Vertex) id.getVertex((int) (mRandom.nextDouble() * mNumVertices));

            } while (mRandom.nextDouble() > ((double) (y.degree()+1)/mMaxDegree));

            if (!y.isSuccessorOf(x) && x != y) {
                graph.removeEdge(randomExistingEdge);
                GraphUtils.addEdge(graph,x,y);
            }
        }

        return graph;
    }

    public void setSeed(long seed) {
        mRandom.setSeed(seed);
    }
}
