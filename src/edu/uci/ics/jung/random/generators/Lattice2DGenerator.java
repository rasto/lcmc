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
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.graph.impl.DirectedSparseGraph;
import edu.uci.ics.jung.utils.GraphUtils;

/**
 * Simple generator of an n x n lattice where each vertex
 * is incident with each of its 4 neighbors (except possibly 
 * for the vertices on the edge depending upon whether the lattice
 * is specified to be toroidal or not).
 * @author Scott
 *
 */
public class Lattice2DGenerator implements GraphGenerator
{
	private int mLatticeSize;
	private boolean mIsToroidal;

	/**
	 * Constructs an instance of the lattice generator
	 * @param latticeSize the size of the lattice, n, thus creating an n x n lattice.
	 * @param isToroidal whether the lattice wraps around or not
	 */
	public Lattice2DGenerator(int latticeSize, boolean isToroidal)
	{
		if (latticeSize < 2)
		{
			throw new IllegalArgumentException("Lattice size must be at least 2.");
		}

		mLatticeSize = latticeSize;
		mIsToroidal = isToroidal;

	}

	/* (non-Javadoc)
	 * @see edu.uci.ics.jung.random.generators.GraphGenerator#generateGraph()
	 */
	public ArchetypeGraph generateGraph()
	{
		int numNodes = (int) Math.pow(mLatticeSize, 2);
		DirectedSparseGraph graph = new DirectedSparseGraph();
		GraphUtils.addVertices(graph, numNodes);

		int currentLatticeRow = 0, currentLatticeColumn = 0;
		int upIndex = 0, downIndex = 0, leftIndex = 0, rightIndex = 0;

		Indexer id = Indexer.getIndexer(graph);

		for (int i = 0; i < numNodes; i++)
		{
			currentLatticeRow = i / mLatticeSize;
			currentLatticeColumn = i % mLatticeSize;

			upIndex = upIndex(currentLatticeRow, currentLatticeColumn);
			leftIndex = leftIndex(currentLatticeRow, currentLatticeColumn);
			downIndex = downIndex(currentLatticeRow, currentLatticeColumn);
			rightIndex = rightIndex(currentLatticeRow, currentLatticeColumn);

			//Add short range connections
			if (currentLatticeRow != 0
				|| (currentLatticeRow == 0 && mIsToroidal))
			{
				GraphUtils.addEdge(
					graph,
					(Vertex) id.getVertex(i),
					(Vertex) id.getVertex(upIndex));
			}
			if (currentLatticeColumn != 0
				|| (currentLatticeColumn == 0 && mIsToroidal))
			{
				GraphUtils.addEdge(
					graph,
					(Vertex) id.getVertex(i),
					(Vertex) id.getVertex(leftIndex));
			}
			if (currentLatticeRow != mLatticeSize-1
				|| (currentLatticeRow == mLatticeSize-1 && mIsToroidal))
			{
				GraphUtils.addEdge(
					graph,
					(Vertex) id.getVertex(i),
					(Vertex) id.getVertex(downIndex));
			}
			if (currentLatticeColumn != mLatticeSize-1
				|| (currentLatticeColumn == mLatticeSize-1 && mIsToroidal))
			{
				GraphUtils.addEdge(
					graph,
					(Vertex) id.getVertex(i),
					(Vertex) id.getVertex(rightIndex));
			}
		}

		return graph;
	}

	protected int upIndex(int currentLatticeRow, int currentLatticeColumn)
	{
		if (currentLatticeRow == 0)
		{
			return mLatticeSize * (mLatticeSize - 1) + currentLatticeColumn;
		}
		else
		{
			return (currentLatticeRow - 1) * mLatticeSize
				+ currentLatticeColumn;
		}
	}

	protected int downIndex(int currentLatticeRow, int currentLatticeColumn)
	{
		if (currentLatticeRow == mLatticeSize - 1)
		{
			return currentLatticeColumn;
		}
		else
		{
			return (currentLatticeRow + 1) * mLatticeSize
				+ currentLatticeColumn;
		}
	}

	protected int leftIndex(int currentLatticeRow, int currentLatticeColumn)
	{
		if (currentLatticeColumn == 0)
		{
			return currentLatticeRow * mLatticeSize + mLatticeSize - 1;
		}
		else
		{
			return currentLatticeRow * mLatticeSize + currentLatticeColumn - 1;
		}
	}

	protected int rightIndex(int currentLatticeRow, int currentLatticeColumn)
	{
		if (currentLatticeColumn == mLatticeSize - 1)
		{
			return currentLatticeRow * mLatticeSize;
		}
		else
		{
			return currentLatticeRow * mLatticeSize + currentLatticeColumn + 1;
		}
	}

	/**
	 * @return the size of the lattice, as specified by the constructor
	 */
	public int getLatticeSize()
	{
		return mLatticeSize;
	}

}
