/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.flows;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;

import edu.uci.ics.jung.algorithms.IterativeProcess;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.MutableInteger;
import edu.uci.ics.jung.utils.UserData;

/**
 * Implements the EdmondsKarpMaxFlow algorithm for solving the maximum flow problem. After the algorithm is executed,
 * each edge is labeled with a MutableDouble value that indicates the flow along that edge.
 * <p>
 * The algorithm operates on the assumption that the user has provided a UserDatum value (with copy action either 
 * SHARED or CLONE, but not REMOVE) of type Number along
 * each edge indicating the capacity available.
 * <p>
 * An example of using this algorithm is as follows:
 * <pre>
 * EdmondsKarpMaxFlow ek = new EdmondsKarpMaxFlow(graph,sourceVertex,"CAPACITY","FLOW");
 * ek.evaluate(); // This actually instructs the solver to compute the max flow
 * </pre>
 *
 * @see "Introduction to Algorithms by Cormen, Leiserson, Rivest, and Stein."
 * @see "Network Flows by Ahuja, Magnanti, and Orlin."
 * @see "Theoretical improvements in algorithmic efficiency for network flow problems by Edmonds and Karp, 1972."
 * @author Scott White
 */
public class EdmondsKarpMaxFlow extends IterativeProcess{
    private static String RESIDUAL_CAPACITY_KEY = "jung.algorithms.flows.ResidualCapacity";
    private static String PARENT_KEY = "jung.algorithms.flows.Parent";
    //represents the best capacity a node has seen so far
    private static String PARENT_CAPACITY_KEY = "jung.algorithms.flows.ParentCapacity";
    private DirectedGraph mFlowGraph;
    private DirectedGraph mOriginalGraph;
    private Vertex mSource;
    private Vertex mTarget;
    private String mEdgeFlowKey;
    private String mEdgeCapacityKey;
    private int mMaxFlow;
    private Set mSourcePartitionNodes;
    private Set mSinkPartitionNodes;
    private Set mMinCutEdges;

    /**
     * Constructs a new instance of the algorithm solver for a given graph, source, and sink.
     * Source and sink vertices must be elements of the specified graph, and must be 
     * distinct.
     * @param directedGraph the flow graph
     * @param source the source vertex
     * @param sink the sink vertex
     * @param edgeCapacityKey the UserDatum key that stores the capacity for each edge.
     * @param edgeFlowKey the UserDatum key where the solver will place the value of the flow for each edge
     */
    public EdmondsKarpMaxFlow(DirectedGraph directedGraph, Vertex source, Vertex sink, String edgeCapacityKey, String edgeFlowKey) 
    {
        if (source.getGraph() != directedGraph || sink.getGraph() != directedGraph)
            throw new IllegalArgumentException("source and sink vertices must be elements of the specified graph");
        
        if (source.equals(sink))
            throw new IllegalArgumentException("source and sink vertices must be distinct");
        
        mOriginalGraph = directedGraph;
        mFlowGraph = (DirectedGraph) directedGraph.copy();
        mSource = (Vertex) source.getEqualVertex(mFlowGraph);
        mTarget = (Vertex) sink.getEqualVertex(mFlowGraph);
        mEdgeFlowKey = edgeFlowKey;
        mEdgeCapacityKey = edgeCapacityKey;
        mMaxFlow = 0;
        mSinkPartitionNodes = new HashSet();
        mSourcePartitionNodes = new HashSet();
        mMinCutEdges = new HashSet();
    }

    private void clearParentValues() {
        for (Iterator vIt=mFlowGraph.getVertices().iterator();vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            currentVertex.removeUserDatum(PARENT_CAPACITY_KEY);
            currentVertex.removeUserDatum(PARENT_KEY);
        }
        mSource.setUserDatum(PARENT_CAPACITY_KEY,new MutableInteger(Integer.MAX_VALUE),UserData.SHARED);
        mSource.setUserDatum(PARENT_KEY,mSource,UserData.SHARED);

    }

