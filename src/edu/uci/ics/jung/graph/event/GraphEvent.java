/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.event;

import java.util.EventObject;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Element;

/**
 *  An event which indicates that a change in the graph structure has occurred. Every
 * graph listener when notified of a change is passed a GraphEvent which contains a
 * reference to the object (Node, Edge, or EventSequence) that was involved in the
 * change as well as the graph whose structure was changed.
 * @author Scott White
 */
public class GraphEvent extends EventObject {
    private Element mGraphElement;

    public GraphEvent(ArchetypeGraph g, Element graphElement) {
        super(g);
        mGraphElement = graphElement;
    }

    public Element getGraphElement() {
        return mGraphElement;
    }

    public ArchetypeGraph getGraph() {
        return (ArchetypeGraph) getSource();
    }

    public String toString() {
    	String geType = null;
		if (mGraphElement instanceof ArchetypeVertex) {
			geType = "vertex";
	   } else if (mGraphElement instanceof ArchetypeEdge) {
			geType = "edge";
	   }
    	return "Graph Element type: " + geType;
    }
}
