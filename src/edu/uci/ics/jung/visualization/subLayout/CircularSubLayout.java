/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 23, 2005
 */
package edu.uci.ics.jung.visualization.subLayout;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;

/**
 * An implementation of SubLayout that places its collection of
 * Vertices in a circle. The center and radius are settable
 * properties.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class CircularSubLayout implements SubLayout {

	protected double radius;
    protected Point2D center;
    protected final Map map = new LinkedHashMap();

    /**
     * create an instance with passed values
     * @param vertices the collection of vertices to arrange in a circle
     * @param radius the radius of the circle
     * @param center the center of the circle
     */
	public CircularSubLayout(Collection vertices, double radius, Point2D center) {
        this.radius = radius;
        this.center = center;
        initializeLocations(vertices);
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	/**
	 * Map the Vertices in the passed collection to their
	 * locations, distributed about the circumference of
	 * a circle
	 * 
	 * @param vertices
	 */
	private void initializeLocations(Collection vertices) {
		Vertex[] vertexArray =
		    (Vertex[]) vertices.toArray(new Vertex[vertices.size()]);
		
        if(center == null) {
            center = new Point2D.Double(radius, radius);
        }
        // only apply sublayout if there is more than one vertex
        if (vertexArray.length > 1) {
			for (int i = 0; i < vertexArray.length; i++) {
				double angle = (2 * Math.PI * i) / vertexArray.length;
				Point2D point = new Point2D.Double(
						(Math.cos(angle) * radius + center.getX()), (Math
								.sin(angle)
								* radius + center.getY()));
				map.put(vertexArray[i], point);
			}
		}
	}
	
    public Point2D getLocation(ArchetypeVertex v) {
        return (Point2D)map.get(v);
    }
}
