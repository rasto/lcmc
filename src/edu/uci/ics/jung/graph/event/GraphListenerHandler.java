/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Dec 29, 2003
 */
package edu.uci.ics.jung.graph.event;

import java.util.*;

import edu.uci.ics.jung.graph.*;

/**
 * This utility class handles Graph listening and call dispatching. Use it in
 * the appropriate ways.
 * 
 * @author danyelf
 */
public class GraphListenerHandler {

	private ArchetypeGraph mGraph;

	public GraphListenerHandler(ArchetypeGraph graph) {
		this.mGraphListenerMap = new HashMap();
		this.mGraph = graph;
	}

	Map mGraphListenerMap;

	/**
	 * @param gel
	 * @param get
	 */
	public void addListener(GraphEventListener gel, GraphEventType get) {
		if (get == GraphEventType.ALL_SINGLE_EVENTS) {
			addListener_internal(gel, GraphEventType.VERTEX_ADDITION);
			addListener_internal(gel, GraphEventType.VERTEX_REMOVAL);
			addListener_internal(gel, GraphEventType.EDGE_ADDITION);
			addListener_internal(gel, GraphEventType.EDGE_REMOVAL);
		} else {
			addListener_internal(gel, get);
		}
	}

	private void addListener_internal(
		GraphEventListener gel,
		GraphEventType get) {

		List listeners = (List) mGraphListenerMap.get(get);

		if (listeners == null) {
			listeners = new ArrayList();
			mGraphListenerMap.put(get, listeners);
		}

		listeners.add(gel);
	}

	/**
	 * @param gel
	 * @param get
	 */
	public void removeListener(GraphEventListener gel, GraphEventType get) {
		if (get == GraphEventType.ALL_SINGLE_EVENTS) {
			removeListener_internal(gel, GraphEventType.VERTEX_ADDITION);
			removeListener_internal(gel, GraphEventType.VERTEX_REMOVAL);
			removeListener_internal(gel, GraphEventType.EDGE_ADDITION);
			removeListener_internal(gel, GraphEventType.EDGE_REMOVAL);
		} else {
			removeListener_internal(gel, get);
		}
	}

	private void removeListener_internal(
		GraphEventListener gel,
		GraphEventType get) {
		List listeners = (List) mGraphListenerMap.get(get);
		if (listeners == null)
			return;

		listeners.remove(gel);
	}

	/**
	 * @param type a GraphEventType
	 * @return true if at least one listener is listening to this type
	 */
	public boolean listenersExist(GraphEventType type) {
		List listeners = (List) mGraphListenerMap.get(type);
		if ((listeners == null) || (listeners.size() == 0)) {
			return false;
		}
		return true;
	}

	protected void notifyListenersVertexAdded(GraphEvent ge) {
		List listeners =
			(List) mGraphListenerMap.get(GraphEventType.VERTEX_ADDITION);
		for (Iterator lIt = listeners.iterator(); lIt.hasNext();) {
			GraphEventListener listener = (GraphEventListener) lIt.next();
			listener.vertexAdded(ge);
		}
	}

	protected void notifyListenersEdgeAdded(GraphEvent ge) {
		List listeners =
			(List) mGraphListenerMap.get(GraphEventType.EDGE_ADDITION);
		for (Iterator lIt = listeners.iterator(); lIt.hasNext();) {
			GraphEventListener listener = (GraphEventListener) lIt.next();
			listener.edgeAdded(ge);
		}
	}

	protected void notifyListenersVertexRemoved(GraphEvent ge) {
		List listeners =
			(List) mGraphListenerMap.get(GraphEventType.VERTEX_REMOVAL);
		for (Iterator lIt = listeners.iterator(); lIt.hasNext();) {
			GraphEventListener listener = (GraphEventListener) lIt.next();
			listener.vertexRemoved(ge);
		}
	}

	protected void notifyListenersEdgeRemoved(GraphEvent ge) {
		List listeners =
			(List) mGraphListenerMap.get(GraphEventType.EDGE_REMOVAL);
		for (Iterator lIt = listeners.iterator(); lIt.hasNext();) {
			GraphEventListener listener = (GraphEventListener) lIt.next();
			listener.edgeRemoved(ge);
		}
	}

	/**
	 * @param e
	 */
	public void handleAdd(ArchetypeEdge e) {
		if (listenersExist(GraphEventType.EDGE_ADDITION)) {
			notifyListenersEdgeAdded(new GraphEvent(mGraph, e));
		}
	}

	/**
	 * @param v
	 */
	public void handleAdd(ArchetypeVertex v) {
		if (listenersExist(GraphEventType.VERTEX_ADDITION)) {
			notifyListenersVertexAdded(new GraphEvent(mGraph, v));
		}
	}

	/**
	 * @param v
	 */
	public void handleRemove(ArchetypeVertex v) {
		if (listenersExist(GraphEventType.VERTEX_REMOVAL)) {
			notifyListenersVertexRemoved(new GraphEvent(mGraph, v));
		}
	}

	/**
	 * @param e
	 */
	public void handleRemove(ArchetypeEdge e) {
		if (listenersExist(GraphEventType.EDGE_REMOVAL)) {
			notifyListenersEdgeRemoved(new GraphEvent(mGraph, e));
		}
	}

}
