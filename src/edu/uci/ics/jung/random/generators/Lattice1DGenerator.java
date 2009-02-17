/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Jan 8, 2004
 *
 */
package edu.uci.ics.jung.random.generators;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * Simple generator of an n x 1 lattice where each vertex
 * is incident with each of its 2 neighbors (except possibly 
 * for the vertices on the edge depending upon whether the lattice
 * is specified to be toroidal or not).
 * @author Scott White
 *
 */
public class Lattice1DGenerator implements GraphGenerator
{
	private int mNumNodes;
	private boolean mIsToroidal;

	/**
	 * Constructs an instance of the lattice generator
	 * @param numNodes # of nodes in the generated graph
	 * @param isToroidal whether the lattice wraps around or not
	 */
	public Lattice1DGenerator(int numNodes, boolean isToroidal)
	{
		if (numNodes < 1)
		{
			throw new IllegalArgumentException("Lattice size must be at least 1.");
		}

		mNumNodes = numNodes;
		mIsToroidal = isToroidal;
	}

	/* (non-Javadoc)
	 * @see edu.uci.ics.jung.random.generators.GraphGenerator#generateGraph()
	 */
	public ArchetypeGraph generateGraph()
	{
		Graph g = new UndirectedSparseGraph();
		GraphUtils.addVertices(g, mNumNodes);
		int upI = 0;//, downI = 0;
		Indexer id = Indexer.getIndexer(g);

		//create lattice structure
		for (int i = 0; i < mNumNodes; i++)
		{
			for (int s = 1; s <= 1; s++)
			{
				Vertex ithVertex = (Vertex) id.getVertex(i);
				upI = upIndex(i, s);
				if (i != mNumNodes - 1 || (i == mNumNodes - 1 && mIsToroidal))
				{
					GraphUtils.addEdge(
						g,
						ithVertex,
						(Vertex) id.getVertex(upI));
				}
			}
		}

		return g;
	}

	/**
		 * Determines the vertices with a smaller index that are in the neighborhood of currentIndex.
		 * @param numSteps indicates the number of steps away from the current index that are being considered.
		 * @param currentIndex the index of the selected vertex.
		 */
	protected int downIndex(int currentIndex, int numSteps)
	{
		if (currentIndex - numSteps < 0)
		{
			return (mNumNodes - 1) + (currentIndex - numSteps);
		}
		return currentIndex + numSteps;
	}

	/**
	 * Determines the index of the neighbor ksteps above
	 * @param numSteps is the number of steps away from the current index that is being considered.
	 * @param currentIndex the index of the selected vertex.
	 */
	protected int upIndex(int currentIndex, int numSteps)
	{
		if (currentIndex + numSteps > mNumNodes - 1)
		{
			return numSteps - (mNumNodes - currentIndex);
		}
		return currentIndex + numSteps;
	}

}
