/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.visualization.transform.MutableAffineTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * BirdsEyeVisualizationViewer is intended to be an additional display of a
 * graph and layout that is being manipulated elsewhere. This class makes no
 * calls that mutate the graph or layout
 * 
 * @deprecated Use the SatelliteVisualizationViewer instead
 * @author Tom Nelson - RABA Technologies
 * 
 *  
 */
public class BirdsEyeVisualizationViewer extends JPanel {

    protected VisualizationViewer vv;

    protected Renderer renderer;
    
    protected VisualizationModel model;
    
	protected Map renderingHints = new HashMap();

    protected float scalex = 1.0f;

    protected float scaley = 1.0f;

    protected Lens lens;
    
    protected MutableTransformer layoutTransformer =
        new MutableAffineTransformer(new AffineTransform());
    
    /**
     * create an instance with passed values
     * @param layout the layout to use
     * @param r the renderer to use
     * @param scalex the scale in the horizontal direction
     * @param scaley the scale in the vertical direction
     */
    public BirdsEyeVisualizationViewer(VisualizationViewer vv,
            float scalex, float scaley) {
        this.vv = vv;
        this.model = vv.getModel();
        this.model.setRelaxerThreadSleepTime(200);
        this.renderer = vv.getRenderer();
        this.scalex = scalex;
        this.scaley = scaley;
        renderingHints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        setLayoutTransformer(vv.getLayoutTransformer());
    }
    
    

    /**
     * @return Returns the layoutTransformer.
     */
    public MutableTransformer getLayoutTransformer() {
        return layoutTransformer;
    }



    /**
     * @param layoutTransformer The layoutTransformer to set.
     */
    public void setLayoutTransformer(MutableTransformer layoutTransformer) {
        this.layoutTransformer = layoutTransformer;
    }



    /**
     * reset the Lens to no zoom/no offset.
     * passes call to Lens 
     *
     */
    public void resetLens() {
        if(lens != null) {
            lens.reset();
        }
    }
    
    /**
     * set the initial values for the Lens 
     * (50% zoom centered in display)
     *
     */
    public void initLens() {
        if(lens != null) {
            lens.init();
        }
    }
    
    /**
     * proportionally zoom the Lens
     * @param percent
     */
    public void zoom(float percent) {
        vv.getViewTransformer().scale(percent, percent, vv.getCenter());
    }

    /**
     * defers setting the perferred size until the component is
     * live and the layout size is known
     * Adds the mouse clicker at that time.
     */
    public void addNotify() {
        super.addNotify();
        Dimension layoutSize = model.getGraphLayout().getCurrentSize();
        if (layoutSize != null) {
            Dimension mySize = new Dimension((int) (layoutSize.width * scalex),
                    (int) (layoutSize.height * scaley));
            setPreferredSize(mySize);
            initMouseClicker();
        }
    }

    /**
     *  Creates and adds the Lens to control zoom/pan functions
     */
    protected void initMouseClicker() {
        Dimension layoutSize = model.getGraphLayout().getCurrentSize();
        if (layoutSize != null) {
            lens = new Lens(vv, scalex, scaley);
        }
        vv.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                VisualizationViewer vv =
                    (VisualizationViewer)e.getSource();
                lens.setFrame(vv);
                repaint();
            }
        });
        addMouseListener(lens);
        addMouseMotionListener(lens);
    }

    /**
     * UNTESTED.
     */
    public void setRenderer(Renderer r) {
        this.renderer = r;
        repaint();
    }

    /**
     * setter for layout
     * @param v the layout
     */
    public void setGraphLayout(Layout layout) {
        model.setGraphLayout(layout);
    }

    /**
     * getter for graph layout
     * @return the layout
     */
    public Layout getGraphLayout() {
        return model.getGraphLayout();
    }

    /**
     * paint the graph components. The Graphics will have been transformed
     * by scalex and scaley prior to calling this method
     * @param g
     */
    private void paintGraph(Graphics g) {
        Layout layout = model.getGraphLayout();
        // paint all the Edges
        for (Iterator iter = layout.getGraph().getEdges().iterator(); iter
                .hasNext();) {
            Edge e = (Edge) iter.next();
            Vertex v1 = (Vertex) e.getEndpoints().getFirst();
            Vertex v2 = (Vertex) e.getEndpoints().getSecond();
            Point2D p1 = layout.getLocation(v1);
            Point2D p2 = layout.getLocation(v2);
            p1 = layoutTransformer.transform(p1);
            p2 = layoutTransformer.transform(p2);
            renderer.paintEdge(g, e, (int) p1.getX(), 
                    (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
        }
        // Paint all the Vertices
        for (Iterator iter = layout.getGraph().getVertices().iterator(); iter
                .hasNext();) {
            Vertex v = (Vertex) iter.next();
            Point2D p = layout.getLocation(v);
            p = layoutTransformer.transform(p);
            renderer.paintVertex(g, v, (int) p.getX(), 
                    (int) p.getY());
        }
    }

    /**
     * paint the graph, transforming with the scalex and scaley
     */
    protected synchronized void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHints(renderingHints);

        AffineTransform oldXform = g2d.getTransform();
        AffineTransform newXform = new AffineTransform(oldXform);
        newXform.scale(scalex, scaley);
        g2d.setTransform(newXform);

        paintGraph(g);

        Color oldColor = g.getColor();
        // the Lens will be cyan
        g2d.setColor(Color.cyan);
        
        // put the old transform back (not scaled) to draw the Lens
        g2d.setTransform(oldXform);
        if (lens != null) {
            g2d.draw(lens);
        }
        // restore old paint after drawing the Lens
        g.setColor(oldColor);
    }
}