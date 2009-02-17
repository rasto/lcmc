/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 26, 2005
 */

package edu.uci.ics.jung.visualization.control;

import java.awt.event.InputEvent;

/**
 * an implementation of the AbstractModalGraphMouse that includes plugins for
 * manipulating a view that is using a LensTransformer.
 * 
 * @author Tom Nelson - RABA Techologies
 *
 */
public class ModalLensGraphMouse extends AbstractModalGraphMouse implements
        ModalGraphMouse {

	/**
	 * not included in the base class
	 */
    protected LensMagnificationGraphMousePlugin magnificationPlugin;
    
    public ModalLensGraphMouse() {
        this(1.1f, 1/1.1f);
    }

    public ModalLensGraphMouse(float in, float out) {
        this(in, out, new LensMagnificationGraphMousePlugin());
    }

    public ModalLensGraphMouse(LensMagnificationGraphMousePlugin magnificationPlugin) {
        this(1.1f, 1/1.1f, magnificationPlugin);
    }
    
    public ModalLensGraphMouse(float in, float out, LensMagnificationGraphMousePlugin magnificationPlugin) {
        this.in = in;
        this.out = out;
        this.magnificationPlugin = magnificationPlugin;
        loadPlugins();
    }
    
    protected void loadPlugins() {
        pickingPlugin = new PickingGraphMousePlugin();
        animatedPickingPlugin = new AnimatedPickingGraphMousePlugin();
        translatingPlugin = new LensTranslatingGraphMousePlugin(InputEvent.BUTTON1_MASK);
        scalingPlugin = new ScalingGraphMousePlugin(new LayoutScalingControl(), 0, in, out);
        rotatingPlugin = new RotatingGraphMousePlugin();
        shearingPlugin = new ShearingGraphMousePlugin();
        
        add(magnificationPlugin);
        add(scalingPlugin);

        setMode(Mode.TRANSFORMING);
    }
}
