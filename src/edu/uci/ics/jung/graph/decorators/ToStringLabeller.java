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
 * Created on Apr 13, 2004
 */
package edu.uci.ics.jung.graph.decorators;

import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserData;

/**
 * Labels vertices by their toString. This class functions as a drop-in
 * replacement for the default StringLabeller method. This class does not
 * guarantee unique labels; or even consistent ones; as a result,
 * getVertexByLabel will always return NULL.
 * 
 * @author danyelf
 */
public class ToStringLabeller extends StringLabeller {

	/**
	 * This method is not meaningful; it throws an IllegalArgumentException
	 */
	public void assignDefaultLabels(Set vertices, int offset)
    {
		throw new IllegalArgumentException();
	}
	/**
	 * This method is not meaningful; it throws an IllegalArgumentException
	 */
	public Vertex removeLabel(String string) {
		throw new IllegalArgumentException();
	}
    /**
     * @param g
     */
    protected ToStringLabeller(Graph g) {
        super(g);
        labelToVertex = null;
        vertexToLabel = null;
        // TODO Auto-generated constructor stub for ToStringLabeller
    }

    /**
     * Sets the StringLabeller of this graph, at this key, to be a
     * ToStringLabeller.
     */
    public static StringLabeller setLabellerTo(Graph g, Object key) {
        StringLabeller sl = new ToStringLabeller(g);
        if (key != null) g.addUserDatum(key, sl, UserData.REMOVE);
        return sl;
    }

    /**
     * Sets the default StringLabeller of this graph to be a ToStringLabeller.
     */
    public static StringLabeller setLabellerTo(Graph g) {
        return setLabellerTo(g, StringLabeller.DEFAULT_STRING_LABELER_KEY);
    }

    /**
     * Retunrs v.toString()
     */
    public String getLabel(ArchetypeVertex v) {
        return v.toString();
    }

    /**
     * Always returns null: this impl doesn't keep a table, and so can't
     * meaningfully address this.
     */
    public Vertex getVertex(String label) {
        return null;
    }

    /**
     * This method always throws an IllegalArgument exception: you cannot
     * externally set the setstring method.
     */
    public void setLabel(Vertex v, String l) throws UniqueLabelException {
        throw new IllegalArgumentException(
                "Can't manually set labels on a ToString labeller");
    }
}