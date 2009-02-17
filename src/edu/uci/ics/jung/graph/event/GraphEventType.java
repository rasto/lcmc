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

/**
 * This is an instance of the Enumerator pattern which creates 1st class static type objects
 * for each of the known graph events.
 * @author Scott White
 */
public interface GraphEventType {
	public VertexAddition VERTEX_ADDITION = new VertexAddition();
	public EdgeAddition EDGE_ADDITION = new EdgeAddition();
	public VertexRemoval VERTEX_REMOVAL = new VertexRemoval();
	public EdgeRemoval EDGE_REMOVAL = new EdgeRemoval();
	public AllSingleEvents ALL_SINGLE_EVENTS = new AllSingleEvents();

    static class VertexAddition implements GraphEventType {
    	public String toString() {
    		return "Vertex Addition";
    	}
    }
    static class VertexRemoval implements GraphEventType {
		public String toString() {
			return "Vertex Removal";
		}
    }
    static class EdgeAddition implements GraphEventType {
		public String toString() {
			return "Edge Addition";
		}
    }
    static class EdgeRemoval implements GraphEventType {
		public String toString() {
			return "Edge Removal";
		}
    }
	static class AllSingleEvents implements GraphEventType {
		public String toString() {
			return "All Single Events";
		}
	}
}
