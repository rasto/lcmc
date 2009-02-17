/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.utils;
import java.util.Iterator;
import edu.uci.ics.jung.algorithms.cluster.ClusterSet;
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
/**
 * @author Scott White
 *
 */
public class GraphProperties {
	
	/**
	 * Checks to see whether the graph is connected.
	 * @param g the graph
	 * @return Return true if yes, false if no
	 */
	public static boolean isConnected(Graph g) {
		WeakComponentClusterer wc = new WeakComponentClusterer();
		ClusterSet cs = wc.extract(g);
		return cs.size() == 1;
	}
	
	/**
	 * Checks to see whether the graphs is simple (that is, whether it contains
	 * parallel edges and self-loops).
	 * @param g the graph
	 * @return true if yes, false if no
	 */
	public static boolean isSimple(Graph g) {
		return !containsSelfLoops(g) && !containsParallelEdges(g);
	}
	
	/**
	 * Checks to see whether the graphs contains self-loops
	 * @param g the graph
	 * @return true if yes, false if no
	 */
	public static boolean containsSelfLoops(Graph g) {
		for (Iterator vIt = g.getVertices().iterator(); vIt.hasNext();) {
			Vertex v = (Vertex) vIt.next();
			if (v.findEdge(v) != null) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks to see whether the graphs contains parallel edges
	 * @param g the graph
	 * @return true if yes, false if no
	 */
	public static boolean containsParallelEdges(Graph g) {
		for (Iterator eIt = g.getEdges().iterator(); eIt.hasNext();) {
			Edge e = (Edge) eIt.next();
			Pair endpoints = e.getEndpoints();
			Vertex anEndPoint = (Vertex) endpoints.getFirst();
			Vertex anotherEndPoint = (Vertex) endpoints.getSecond();
			if (anEndPoint.findEdgeSet(anotherEndPoint).size() > 1) {
				return true;
			}
		}
		return false;
	}
}
