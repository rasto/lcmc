/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.algorithms.connectivity;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.decorators.NumericDecorator;
import edu.uci.ics.jung.utils.UserData;

import java.util.*;

/**
 * Labels each node in the graph according to the BFS distance from the start node(s). If nodes are unreachable, then
 * they are assigned a distance of -1.
 * All nodes traversed at step k are marked as predecessors of their successors traversed at step k+1.
 * <p>
 * Running time is: O(m)
 * @author Scott White
 */
public class BFSDistanceLabeler {
	public static final String DEFAULT_DISTANCE_KEY = "algorithms.connectivity.BFSDiststanceLabeler.DISTANCE_KEY";
    private NumericDecorator mDistanceDecorator;
    private List mCurrentList;
    private Set mUnvisitedVertices;
    private List mVerticesInOrderVisited;
    private Map mPredecessorMap;


    /**
     * Creates a new BFS labeler for the specified graph and root set.
     * @param distanceKey the UserDatum key the algorithm should use to store/decorate the distances from the root set
     * The distances are stored in the corresponding Vertex objects and are of type MutableInteger
     */
    public BFSDistanceLabeler(String distanceKey) {
        mDistanceDecorator = new NumericDecorator(distanceKey,UserData.SHARED);
        mPredecessorMap = new HashMap();
    }
    
	/**
	 * Creates a new BFS labeler for the specified graph and root set
	 * The distances are stored in the corresponding Vertex objects and are of type MutableInteger
	 */
	public BFSDistanceLabeler() {
		mDistanceDecorator = new NumericDecorator(DEFAULT_DISTANCE_KEY,UserData.SHARED);
		mPredecessorMap = new HashMap();
	}

    /**
     * Returns the list of vertices visited in order of traversal
     * @return the list of vertices
     */
    public List getVerticesInOrderVisited() {
        return mVerticesInOrderVisited;
    }

    /**
     * Returns the set of all vertices that were not visited
     * @return the list of unvisited vertices
     */
    public Set getUnivistedVertices() {
        return mUnvisitedVertices;
    }

    /**
     * Given a vertex, returns the shortest distance from any node in the root set to v
     * @param v the vertex whose distance is to be retrieved
     * @return the shortest distance from any node in the root set to v
     */
    public int getDistance(Graph g, Vertex v) {
        if (!g.getVertices().contains(v)) {
            throw new IllegalArgumentException("Vertex is not contained in the graph.");
        }

        return mDistanceDecorator.getValue(v).intValue();
    }

    /**
     * Returns set of predecessors of the given vertex
     * @param v the vertex whose predecessors are to be retrieved
     * @return the set of predecessors
     */
    public Set getPredecessors(Vertex v) {
        return (Set) mPredecessorMap.get(v);
    }

    protected void initialize(Graph g, Set rootSet) {
        mVerticesInOrderVisited = new ArrayList();
        mUnvisitedVertices = new HashSet();
        for (Iterator vIt=g.getVertices().iterator(); vIt.hasNext();) {
            Vertex currentVertex = (Vertex) vIt.next();
            mUnvisitedVertices.add(currentVertex);
            mPredecessorMap.put(currentVertex,new HashSet());
        }

        mCurrentList = new ArrayList();
        for (Iterator rootIt = rootSet.iterator(); rootIt.hasNext();) {
            Vertex v = (Vertex) rootIt.next();
            mDistanceDecorator.setValue(new Integer(0),v);
            mCurrentList.add(v);
            mUnvisitedVertices.remove(v);
            mVerticesInOrderVisited.add(v);
        }
    }

    private void addPredecessor(Vertex predecessor,Vertex sucessor) {
        HashSet predecessors = (HashSet) mPredecessorMap.get(sucessor);
        predecessors.add(predecessor);
    }

    public void removeDecorations(Graph g) {
        for (Iterator vIt=g.getVertices().iterator();vIt.hasNext();) {
            Vertex v = (Vertex) vIt.next();
            mDistanceDecorator.removeValue(v);
        }
    }

    /**
     * Computes the distances of all the node from the starting root nodes. If there is more than one root node
     * the minimum distance from each root node is used as the designated distance to a given node. Also keeps track
     * of the predecessors of each node traversed as well as the order of nodes traversed.
     * @param graph the graph to label
     * @param rootSet the set of starting vertices to traverse from
     */
    public void labelDistances(Graph graph, Set rootSet) {

        initialize(graph,rootSet);

        int distance = 1;
        while (true) {
            List newList = new ArrayList();

           for (Iterator vIt = mCurrentList.iterator(); vIt.hasNext();) {
                Vertex currentVertex = (Vertex) vIt.next();
                for (Iterator uIt = currentVertex.getSuccessors().iterator(); uIt.hasNext();) {
                    visitNewVertex(currentVertex,(Vertex) uIt.next(), distance, newList);
                }
            }
            if (newList.size() == 0) break;
            mCurrentList = newList;
            distance++;
        }

        for (Iterator vIt = mUnvisitedVertices.iterator(); vIt.hasNext();) {
            Vertex v = (Vertex) vIt.next();
            mDistanceDecorator.setValue(new Integer(-1),v);

        }
    }

    /**
     * Computes the distances of all the node from the specified root node. Also keeps track
     * of the predecessors of each node traveresed as well as the order of nodes traversed.
     *  @param graph the graph to label
     * @param root the single starting vertex to traverse from
     */
    public void labelDistances(Graph graph, Vertex root) {
        Set rootSet = new HashSet();
        rootSet.add(root);
        labelDistances(graph,rootSet);

    }

    private void visitNewVertex(Vertex predecessor,Vertex neighbor, int distance, List newList) {
        if (mUnvisitedVertices.contains(neighbor)) {
            mDistanceDecorator.setValue(new Integer(distance),neighbor);
            newList.add(neighbor);
            mVerticesInOrderVisited.add(neighbor);
            mUnvisitedVertices.remove(neighbor);
        }
        int predecessorDistance = mDistanceDecorator.getValue(predecessor).intValue();
        int successorDistance = mDistanceDecorator.getValue(neighbor).intValue();
        if (predecessorDistance < successorDistance) {
            addPredecessor(predecessor,neighbor);
        }
    }
}
