/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 * Created on Mar 8, 2005
 *
 */
package edu.uci.ics.jung.visualization;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * SimpleGraphMouse is the original GraphMouse class that was
 * nested in VisualizationViewer and installed as a listener
 * for mouse events and mouse motion events. Users can restore
 * previous capability by creating an instance of this class 
 * and using VisualizationViewer.setGraphMouse() to re-install it.
 * Changes in this code from the nested version are mainly those
 * necessary to move it outside the VisualizationViewer class,
 * and to properly handle the transform values of the VisualizationViewer
 * (the zoom and pan)
 *
 * 
 */
public class SimpleGraphMouse extends MouseAdapter
	implements VisualizationViewer.GraphMouse {
	
	/**
	 * the VisualizationViewer who's mouse events call these
	 * methods.
	 */
	protected VisualizationViewer vv;
	
	/**
	 * how far the mouse point x is from the vertex center x
	 */
	protected float offsetx;
	
	/**
	 * how far the mouse point y is from the vertex center y
	 */
	protected float offsety;
	
	/**
	 * the vertex to drag with a mouseDragged operation
	 */
	protected Vertex vertexToDrag;

	/**
	 * create an instance for the passed VisualizationViewer
	 * @param vv
	 */
	public SimpleGraphMouse(VisualizationViewer vv) {
	    this.vv = vv;
	}

	/**
	 * Uses the layout class to attempt to pick a vertex in
	 * the graph.
	 */
	public void mousePressed(MouseEvent e) {
	    PickSupport pickSupport = vv.getPickSupport();
	    PickedState pickedState = vv.getPickedState();
	    
	    if(pickSupport != null && pickedState != null) {
	        Layout layout = vv.getGraphLayout();
	        if(SwingUtilities.isLeftMouseButton(e)) {
	            // p is the screen point for the mouse event
	            Point2D p = e.getPoint();
	            // transform it to graph coordinates:
	            Point2D gp = vv.inverseTransform(p);

	            Vertex v = pickSupport.getVertex(gp.getX(), gp.getY());
	            if(v != null) {
	                pickedState.clearPickedVertices();
	                pickedState.pick(v, true);
	                vertexToDrag = v;
	                // layout.getLocation applies the layout transformer so
	                // q is transformed by the layout transformer only
	                Point2D q = layout.getLocation(v);
	                // i need to put it back in the graph coordinates:
	                Point2D gq = vv.getLayoutTransformer().inverseTransform(q);
	                offsetx = (float) (gp.getX()-gq.getX());
	                offsety = (float) (gp.getY()-gq.getY());
	            } else {
	                Edge edge = pickSupport.getEdge(gp.getX(), gp.getY());
	                if(edge != null) {
	                    pickedState.clearPickedEdges();
	                    pickedState.pick(edge, true);
	                }
	            }
	            vv.repaint();
	            
	        } else if(SwingUtilities.isMiddleMouseButton(e)) {
	            Point2D p = vv.inverseTransform(e.getPoint());
	            Vertex v = pickSupport.getVertex(p.getX(), p.getY());
	            if(v != null) {
                    boolean wasThere = pickedState.pick(v, !pickedState.isPicked(v));
	                if(wasThere) {
	                    vertexToDrag = null;
	                } else {
	                    vertexToDrag = v;
	                        Point2D point = vv.getLayoutTransformer().inverseTransform(layout.getLocation(v));
	                        offsetx = (float) (p.getX()-point.getX());
	                        offsety = (float) (p.getY()-point.getY());
//	                    }
	                }
	            } else {
	                Edge edge = pickSupport.getEdge(p.getX(), p.getY());
	                if(edge != null) {
	                    pickedState.pick(edge, !pickedState.isPicked(edge));
	                }
	            }
	            vv.repaint();
	        }
	    }
	}
	    
	
	/**
	 * clean up after the pick of a Vertex or Edge
	 */
	public void mouseReleased(MouseEvent e) {
	    vertexToDrag = null;
	}
	
	/**
	 * if a Vertex was picked in the mousePressed method, use the
	 * layout class to force that Vertex to move, following the
	 * motion of the mouse.
	 */
	public void mouseDragged(MouseEvent e) {
		if (vertexToDrag != null) {
		    Point2D p = vv.inverseTransform(e.getPoint());
	        Layout layout = vv.getGraphLayout();
	        layout.forceMove(vertexToDrag, p.getX()-offsetx, p.getY()-offsety);
	        vv.repaint();
		}
	}
	
	/**
	 * no-op here
	 */
	public void mouseMoved(MouseEvent e) {
		return;
	}
	
	/**
	 * no-op here
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
	    return;
	}
}

