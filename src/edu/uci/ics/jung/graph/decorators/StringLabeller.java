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
 * Created on Jun 13, 2003
 *
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserData;

/**
 * 
 * A StringLabeller applies a set of labels to a Graph. The Labeller,
 * specifically, attaches itself to a Graph's UserData, and maintains an index
 * of Strings that are labels. Note that the strings must be unique so that
 * getVertex( label ) will work.
 * 
 * @author danyelf
 *  
 */
public class StringLabeller implements VertexStringer {

	/**
	 * The key that hasLabeller() and getLabeller() use.
	 */
	public static final Object DEFAULT_STRING_LABELER_KEY = "StringLabeller.LabelDefaultKey";
	protected Map labelToVertex = new HashMap();
	protected Map vertexToLabel = new HashMap();
	protected Graph graph;
//    protected WeakReference graph;

	/**
	 * @param g
	 *            The graph to which this labeller should attach itself
	 */
	protected StringLabeller(Graph g) {
		this.graph = g;
//        this.graph = new WeakReference(g);
	}

	/**
	 * Gets a labeller associated with this graph. If no Labeller is associated,
	 * creates one and returns it. This method is the same as getLabeller with a
	 * key argument, but uses the DEFAULT_STRING_LABELER_KEY as its UserData
	 * key.
	 * 
	 * param g The Graph to check.
	 */
	public static StringLabeller getLabeller(Graph g) {
		return getLabeller(g, DEFAULT_STRING_LABELER_KEY);
	}

	/**
	 * Checks if a labeller is associated with this graph.
	 * 
	 * @param g
	 *            The graph to check.
	 */
	public static boolean hasStringLabeller(Graph g) {
		return hasStringLabeller(g, DEFAULT_STRING_LABELER_KEY);
	}

	/**
	 * Checks for a labeller attached to a particular key in the graph. Useful
	 * for creating more than one Labeller for a particular Graph.
	 * 
	 * @param g
	 *            the Graph
	 * @param key
	 *            the UserData key to which it is attached
	 * @return true if the graph has this labeller.
	 */
	public static boolean hasStringLabeller(Graph g, Object key) {
		StringLabeller id = (StringLabeller) g.getUserDatum(key);
		return (id != null);
	}

	/**
	 * Returns a labeller attached to a particular key in the graph. Useful for
	 * creating more than one Labeller for a particular Graph.
	 * 
	 * @param g
	 *            the Graph
	 * @param key
	 *            the UserData key to which it is attached
	 * @return a StringLabeller
	 */
	public static StringLabeller getLabeller(Graph g, Object key) {
		StringLabeller id = (StringLabeller) g.getUserDatum(key);
		if (id != null)
			return id;
		id = new StringLabeller(g);
		g.addUserDatum(key, id, UserData.REMOVE);
		return id;
	}

	/**
	 * Gets the graph associated with this StringLabeller
	 * 
	 * @return a Graph that uses this StringLabeller.
	 */
	public Graph getGraph() {
		return graph;
//        return (Graph)graph.get();
	}

	/**
	 * Gets the String label associated with a particular Vertex.
	 * 
	 * @param v
	 *            a Vertex inside the Graph.
	 * @throws FatalException
	 *             if the Vertex is not in the Graph associated with this
	 *             Labeller.
	 */
	public String getLabel(ArchetypeVertex v) {
		if (getGraph().getVertices().contains(v)) {
			return (String) vertexToLabel.get(v);
		} else
			throw new FatalException("Vertex not in my graph!");
	}

	/**
	 * Gets the Vertex from the graph associated with this label.
	 * 
	 * @param label
	 */
	public Vertex getVertex(String label) {
		return (Vertex) labelToVertex.get(label);
	}

	/**
	 * Associates a Vertex with a Label, overrwriting any previous labels on
	 * this vertex.
	 * 
	 * @param v
	 *            a Vertex in the labeller's graph
	 * @param l
	 *            a Label to be associated with this vertex
	 * @throws FatalException
	 *             thrown if this vertex isn't in the Labeller's graph
	 * @throws UniqueLabelException
	 *             thrown if this label is already associated with some other
	 *             vertex.
	 */
	public void setLabel(Vertex v, String l) throws UniqueLabelException {

		if (v.getGraph() == graph) {
			if (labelToVertex.containsKey(l)) {
				// we already have a vertex with this label
				throw new UniqueLabelException(l + " is already on vertex "
						+ labelToVertex.get(l));
			}
			// ok, we know we don't have this label anywhere yet
			if (vertexToLabel.containsKey(v)) {
				Object junk = vertexToLabel.get(v);
				labelToVertex.remove(junk);
			}
			vertexToLabel.put(v, l);
			labelToVertex.put(l, v);
		} else {
			// throw some sort of exception here
			throw new FatalException("This vertex is not a part of this graph");
		}

	}

	/**
	 * Assigns textual labels to every vertex passed in. Walks through the graph
	 * in iterator order, assigning labels "offset", "offset+1" "offset+2". The
	 * count starts at offset.
	 * 
	 * @param vertices
	 *            The set of Vertices to label. All must be part of this graph.
	 * @param offset
	 *            The starting value to number vertices from
	 * @throws UniqueLabelException
	 *             Is thrown if some other vertexc is already numbered.
	 * @throws FatalException
	 *             if any Vertex is not part of the Graph.
	 */
	public void assignDefaultLabels(Set vertices, int offset)
			throws UniqueLabelException {
		int labelIdx = offset;
		for (Iterator udcIt = vertices.iterator(); udcIt.hasNext();) {
			Vertex v = (Vertex) udcIt.next();
			String label = String.valueOf(labelIdx);
			setLabel(v, label);
			labelIdx++;
		}
	}

	/**
	 * A minor class to store exceptions from duplicate labels in the Graph.
	 * 
	 * @author danyelf
	 */
	public static class UniqueLabelException extends Exception {

		public UniqueLabelException(String string) {
			super(string);
		}

	}

	/**
	 * @param string
	 */
	public Vertex removeLabel(String string) {
		if (labelToVertex.containsKey(string)) {
			Vertex v = (Vertex) labelToVertex.get(string);
			labelToVertex.remove(string);
			vertexToLabel.remove(v);
			return v;
		} else {
			return null;
		}

	}

	/**
	 * Wipes the entire table. Resets everything.
	 */
	public void clear() {
		vertexToLabel.clear();
		labelToVertex.clear();
	}

}