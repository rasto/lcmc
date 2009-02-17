/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package edu.uci.ics.jung.visualization.transform;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * LensTransformer wraps a MutableAffineTransformer and modifies
 * the transform and inverseTransform methods so that they create a
 * projection of the graph points within an elliptical lens.
 * 
 * LensTransformer uses an
 * affine transform to cause translation, scaling, rotation, and shearing
 * while applying a possibly non-affine filter in its transform and
 * inverseTransform methods.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public abstract class LensTransformer extends MutableTransformerDecorator implements MutableTransformer {

    /**
     * the area affected by the transform
     */
    protected Ellipse2D ellipse = new Ellipse2D.Float();
    
    protected float magnification = 0.7f;
    
    /**
     * create an instance, setting values from the passed component
     * and registering to listen for size changes on the component
     * @param component
     */
    public LensTransformer(Component component) {
        this(component, new MutableAffineTransformer());
    }
    /**
     * create an instance with a possibly shared transform
     * @param component
     * @param delegate
     */
    public LensTransformer(Component component, MutableTransformer delegate) {
    		super(delegate);
        setComponent(component);
        component.addComponentListener(new ComponentListenerImpl());
   }
    
    /**
     * set values from the passed component.
     * declared private so it can't be overridden
     * @param component
     */
    private void setComponent(Component component) {
        Dimension d = component.getSize();
        if(d.width <= 0 || d.height <= 0) {
            d = component.getPreferredSize();
        }
        float ewidth = d.width/1.5f;
        float eheight = d.height/1.5f;
        ellipse.setFrame(d.width/2-ewidth/2, d.height/2-eheight/2, ewidth, eheight);
    }
    
    /**
     * @return Returns the magnification.
     */
    public float getMagnification() {
        return magnification;
    }
    /**
     * @param magnification The magnification to set.
     */
    public void setMagnification(float magnification) {
        this.magnification = magnification;
    }
    /**
     * @return Returns the viewCenter.
     */
    public Point2D getViewCenter() {
        return new Point2D.Double(ellipse.getCenterX(), ellipse.getCenterY());
    }
    /**
     * @param viewCenter The viewCenter to set.
     */
    public void setViewCenter(Point2D viewCenter) {
        double width = ellipse.getWidth();
        double height = ellipse.getHeight();
        ellipse.setFrame(viewCenter.getX()-width/2,
                viewCenter.getY()-height/2,
                width, height);
    }

    /**
     * @return Returns the viewRadius.
     */
    public double getViewRadius() {
        return ellipse.getHeight()/2;
    }
    /**
     * @param viewRadius The viewRadius to set.
     */
    public void setViewRadius(double viewRadius) {
        double x = ellipse.getCenterX();
        double y = ellipse.getCenterY();
        double viewRatio = getRatio();
        ellipse.setFrame(x-viewRadius/viewRatio,
                y-viewRadius,
                2*viewRadius/viewRatio,
                2*viewRadius);
    }
    
    /**
     * @return Returns the ratio.
     */
    public double getRatio() {
        return ellipse.getHeight()/ellipse.getWidth();
    }
    
    public void setEllipse(Ellipse2D ellipse) {
        this.ellipse = ellipse;
    }
    public Ellipse2D getEllipse() {
        return ellipse;
    }
    public void setToIdentity() {
        this.delegate.setToIdentity();
    }

    /**
     * react to size changes on a component
     */
    protected class ComponentListenerImpl extends ComponentAdapter {
        public void componentResized(ComponentEvent e) {
            setComponent(e.getComponent());
         }
    }
    
    /**
     * a convenience class to represent a point in
     * polar coordinates
     */
    protected static class PolarPoint extends Point2D.Double {
        public PolarPoint(double theta, double radius) {
            super(theta, radius);
        }
        public double getTheta() { return getX(); }
        public double getRadius() { return getY(); }
        public void setTheta(double theta) { setLocation(theta, getRadius()); }
        public void setRadius(double radius) { setLocation(getTheta(), radius); }
    }
    
    /**
     * Returns the result of converting <code>polar</code> to Cartesian coordinates.
     */
    protected Point2D polarToCartesian(PolarPoint polar) {
        return polarToCartesian(polar.getTheta(), polar.getRadius());
    }
    
    /**
     * Returns the result of converting <code>(theta, radius)</code> to Cartesian coordinates.
     */
     protected Point2D polarToCartesian(double theta, double radius) {
        return new Point2D.Double(radius*Math.cos(theta), radius*Math.sin(theta));
    }
    
    /**
     * Returns the result of converting <code>point</code> to polar coordinates.
     */
    protected PolarPoint cartesianToPolar(Point2D point) {
        return cartesianToPolar(point.getX(), point.getY());
    }
    
    /**
     * Returns the result of converting <code>(x, y)</code> to polar coordinates.
     */
    protected PolarPoint cartesianToPolar(double x, double y) {
        double theta = Math.atan2(y,x);
        double radius = Math.sqrt(x*x+y*y);
        return new PolarPoint(theta, radius);
    }
    
    /**
     * override base class transform to project the fisheye effect
     */
    public abstract Point2D transform(Point2D graphPoint);
    
    /**
     * override base class to un-project the fisheye effect
     */
    public abstract Point2D inverseTransform(Point2D viewPoint);
    
    public double getDistanceFromCenter(Point2D p) {
        double dx = ellipse.getCenterX()-p.getX();
        double dy = ellipse.getCenterY()-p.getY();
        dx *= getRatio();
        return Math.sqrt(dx*dx + dy*dy);
    }
}