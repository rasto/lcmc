/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 *
 */
package edu.uci.ics.jung.visualization;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeSupport;

import edu.uci.ics.jung.visualization.transform.MutableTransformer;


/**
 * Lens is intended to be used as an overlay on the
 * BirdsEyeVisualizationViewer. It is a Rectangle that
 * acts as a MouseListener (for moving and resizing the
 * Rectangle).
 * 
 * @deprecated use the SatelliteVisualizationViewer instead
 * 
 * @author Tom Nelson - RABA Technologies
 * 
 *  
 */
public class Lens extends Rectangle2D.Float implements MouseListener,
        MouseMotionListener {

    /**
     * true if we are dragging the Rectangle around
     */
    protected boolean pan;

    /**
     * true if we are dragging the right leg
     */
    protected boolean dragRightLeg;

    /**
     * true if we are dragging the base 
     */
    protected boolean dragBase;

    /**
     * true if we are dragging the left leg
     */
    protected boolean dragLeftLeg;

    /**
     * true if we are dragging the top
     */
    protected boolean dragTop;

    /**
     * true if the mouse pointer is outside the window
     */
    protected boolean outside;

    /**
     * the offset in the x direction, as a percentage of width
     */
    protected float offx;

    /**
     * the offset in the y direction, as a percentage of height
     */
    protected float offy;

    /**
     * the left leg of the rectangle
     */
    protected Line2D leftLeg;

    /**
     * the right leg of the rectangle
     */
    protected Line2D rightLeg;

    /**
     * the base of the rectangle
     */
    protected Line2D base;

    /**
     * the top of the rectangle
     */
    protected Line2D top;

    /**
     * the scale of the BirdsEyeVisualizationViewer compared to
     * the graph display
     */
    protected float scalex;

    /**
     * the scale of the BirdsEyeVisualizationViewer compared to
     * the graph display
     */
    protected float scaley;

    /**
     * the layout being used by the BirdsEye
     */
    protected Layout layout;
    
    /**
     * the VisualizationViewer that is scaled and translated
     * by this Lens
     */
    protected VisualizationViewer vv;

    /**
     * support for property changes
     */
    protected PropertyChangeSupport support;

    /**
     * ratio of width to height
     */
    protected float aspectRatio;
    
    protected Point down;
    
    protected AffineTransform lensXform;

    /**
     * Create a Lens that is centered in the 
     * BirdsEyeVisualizationViewer
     * 
     */
    public Lens(VisualizationViewer vv, float scalex, float scaley) {
        super(vv.getGraphLayout().getCurrentSize().width * scalex / 4, 
                vv.getGraphLayout().getCurrentSize().height * scaley / 4, 
                vv.getGraphLayout().getCurrentSize().width * scalex / 2,
                vv.getGraphLayout().getCurrentSize().height * scaley / 2);
        this.vv = vv;
        this.layout = vv.getGraphLayout();
        this.scalex = scalex;
        this.scaley = scaley;
        lensXform = AffineTransform.getScaleInstance(1/scalex, 1/scaley);
        this.aspectRatio = (float)((getMaxX() - getMinX()) / (getMaxY() - getMinY()));
        rightLeg = new Line2D.Float((float)getMaxX(), (float)getMinY(), (float)getMaxX(), (float)getMaxY());
        leftLeg = new Line2D.Float((float)getMinX(), (float)getMinY(), (float)getMinX(), (float)getMaxY());
        base = new Line2D.Float((float)getMinX(), (float)getMaxY(), (float)getMaxX(), (float)getMaxY());
        top = new Line2D.Float((float)getMinX(), (float)getMinY(), (float)getMaxX(), (float)getMinY());
    }

    /**
     * reset the rectangle to the full size of the BirdsEyeVisualizationViewer
     * This will result in no zoom or pan of the main display
     *
     */
    public void reset() {
        Dimension d = vv.getSize();
        Dimension ld = vv.getGraphLayout().getCurrentSize();
		vv.getViewTransformer().setScale((float)d.width/ld.width, (float)d.height/ld.height, new Point2D.Float());
    }
    
    public void setFrame(VisualizationViewer vv) {
        MutableTransformer viewTransformer = vv.getViewTransformer();
        Dimension d = new Dimension(
              (int) (vv.getSize().width  * scalex ), 
              (int) (vv.getSize().height  * scaley ));
        float width = (float) (d.width/(viewTransformer.getScaleX()));
        float height = (float) (d.height/(viewTransformer.getScaleY()));
        float x = -(float) (viewTransformer.getTranslateX()*scalex/(viewTransformer.getScaleX()));
        float y = -(float) (viewTransformer.getTranslateY()*scaley/(viewTransformer.getScaleY()));
        setFrame(x,y,width,height);
    }
    
    /**
     * set the Rectangle to be centered in the BirdsEyeVisualizationViewer
     *
     */
    public void init() {
        vv.getViewTransformer().setScale(2.0f, 2.0f, vv.getCenter());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {

        down = e.getPoint();
        
        if (leftLeg.ptLineDist(down) < 5) {
            dragLeftLeg = true;
        } else if (rightLeg.ptLineDist(down) < 5) {
            dragRightLeg = true;
        } else if (base.ptLineDist(down) < 5) {
            dragBase = true;
        } else if (top.ptLineDist(down) < 5) {
            dragTop = true;
        } else if (contains(down)) {
            pan = true;
        } else {
            pan = dragLeftLeg = dragRightLeg = dragBase = dragTop = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
        pan = dragLeftLeg = dragRightLeg = dragTop = dragBase = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
        outside = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
        outside = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
        if (outside)
            return;

        Point2D q = down;
        Point2D p = e.getPoint();
        if (pan) {
            vv.getViewTransformer().translate((q.getX()-p.getX())/scalex, (q.getY()-p.getY())/scaley);
        } else {
            float dw = 0.0f;
            float dh = 0.0f;

            if (dragRightLeg) {
                dw = (float) (p.getX()-q.getX());
                dh = dw / aspectRatio;

            } else if (dragLeftLeg) {
                dw = (float) (q.getX()-p.getX());
                dh = dw / aspectRatio;

            } else if (dragBase) {
                dh = (float) (p.getY()-q.getY());
                dw = dh * aspectRatio;

            } else if (dragTop) {
                dh = (float) (q.getY()-p.getY());
                dw = dh * aspectRatio;
            }
            float pwidth = (float)getMaxX() - (float)getMinX();
            float pheight = (float)getMaxY() - (float)getMinY();
            float newWidth = pwidth + 2 * dw;
            float newHeight = pheight + 2 * dh;
            if (newWidth < 3 || newHeight < 3)
                return;
            vv.getViewTransformer().scale(pwidth/newWidth, pheight/newHeight, vv.getCenter());
        }
        down = e.getPoint();

        rightLeg = new Line2D.Float((float)getMaxX(), (float)getMinY(), (float)getMaxX(), (float)getMaxY());
        leftLeg = new Line2D.Float((float)getMinX(), (float)getMinY(), (float)getMinX(), (float)getMaxY());
        base = new Line2D.Float((float)getMinX(), (float)getMaxY(), (float)getMaxX(), (float)getMaxY());
        top = new Line2D.Float((float)getMinX(), (float)getMinY(), (float)getMaxX(), (float)getMinY());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {
    }
}