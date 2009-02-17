/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 */
package edu.uci.ics.jung.visualization.transform.shape;

import java.awt.Dimension;

import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.Renderer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalLensGraphMouse;
import edu.uci.ics.jung.visualization.transform.AbstractLensSupport;
import edu.uci.ics.jung.visualization.transform.LensSupport;
import edu.uci.ics.jung.visualization.transform.LensTransformer;

/**
 * Uses a LensTransformer to use in the view
 * transform. This one will distort Vertex shapes.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class ViewLensSupport extends AbstractLensSupport
    implements LensSupport {
    
    protected PluggableRenderer pluggableRenderer;
    protected TransformingPluggableRenderer transformingPluggableRenderer;
    protected Renderer renderer;
    
    public ViewLensSupport(VisualizationViewer vv) {
        this(vv, new HyperbolicShapeTransformer(vv),
                new ModalLensGraphMouse());
    }
    public ViewLensSupport(VisualizationViewer vv, LensTransformer lensTransformer,
            ModalGraphMouse lensGraphMouse) {
        super(vv, lensGraphMouse);
        this.renderer = vv.getRenderer();
        this.lensTransformer = lensTransformer;
        Dimension d = vv.getSize();
        if(d.width == 0 || d.height == 0) {
            d = vv.getPreferredSize();
        }
        lensTransformer.setViewRadius(d.width/5);

        if(renderer instanceof PluggableRenderer) {
            this.pluggableRenderer = (PluggableRenderer)renderer;
        } else {
            this.pluggableRenderer = new PluggableRenderer();
        }
    }
    public void activate() {
        if(lens == null) {
            lens = new Lens(lensTransformer);
        }
        if(lensControls == null) {
            lensControls = new LensControls(lensTransformer);
        }
        vv.setViewTransformer(lensTransformer);
        if(transformingPluggableRenderer == null) {   
            transformingPluggableRenderer = 
            		new TransformingPluggableRenderer(pluggableRenderer);
        }
        transformingPluggableRenderer.setTransformer(lensTransformer);
        vv.setRenderer(transformingPluggableRenderer);
        vv.addPreRenderPaintable(lens);
        vv.addPostRenderPaintable(lensControls);
        vv.setGraphMouse(lensGraphMouse);
        vv.setToolTipText(instructions);
        vv.repaint();
    }

    public void deactivate() {
        vv.setViewTransformer(savedViewTransformer);
        vv.removePreRenderPaintable(lens);
        vv.removePostRenderPaintable(lensControls);
        vv.setRenderer(renderer);
        vv.setToolTipText(defaultToolTipText);
        vv.setGraphMouse(graphMouse);
        vv.repaint();
    }
}