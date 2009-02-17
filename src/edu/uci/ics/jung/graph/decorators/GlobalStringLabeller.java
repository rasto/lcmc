/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
/*
 * Created on Jun 13, 2003
 *  
 */
package edu.uci.ics.jung.graph.decorators;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserData;

/**
 * 
 * The GlobalStringLabeller applies labels to all vertices in a series of
 * graphs. That is, rather than storing one instance per graph, it stores one
 * instance globally that maps from vertex ID to label.
 * 
 * @author danyelf
 *  
 */

public class GlobalStringLabeller extends StringLabeller {

    protected static GlobalStringLabeller instance;

    protected GlobalStringLabeller() {
        super(null);
    }

    /**
     * Sets the StringLabeller of this graph, at this key, to be a
     * ToStringLabeller.
     */
    public static StringLabeller setLabellerTo(Graph g, Object key) {
        StringLabeller sl = GlobalStringLabeller.getInstance();
        if (key != null) g.addUserDatum(key, sl, UserData.REMOVE);
        return sl;
    }

    /**
     * @return a <code>GlobalStringLabeller</code> instance
     */
    public synchronized static StringLabeller getInstance() {
        if (instance == null) {
            instance = new GlobalStringLabeller();
        }
        return instance;
    }
    
    public static StringLabeller getLabeller( Graph g ) {
        return setLabellerTo(g);
    }

    /**
     * Sets the default StringLabeller of this graph to be a ToStringLabeller.
     */
    public static StringLabeller setLabellerTo(Graph g) {
        return setLabellerTo(g, StringLabeller.DEFAULT_STRING_LABELER_KEY);
    }

    /**
     * Checks if a labeller--any labeller--is associated with this graph.
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
        GlobalStringLabeller id = (GlobalStringLabeller) g.getUserDatum(key);
        return (id != null);
    }

    /**
     * Gets the String label associated with a particular Vertex.
     * 
     * @param v a Vertex
     * @return the label associated with that vertex
     * If the vertex is not in the graph, throws a FatalException 
     */
    public String getLabel(ArchetypeVertex v) {
        if (vertexToLabel.containsKey(v)) {
            return (String) vertexToLabel.get(v);
        } else
            throw new FatalException("Vertex not registered in GlobalStringLabeller!");
    }

    /**
     * Associates a Vertex with a Label, overrwriting any previous labels on
     * this vertex or vertices equal to it.
     * 
     * @param v
     *            a Vertex 
     * @param l
     *            a Label to be associated with this vertex
     * @throws UniqueLabelException
     *             thrown if this label is already associated with some other
     *             vertex.
     */
    public void setLabel(Vertex v, String l) throws UniqueLabelException {

        if (labelToVertex.containsKey(l)) {
        // we already have a vertex with this label
        throw new UniqueLabelException(l + " is already on vertex "
                + labelToVertex.get(l)); }
        // ok, we know we don't have this label anywhere yet
        if (vertexToLabel.containsKey(v)) {
            Object junk = vertexToLabel.get(v);
            labelToVertex.remove(junk);
        }
        vertexToLabel.put(v, l);
        labelToVertex.put(l, v);

    }
}