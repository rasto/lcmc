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
 * Created on Mar 19, 2005
 *
 */
package edu.uci.ics.jung.visualization;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;


/**
 * Simple implementation of PickSupport that returns the vertex or edge
 * that is closest to the specified location.  This implementation
 * provides the same picking options that were available in
 * previous versions of AbstractLayout.
 * 
 * @author Tom Nelson
 * @author Joshua O'Madadhain
 */
public class RadiusPickSupport extends RadiusGraphElementAccessor implements PickSupport {
    
    protected HasGraphLayout hasGraphLayout;
    
    public RadiusPickSupport(HasGraphLayout hasGraphLayout, double maxDistance) {
        this(maxDistance);
        this.hasGraphLayout = hasGraphLayout;
    }
    
    public RadiusPickSupport() {
        this(Math.sqrt(Double.MAX_VALUE - 1000));
    }
    
    /**
     * the layout will always be provided by the VisualizationViewer
     * this is supporting picking for
     * @param maxDistance
     */
    public RadiusPickSupport(double maxDistance) {
        super(null, maxDistance);
    }
    
    /**
     * called by VisualizationViewer when this PickSupport impl is
     * added to VisualizationViewer. This allows the PickSupport to
     * always get the current Layout from the VisualizationViewer it
     * supports picking on.
     */
    public void setHasGraphLayout(HasGraphLayout hasGraphLayout) {
        this.hasGraphLayout = hasGraphLayout;
    }
    
	/**
	 * Gets the vertex nearest to the location of the (x,y) location selected,
	 * within a distance of <tt>maxDistance</tt>. Iterates through all
	 * visible vertices and checks their distance from the click. Override this
	 * method to provde a more efficient implementation.
	 */
	public Vertex getVertex(double x, double y) {
	    return getVertex(x, y, this.maxDistance);
	}

	/**
	 * Gets the vertex nearest to the location of the (x,y) location selected,
	 * within a distance of <tt>maxDistance</tt>. Iterates through all
	 * visible vertices and checks their distance from the click. Override this
	 * method to provde a more efficient implementation.
	 * @param x
	 * @param y
	 * @param maxDistance temporarily overrides member maxDistance
	 */
	public Vertex getVertex(double x, double y, double maxDistance) {
	    // if vv is set, use it to get the most current layout
	    if(hasGraphLayout != null) {
	        layout = hasGraphLayout.getGraphLayout();
	    }
	    return super.getVertex(x, y, maxDistance);
	}
	
	/**
	 * Gets the edge nearest to the location of the (x,y) location selected.
	 * Calls the longer form of the call.
	 */
	public Edge getEdge(double x, double y) {
	    return getEdge(x, y, this.maxDistance);
	}

	/**
	 * Gets the edge nearest to the location of the (x,y) location selected,
	 * within a distance of <tt>maxDistance</tt>, Iterates through all
	 * visible edges and checks their distance from the click. Override this
	 * method to provide a more efficient implementation.
	 * 
	 * @param x
	 * @param y
	 * @param maxDistance temporarily overrides member maxDistance
	 * @return Edge closest to the click.
	 */
	public Edge getEdge(double x, double y, double maxDistance) {
	    // if vv is set, use it to get the most current layout
	    if(hasGraphLayout != null) {
	        layout = hasGraphLayout.getGraphLayout();
	    }
	    return super.getEdge(x, y, maxDistance);
	}
}
