/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 * Created on Mar 11, 2005
 *
 */
package edu.uci.ics.jung.visualization;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.visualization.transform.LayoutTransformer;
import edu.uci.ics.jung.visualization.transform.MutableAffineTransformer;
import edu.uci.ics.jung.visualization.transform.Transformer;

/**
 * ShapePickSupport provides access to Vertices and Edges based on
 * their actual shapes. 
 * 
 * @author Tom Nelson - RABA Technologies
 *
 */
public class ShapePickSupport implements PickSupport {

    protected HasGraphLayout hasGraphLayout;
    protected HasShapeFunctions hasShapeFunctions;
    protected float pickSize;
    protected LayoutTransformer layoutTransformer;
    
    /**
     * Create an instance.
     * The HasGraphLayout is used as the source of the current
     * Graph Layout. The HasShapes
     * is used to access the VertexShapes and the EdgeShapes
     * @param hasGraphLayout source of the current layout.
     * @param hasShapeFunctions source of Vertex and Edge shapes.
     * @param pickSize how large to make the pick footprint for line edges
     */
    public ShapePickSupport(HasGraphLayout hasGraphLayout, 
            LayoutTransformer layoutTransformer, HasShapeFunctions hasShapeFunctions, 
            float pickSize) {
        this.hasGraphLayout = hasGraphLayout;
        this.hasShapeFunctions = hasShapeFunctions;
        this.layoutTransformer = layoutTransformer;
        this.pickSize = pickSize;
    }
    
    public ShapePickSupport(float pickSize) {
        this.pickSize = pickSize;
    }
            
    /**
     * Create an instance.
     * The pickSize footprint defaults to 2.
     */
    public ShapePickSupport() {
        this(2);
    }
    
    /**
     * called by a HasLayout impl (like VisualizationViewer) when this 
     * PickSupport impl is
     * added to it. This allows the PickSupport to
     * always get the current Layout and the current Renderer
     * from thecomponent it supports picking on.
     */
    public void setHasGraphLayout(HasGraphLayout hasGraphLayout) {
        this.hasGraphLayout = hasGraphLayout;
    }

    /**
     * @param hasShapes The hasShapes to set.
     */
    public void setHasShapes(HasShapeFunctions hasShapes) {
        this.hasShapeFunctions = hasShapes;
    }
    /**
     * @return Returns the layoutTransformer.
     */
    public LayoutTransformer getLayoutTransformer() {
        return layoutTransformer;
    }

    /**
     * When this PickSupport is set on a VisualizationViewer,
     * the VisualizationViewer calls this method to pass its
     * layout transformer in
     * 
     * @param layoutTransformer The layoutTransformer to set.
     */
    public void setLayoutTransformer(LayoutTransformer layoutTransformer) {
        this.layoutTransformer = layoutTransformer;
    }

    /** 
     * Iterates over Vertices, checking to see if x,y is contained in the
     * Vertex's Shape. If (x,y) is contained in more than one vertex, use
     * the vertex whose center is closest to the pick point.
     * @see edu.uci.ics.jung.visualization.PickSupport#getVertex(double, double)
     */
    public Vertex getVertex(double x, double y) {
        Layout layout = hasGraphLayout.getGraphLayout();

        Vertex closest = null;
        double minDistance = Double.MAX_VALUE;
        while(true) {
            try {
                for (Iterator iter=layout.getGraph().getVertices().iterator(); iter.hasNext();	) {
                    if(hasShapeFunctions != null) {
                        Vertex v = (Vertex) iter.next();
                        Shape shape = hasShapeFunctions.getVertexShapeFunction().getShape(v);
                        // transform the vertex location to screen coords
                        Point2D p = layoutTransformer.layoutTransform(layout.getLocation(v));
                        if(p == null) continue;
                        AffineTransform xform = 
                            AffineTransform.getTranslateInstance(p.getX(), p.getY());
                        shape = xform.createTransformedShape(shape);
                        // see if this vertex center is closest to the pick point
                        // among any other containing vertices
                        if(shape.contains(x, y)) {
                            
                            Rectangle2D bounds = shape.getBounds2D();
                            double dx = bounds.getCenterX() - x;
                            double dy = bounds.getCenterY() - y;
                            double dist = dx * dx + dy * dy;
                            if (dist < minDistance) {
                                minDistance = dist;
                                closest = v;
                            }
                        }
                    } 
                }
                break;
            } catch(ConcurrentModificationException cme) {}
        }
        return closest;
    }