    protected boolean hasAugmentingPath() {

        mSinkPartitionNodes.clear();
        mSourcePartitionNodes.clear();
        for (Iterator vIt=mFlowGraph.getVertices().iterator();vIt.hasNext();) {
            Vertex v = (Vertex) vIt.next();
            mSinkPartitionNodes.add(v);
        }
        Set visitedEdgesMap = new HashSet();
        Buffer queue = new UnboundedFifoBuffer();
        queue.add(mSource);

        while (!queue.isEmpty()) {
            Vertex currentVertex = (Vertex) queue.remove();
            mSinkPartitionNodes.remove(currentVertex);
            mSourcePartitionNodes.add(currentVertex);
            MutableInteger currentCapacity = (MutableInteger) currentVertex.getUserDatum(PARENT_CAPACITY_KEY);

            Set neighboringEdges = currentVertex.getOutEdges();

            for (Iterator neIt = neighboringEdges.iterator(); neIt.hasNext();) {
                DirectedEdge neighboringEdge  = (DirectedEdge) neIt.next();
                Vertex neighboringVertex = neighboringEdge.getDest();

                MutableInteger residualCapacity = (MutableInteger) neighboringEdge.getUserDatum(RESIDUAL_CAPACITY_KEY);
                if (residualCapacity.intValue() <= 0 || visitedEdgesMap.contains(neighboringEdge))
                    continue;

                Vertex neighborsParent = (Vertex) neighboringVertex.getUserDatum(PARENT_KEY);
                MutableInteger neighborCapacity = (MutableInteger) neighboringVertex.getUserDatum(PARENT_CAPACITY_KEY);
                int newCapacity = Math.min(residualCapacity.intValue(),currentCapacity.intValue());

                if ((neighborsParent == null) || newCapacity > neighborCapacity.intValue()) {
                    neighboringVertex.setUserDatum(PARENT_KEY,currentVertex,UserData.SHARED);
                    neighboringVertex.setUserDatum(PARENT_CAPACITY_KEY,new MutableInteger(newCapacity),UserData.SHARED);
                    visitedEdgesMap.add(neighboringEdge);
                    if (neighboringVertex != mTarget) {
                       queue.add(neighboringVertex);
                    }
                }
            }
        }

        boolean hasAugmentingPath = false;
        MutableInteger targetsParentCapacity = (MutableInteger) mTarget.getUserDatum(PARENT_CAPACITY_KEY);
        if (targetsParentCapacity != null && targetsParentCapacity.intValue() > 0) {
            updateResidualCapacities();
            hasAugmentingPath = true;
        }
        clearParentValues();
        return hasAugmentingPath;


    }

     protected double evaluateIteration() {
        while (hasAugmentingPath()) {
        }

        computeMinCut();
        return 0;
    }

    private void computeMinCut() {

        for (Iterator eIt=mOriginalGraph.getEdges().iterator();eIt.hasNext();) {
            DirectedEdge e = (DirectedEdge) eIt.next();
            if (mSinkPartitionNodes.contains(e.getSource()) && mSinkPartitionNodes.contains(e.getDest())) {
                continue;
            }
            if (mSourcePartitionNodes.contains(e.getSource()) && mSourcePartitionNodes.contains(e.getDest())) {
                continue;
            }
            if (mSinkPartitionNodes.contains(e.getSource()) && mSourcePartitionNodes.contains(e.getDest())) {
                continue;
            }
            mMinCutEdges.add(e);
        }

    }

    /**
     * Returns the max flow value
     * @return int the value of the maximum flow from the source to the sink
     */
    public int getMaxFlow() {
        return mMaxFlow;
    }

    /**
     * Retrieves the nodes lying on the side of the partition (partitioned using the
     * min-cut) of the sink node
     * @return the set of nodes in the sink partition class
     */
    public Set getNodesInSinkPartition() {
        return mSinkPartitionNodes;
    }

    /**
     * Retrieves the nodes lying on the side of the partition (partitioned using the
     * min-cut) of the source node
     * @return the set of nodes in the source partition class
     */
    public Set getNodesInSourcePartition() {
        return mSourcePartitionNodes;
    }

