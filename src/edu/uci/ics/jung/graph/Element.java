/*
 * Created on Mar 22, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph;

import java.util.Set;

import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * An interface for elements (vertices and edges) of generalized graphs.
 * Allows code to be written that applies to both vertices and edges, when their
 * structural role in a graph is not relevant (such as decorations).
 * 
 * @author Joshua O'Madadhain
 */
public interface Element extends UserDataContainer
{
    /**
     * Returns a reference to the graph that contains this element.
     * If this element is not contained by any graph (is an "orphaned" element),
     * returns null.
     */
    public ArchetypeGraph getGraph();
    
    /**
     * Returns the set of elements that are incident to this element.
     * For a vertex this corresponds to returning the vertex's incident
     * edges; for an edge this corresponds to returning the edge's incident
     * vertices.
     */
    public Set getIncidentElements();
}
