/*
 * Created on Apr 9, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.util.EventListener;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * An interface for classes that listen for the selection
 * of vertices or edges, usually via user interaction.
 * @deprecated use ItemListener for tracking pick changes
 * @author Joshua O'Madadhain
 */
public interface PickEventListener extends EventListener
{
    public void vertexPicked(ArchetypeVertex v);
    public void vertexUnpicked(ArchetypeVertex v);
    public void edgePicked(ArchetypeEdge v);
    public void edgeUnpicked(ArchetypeEdge v);
}
