/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 *
 * Created on Apr 12, 2005
 */
package edu.uci.ics.jung.visualization;

import java.awt.geom.Point2D;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

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
public class RadiusGraphElementAccessor implements GraphElementAccessor {
    
    protected Layout layout;
    protected double maxDistance;
    
    public RadiusGraphElementAccessor(Layout l) {
        this(l, Math.sqrt(Double.MAX_VALUE - 1000));
    }
    
    public RadiusGraphElementAccessor(Layout l, double maxDistance) {
        this.maxDistance = maxDistance;
        this.layout = l;
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
		double minDistance = maxDistance * maxDistance;
        Vertex closest = null;
		while(true) {
		    try {
		        for (Iterator iter = layout.getGraph().getVertices().iterator();
		        iter.hasNext();
		        ) {
		            Vertex v = (Vertex) iter.next();
		            Point2D p = layout.getLocation(v);
		            double dx = p.getX() - x;
		            double dy = p.getY() - y;
		            double dist = dx * dx + dy * dy;
		            if (dist < minDistance) {
		                minDistance = dist;
		                closest = v;
		            }
		        }
		        break;
		    } catch(ConcurrentModificationException cme) {}
		}
		return closest;
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
		double minDistance = maxDistance * maxDistance;
		Edge closest = null;
		while(true) {
		    try {
		        for (Iterator iter = layout.getGraph().getEdges().iterator(); iter.hasNext();) {
		            Edge e = (Edge) iter.next();
		            // if anyone uses a hyperedge, this is too complex.
		            if (e.numVertices() != 2)
		                continue;
		            // Could replace all this set stuff with getFrom_internal() etc.
		            Set vertices = e.getIncidentVertices();
		            Iterator vertexIterator = vertices.iterator();
		            Vertex v1 = (Vertex) vertexIterator.next();
		            Vertex v2 = (Vertex) vertexIterator.next();
		            // Get coords
		            Point2D p1 = layout.getLocation(v1);
		            Point2D p2 = layout.getLocation(v2);
		            double x1 = p1.getX();
		            double y1 = p1.getY();
		            double x2 = p2.getX();
		            double y2 = p2.getY();
		            // Calculate location on line closest to (x,y)
		            // First, check that v1 and v2 are not coincident.
		            if (x1 == x2 && y1 == y2)
		                continue;
		            double b =
		                ((y - y1) * (y2 - y1) + (x - x1) * (x2 - x1))
		                / ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		            //
		            double distance2; // square of the distance
		            if (b <= 0)
		                distance2 = (x - x1) * (x - x1) + (y - y1) * (y - y1);
		            else if (b >= 1)
		                distance2 = (x - x2) * (x - x2) + (y - y2) * (y - y2);
		            else {
		                double x3 = x1 + b * (x2 - x1);
		                double y3 = y1 + b * (y2 - y1);
		                distance2 = (x - x3) * (x - x3) + (y - y3) * (y - y3);
		            }
		            
		            if (distance2 < minDistance) {
		                minDistance = distance2;
		                closest = e;
		            }
		        }
		        break;
		    } catch(ConcurrentModificationException cme) {}
		}
		return closest;
	}

    public void setLayout(Layout l)
    {
        this.layout = l;
    }
}