    /**
     * Retrieve the min-cut edge set
     * @return set of edges in the min cut set
     */
    public Set getMinCutEdges() {
        return mMinCutEdges;

    }

    /**
     * Retrieves the flow graph used to compute the max flow
     * @return a copy of the flow graph
     */
    public DirectedGraph getFlowGraph() {
        return (DirectedGraph) mFlowGraph.copy();
    }

    protected void initializeIterations() {
        mSource.setUserDatum(PARENT_CAPACITY_KEY,new MutableInteger(Integer.MAX_VALUE),UserData.SHARED);
        mSource.setUserDatum(PARENT_KEY,mSource,UserData.SHARED);

        List edgeList = new ArrayList();
        edgeList.addAll(mFlowGraph.getEdges());

        for (int eIdx=0;eIdx< edgeList.size();eIdx++) {
            DirectedEdge edge = (DirectedEdge) edgeList.get(eIdx);
            Number capacity = (Number) edge.getUserDatum(mEdgeCapacityKey);
            if (capacity == null) {
                throw new IllegalArgumentException("Edge capacities must be decorated using key: " + mEdgeCapacityKey);
            }
            edge.setUserDatum(RESIDUAL_CAPACITY_KEY,new MutableInteger(capacity.intValue()),UserData.SHARED);

            if (!edge.getDest().isPredecessorOf(edge.getSource())) {
                DirectedEdge backEdge = (DirectedEdge) GraphUtils.addEdge(mFlowGraph, edge.getDest(),edge.getSource());
                backEdge.setUserDatum(RESIDUAL_CAPACITY_KEY,new MutableInteger(0),UserData.SHARED);
            }
        }
    }

    protected void finalizeIterations() {

        for (Iterator eIt=mFlowGraph.getEdges().iterator();eIt.hasNext();) {
            DirectedEdge currentEdge = (DirectedEdge) eIt.next();

            Number capacity = (Number) currentEdge.getUserDatum(mEdgeCapacityKey);
            Number residualCapacity = (Number) currentEdge.getUserDatum(RESIDUAL_CAPACITY_KEY);
            if (capacity != null) {
                MutableInteger flowValue = new MutableInteger(capacity.intValue()-residualCapacity.intValue());
                currentEdge.setUserDatum(mEdgeFlowKey,flowValue,UserData.SHARED);
            }
        }

        Set backEdges = new HashSet();
        for (Iterator eIt=mFlowGraph.getEdges().iterator();eIt.hasNext();) {
            DirectedEdge currentEdge = (DirectedEdge) eIt.next();
            if (currentEdge.getUserDatum(mEdgeCapacityKey) == null) {
                backEdges.add(currentEdge);
            } else
                currentEdge.removeUserDatum(RESIDUAL_CAPACITY_KEY);
        }

        GraphUtils.removeEdges(mFlowGraph, backEdges);
    }

    private void updateResidualCapacities() {

        MutableInteger augmentingPathCapacity = (MutableInteger) mTarget.getUserDatum(PARENT_CAPACITY_KEY);
        mMaxFlow += augmentingPathCapacity.intValue();
        Vertex currentVertex = mTarget;
        Vertex parentVertex = null;
        while ((parentVertex = (Vertex) currentVertex.getUserDatum(PARENT_KEY)) != currentVertex) {
            DirectedEdge currentEdge = (DirectedEdge) parentVertex.findEdge(currentVertex);
            MutableInteger residualCapacity = (MutableInteger) currentEdge.getUserDatum(RESIDUAL_CAPACITY_KEY);
            residualCapacity.subtract(augmentingPathCapacity.intValue());

            DirectedEdge backEdge = (DirectedEdge) currentVertex.findEdge(parentVertex);
            residualCapacity = (MutableInteger) backEdge.getUserDatum(RESIDUAL_CAPACITY_KEY);
            residualCapacity.add(augmentingPathCapacity.intValue());
            currentVertex = parentVertex;
        }
    }
}
