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
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * Random Generator of Erdos-Renyi "binomial model"
 *  @author William Giordano, Scott White, Joshua O'Madadhain
 */
public class ErdosRenyiGenerator implements GraphGenerator
{
    private int mNumVertices;
    private double mEdgeConnectionProbability;
    private Random mRandom;

    /**
     *
     * @param numVertices number of vertices graph should have
     * @param p Connection's probability between 2 vertices
     */
	public ErdosRenyiGenerator(int numVertices,double p)
    {
        if (numVertices <= 0) {
            throw new IllegalArgumentException("A positive # of vertices must be specified.");
        }
        mNumVertices = numVertices;
        if (p < 0 || p > 1) {
            throw new IllegalArgumentException("p must be between 0 and 1.");
        }
        mEdgeConnectionProbability = p;
        mRandom = new Random();
	}

    /**
     * Returns a graph in which each pair of vertices is connected by 
     * an undirected edge with the probability specified by the constructor.
     */
	public ArchetypeGraph generateGraph() {
        UndirectedGraph g = new UndirectedSparseGraph();
        GraphUtils.addVertices(g,mNumVertices);
        Object[] v_array = g.getVertices().toArray();

		for (int i = 0; i < mNumVertices-1; i++)
		{
            Vertex v_i = (Vertex) v_array[i];
			for (int j = i+1; j < mNumVertices; j++)
			{
                Vertex v_j = (Vertex) v_array[j];
				if (mRandom.nextDouble() < mEdgeConnectionProbability)
					g.addEdge(new UndirectedSparseEdge(v_i, v_j));
			}
		}
        return g;
    }

    public void setSeed(long seed) {
        mRandom.setSeed(seed);
    }
}











