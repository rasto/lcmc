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

import java.util.Random;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * WattsBetaSmallWorldGenerator is a graph generator that produces a small
 * world network using the beta-model as proposed by Duncan Watts. The basic ideas is
 * to start with a one-dimensional ring lattice in which each vertex has k-neighbors and then randomly
 * rewire the edges, with probability beta, in such a way that a small world networks can be created for
 * certain values of beta and k that exhibit low charachteristic path lengths and high clustering coefficient.
 * @see "Small Worlds:The Dynamics of Networks between Order and Randomness by D.J. Watts"
 * @author Christopher Brooks, Scott White
 *
 */
public class WattsBetaSmallWorldGenerator extends Lattice1DGenerator {
    private int numNodes = 0;
    private double beta = 0;
    private int degree = 0;
    private Random random = new Random();

    /**
     * Constructs the small world graph generator.
     * @param numNodes the number of nodes in the ring lattice
     * @param beta the probability of an edge being rewired randomly; the proportion of randomly rewired edges in a graph.
     * @param degree the number of edges connected to each vertex; the local neighborhood size.
     */
    public WattsBetaSmallWorldGenerator(int numNodes, double beta, int degree) {
    	super(numNodes,true);
    	
        if (numNodes < 10) {
            throw new IllegalArgumentException("Lattice must contain at least 10 vertices.");
        }
        if (degree % 2 != 0) {
            throw new IllegalArgumentException("All nodes must have an even degree.");
        }
        if (beta > 1.0 || beta < 0.0) {
            throw new IllegalArgumentException("Beta must be between 0 and 1.");
        }
        this.numNodes = numNodes;
        this.beta = beta;
        this.degree = degree;

        //System.out.println("Creating a lattice with n="+nodes+", k="+degree+", and beta="+beta+".");
    }

    /**
     * Generates a beta-network from a 1-lattice according to the parameters given.
     * @return a beta-network model that is potentially a small-world
     */
    public ArchetypeGraph generateGraph() {
        Graph g = new UndirectedSparseGraph();
        GraphUtils.addVertices(g, numNodes);
        int upI = 0;//, downI = 0;
        Indexer id = Indexer.getIndexer(g);

        int numKNeighbors = (degree / 2);

        //create lattice structure
        for (int i = 0; i < numNodes; i++) {
            for (int s = 1; s <= numKNeighbors; s++) {
                Vertex ithVertex = (Vertex) id.getVertex(i);
                upI = upIndex(i, s);
//                downI = downIndex(i, s);
                GraphUtils.addEdge(g, ithVertex, (Vertex) id.getVertex(upI));
                //GraphUtils.addEdge((Graph) g, ithVertex, id.getVertex(downI));
            }
        }

        //rewire edges
        for (int i = 0; i < numNodes; i++) {
            for (int s = 1; s <= numKNeighbors; s++) {

                while (true) {
                    // randomly rewire a proportion, beta, of the edges in the graph.
                    double r = random.nextDouble();
                    if (r < beta) {
                        int v = (int) random.nextInt(numNodes);

                        Vertex vthVertex = (Vertex) id.getVertex(v);
                        Vertex ithVertex = (Vertex) id.getVertex(i);
                        Vertex kthVertex = (Vertex) id.getVertex(upIndex(i, s));
                        Edge e = ithVertex.findEdge(kthVertex);

                        if (!kthVertex.isNeighborOf(vthVertex) && kthVertex != vthVertex) {
                            g.removeEdge(e);
                            GraphUtils.addEdge(g, kthVertex, vthVertex);
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        }

        return g;
    }
}