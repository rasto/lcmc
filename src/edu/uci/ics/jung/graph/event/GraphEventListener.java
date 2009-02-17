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

import java.util.EventListener;

/**
 * The interface for "listening" to graph events (changes in the structure of a graph).
 * To be notified of graph events, a class must implement this interface and
 * call addListener(...) [or beginEventSequence()] on the appropriate graph.
 * Notification happens by way of the appropriate method in this interface being
 * called immediately after the event occurs.
 * @author Scott White
 */
public interface GraphEventListener extends EventListener {
    public void vertexAdded(GraphEvent event);
	public void vertexRemoved(GraphEvent event);
	public void edgeAdded(GraphEvent event);
	public void edgeRemoved(GraphEvent event);
}
