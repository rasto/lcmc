/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Jul 21, 2005
 */

package edu.uci.ics.jung.visualization.transform;

import java.awt.Dimension;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalLensGraphMouse;
/**
 * A class to make it easy to add an 
 * examining lens to a jung graph application. See HyperbolicTransformerDemo
 * for an example of how to use it.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class LayoutLensSupport extends AbstractLensSupport 
    implements LensSupport {

    public LayoutLensSupport(VisualizationViewer vv) {
        this(vv, new HyperbolicTransformer(vv, vv.getLayoutTransformer()),
                new ModalLensGraphMouse());
    }
    /**
     * create the base class, setting common members and creating
     * a custom GraphMouse
     * @param vv the VisualizationViewer to work on
     */
    public LayoutLensSupport(VisualizationViewer vv, LensTransformer lensTransformer,
            ModalGraphMouse lensGraphMouse) {
        super(vv, lensGraphMouse);
        this.lensTransformer = lensTransformer;

        Dimension d = vv.getSize();
        if(d.width <= 0 || d.height <= 0) {
            d = vv.getPreferredSize();
        }
        lensTransformer.setViewRadius(d.width/5);
   }
    
    public void activate() {
        if(lens == null) {
            lens = new Lens(lensTransformer);
        }
        if(lensControls == null) {
            lensControls = new LensControls(lensTransformer);
        }
        vv.setLayoutTransformer(lensTransformer);
        vv.setViewTransformer(new MutableAffineTransformer());
        vv.addPreRenderPaintable(lens);
        vv.addPostRenderPaintable(lensControls);
        vv.setGraphMouse(lensGraphMouse);
        vv.setToolTipText(instructions);
        vv.repaint();
    }
    
    public void deactivate() {
        if(savedViewTransformer != null) {
            vv.setViewTransformer(savedViewTransformer);
        }
        if(lensTransformer != null) {
            vv.removePreRenderPaintable(lens);
            vv.removePostRenderPaintable(lensControls);
            vv.setLayoutTransformer(lensTransformer.getDelegate());
        }
        vv.setToolTipText(defaultToolTipText);
        vv.setGraphMouse(graphMouse);
        vv.repaint();
    }
}