    /**
     * return an edge whose shape intersects the 'pickArea' footprint of the passed
     * x,y, coordinates.
     */
    public Edge getEdge(double x, double y) {
        Layout layout = hasGraphLayout.getGraphLayout();

        // as a Line has no area, we can't always use edgeshape.contains(point) so we
        // make a small rectangular pickArea around the point and check if the
        // edgeshape.intersects(pickArea)
        Rectangle2D pickArea = 
            new Rectangle2D.Float((float)x-pickSize/2,(float)y-pickSize/2,pickSize,pickSize);
		Edge closest = null;
		double minDistance = Double.MAX_VALUE;
		while(true) {
		    try {
		        for (Iterator iter=layout.getGraph().getEdges().iterator(); iter.hasNext();	) {
		            
		            if(hasShapeFunctions != null) {
		                Edge e = (Edge) iter.next();
		                Pair pair = e.getEndpoints();
		                Vertex v1 = (Vertex)pair.getFirst();
		                Vertex v2 = (Vertex)pair.getSecond();
		                boolean isLoop = v1.equals(v2);
		                Point2D p1 = layoutTransformer.layoutTransform(layout.getLocation(v1));
		                Point2D p2 = layoutTransformer.layoutTransform(layout.getLocation(v2));
		                if(p1 == null || p2 == null) continue;
		                float x1 = (float) p1.getX();
		                float y1 = (float) p1.getY();
		                float x2 = (float) p2.getX();
		                float y2 = (float) p2.getY();
		                
		                // translate the edge to the starting vertex
		                AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);
		                
		                Shape edgeShape = hasShapeFunctions.getEdgeShapeFunction().getShape(e);
		                if(isLoop) {
		                    // make the loops proportional to the size of the vertex
		                    Shape s2 = hasShapeFunctions.getVertexShapeFunction().getShape(v2);
		                    Rectangle2D s2Bounds = s2.getBounds2D();
		                    xform.scale(s2Bounds.getWidth(),s2Bounds.getHeight());
		                    // move the loop so that the nadir is centered in the vertex
		                    xform.translate(0, -edgeShape.getBounds2D().getHeight()/2);
		                } else {
		                    float dx = x2 - x1;
		                    float dy = y2 - y1;
		                    // rotate the edge to the angle between the vertices
		                    double theta = Math.atan2(dy,dx);
		                    xform.rotate(theta);
		                    // stretch the edge to span the distance between the vertices
		                    float dist = (float) Math.sqrt(dx*dx + dy*dy);
		                    xform.scale(dist, 1.0f);
		                }
		                
		                // transform the edge to its location and dimensions
		                edgeShape = xform.createTransformedShape(edgeShape);
		                
		                // because of the transform, the edgeShape is now a GeneralPath
		                // see if this edge is the closest of any that intersect
		                if(edgeShape.intersects(pickArea)) {
		                    float cx=0;
		                    float cy=0;
		                    float[] f = new float[6];
		                    PathIterator pi = new GeneralPath(edgeShape).getPathIterator(null);
		                    if(pi.isDone()==false) {
		                        pi.next();
		                        pi.currentSegment(f);
		                        cx = f[0];
		                        cy = f[1];
		                        if(pi.isDone()==false) {
		                            pi.currentSegment(f);
		                            cx = f[0];
		                            cy = f[1];
		                        }
		                    }
		                    float dx = (float) (cx - x);
		                    float dy = (float) (cy - y);
		                    float dist = dx * dx + dy * dy;
		                    if (dist < minDistance) {
		                        minDistance = dist;
		                        closest = e;
		                    }
		                }
		            }
		        }
		        break;
		    } catch(ConcurrentModificationException cme) {}
		}
		return closest;
    }

    /**
     * <code>ShapePickSupport</code> gets its layout from its VisualizationViewer, so this
     * method currently does nothing.
     * @see edu.uci.ics.jung.visualization.PickSupport#setLayout(edu.uci.ics.jung.visualization.Layout)
     */
    public void setLayout(Layout layout) {}
}
