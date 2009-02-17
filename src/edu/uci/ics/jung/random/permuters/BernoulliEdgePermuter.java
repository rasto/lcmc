/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.random.permuters;

import java.util.HashMap;
import java.util.Map;

import cern.jet.random.sampling.RandomSampler;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.utils.MutableInteger;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.PredicateUtils;

/**
 * An edge permuter that permutes edges by sampling uniformly at random a given number of possible edges and for each
 * that exists that edge is removed and for each that doesn't exist that edge is added. The user can specify
 * with what probability this should removal/addition process should happen.
 * @author Scott White
 */
public class BernoulliEdgePermuter implements EdgePermuter {
	Map mEdgeIndexLookupTable;
	private long[] mPermuteEdgeSample;
	private int mNumEdgesToPermute;

    /**
     * Constructs the edge permuter.
     * @param numEdgesToPermute the number of edges to permute
     */
	public BernoulliEdgePermuter(int numEdgesToPermute) {
		mEdgeIndexLookupTable = new HashMap();
		mNumEdgesToPermute = numEdgesToPermute;
	}

	protected void initialize(Graph g) {
		mPermuteEdgeSample = new long[mNumEdgesToPermute];

		int numVertices = g.numVertices();
		Indexer id = Indexer.getIndexer(g);

		int edgeCtr = 0;
		for (int i = 0; i < numVertices; i++) {
			for (int j = 0; j < numVertices; j++) {
				if (i != j) {
					mEdgeIndexLookupTable.put(
						new MutableInteger(edgeCtr),
						new Pair(id.getVertex(i), id.getVertex(j)));
					edgeCtr++;
				}
			}
		}

	}

    /**
     * Permutes the edges with default probability 1, meaning that if an edge is sample it will either be removed
     * or added depending on whether it exists already
     * @param graph the graph whose edges are to be permuted
     */
	public void permuteEdges(Graph graph) {
		permuteEdges(graph, 1.0);
	}

    /**
     * Permutes the edges using a user-specified probability that an edge is removed or added.
     * @param graph the graph whose edges are to be permuted
     * @param probEdgeFlip the probability that if a possible edge is sample it is removed, if it already exists
     * or added if it doesn't
     */
	public void permuteEdges(Graph graph, double probEdgeFlip) {
		if ((probEdgeFlip < 0) || (probEdgeFlip > 1)) {
			throw new IllegalArgumentException("Probability must be between 0 and 1.");
		}
		int numVertices = graph.numVertices();
		int numPossibleEdges = numVertices * numVertices - numVertices;
		if ((mNumEdgesToPermute < 0)
			|| (mNumEdgesToPermute > numPossibleEdges)) {
			throw new IllegalArgumentException("Number specified for number of edges to flip must be between 0 and n^2-n");
		}
		initialize(graph);

		RandomSampler randomSampler =
			new RandomSampler(
				mNumEdgesToPermute,
				numPossibleEdges,
				0,
				null);
		randomSampler.nextBlock(mNumEdgesToPermute, mPermuteEdgeSample, 0);
		//int currentEdgeSample = 0;
		Vertex sourceVertex = null;
		Vertex destVertex = null;
		MutableInteger currentKey = new MutableInteger();

		for (int i = 0; i < mNumEdgesToPermute; i++) {
			currentKey.setInteger((int) mPermuteEdgeSample[i]);
			Pair currentEdge = (Pair) mEdgeIndexLookupTable.get(currentKey);
			sourceVertex = (Vertex) currentEdge.getFirst();
			destVertex = (Vertex) currentEdge.getSecond();

			if (sourceVertex == destVertex) {
				continue;
			}

			if (Math.random() <= probEdgeFlip) {
				if (!sourceVertex.isPredecessorOf(destVertex)) {
                    if (PredicateUtils.enforcesUndirected(graph))
                        graph.addEdge(new UndirectedSparseEdge(sourceVertex, destVertex));
                    else // if either mixed or directed, create a directed edge
                        graph.addEdge(new DirectedSparseEdge(sourceVertex, destVertex));
//					GraphUtils.addEdge(graph, sourceVertex, destVertex);
				} else {
					Edge e = sourceVertex.findEdge(destVertex);
					graph.removeEdge(e);
				}
			}
		}

	}
}
