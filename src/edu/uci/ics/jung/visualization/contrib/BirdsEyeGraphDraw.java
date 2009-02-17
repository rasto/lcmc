/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization.contrib;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JComponent;

import edu.uci.ics.jung.visualization.BirdsEyeVisualizationViewer;
import edu.uci.ics.jung.visualization.GraphDraw;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.Renderer;

/**
 * Similar to GraphDraw, but with no method calls that will modify the
 * underlying graph. This component is used as the view to create a
 * birds-eye zoom/pan tool
 * 
 * @author Tom Nelson - adapted from code written by Danyel Fisher
 * @deprecated As of version 1.7.  See SatelliteViewDemo for an example of how to do this.
 */
public class BirdsEyeGraphDraw extends JComponent {

    /**
     * the GraphDraw to manage
     */
    protected GraphDraw gd;
	
	/**
	 * the first SettableRenderer created
	 */
	private final Renderer originalRenderer;
	
	/**
	 * the renderer that is passed thru to the VisualizationViewer
	 */
	Renderer r;
	
	/**
	 * the layout in use
	 */
	Layout layout;
	
	/**
	 * the VisualizationViewer for this GraphDraw. It does not
	 * modify the underlying graph
	 */
	BirdsEyeVisualizationViewer vv;

	/**
	 * Creates a read-only graph drawing environment that draws this graph object. By
	 * default, uses the Spring layout, the Fade renderer and the
	 * AbstractSettable renderer, 
	 * 
	 * @param g the graph
	 * @param scalex the scale in the horizontal axis
	 * @param scaley the scale in the vertical axis
	 */
	public BirdsEyeGraphDraw(GraphDraw gd, float scalex, float scaley) {
		this.gd = gd;
		//StringLabeller sl = StringLabeller.getLabeller(g);
		layout = gd.getVisualizationViewer().getGraphLayout();
		originalRenderer = gd.getVisualizationViewer().getRenderer();//new SettableRenderer(sl);
		r = originalRenderer;
		vv = new BirdsEyeVisualizationViewer(gd.getVisualizationViewer(), scalex, scaley);
		setLayout(new BorderLayout());
		add(vv, BorderLayout.CENTER);
	}

	/**
	 * Returns the visualizationviewer that actually does the graph drawing.
	 * 
	 * @return
	 */
	public BirdsEyeVisualizationViewer getVisualizationViewer() {
		return vv;
	}

	public void setBackground(Color bg) {
		super.setBackground(bg);
		vv.setBackground(bg);
	}

	/**
	 * A method to set the renderer. Passes it to the VisualizationViewer
	 * 
	 * @param r
	 *            the new renderer
	 */
	public void setRenderer(Renderer r) {
		this.r = r;
		vv.setRenderer(r);
	}

	/**
	 * resets to the original renderer. Passes is to the VisualizationViewer
	 *
	 */
	public void resetRenderer() {
		this.r = originalRenderer;
		vv.setRenderer(r);
	}

	/** 
	 * getter for the original renderer
	 * @return the original renderer
	 */
	public Renderer getRender() {
		return originalRenderer;
	}


	/**
	 * Dynamically chooses a new GraphLayout.
	 * 
	 * @param l
	 *            the new graph layout algorithm
	 */
	public void setGraphLayout(Layout l) {
		this.layout = l;
		vv.setGraphLayout(l);
	}

	/**
	 * Returns the currently operative layout.
	 */
	public Layout getGraphLayout() {
		return layout;
	}
}
