/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 * Created on Mar 8, 2005
 *
 */
package edu.uci.ics.jung.visualization;

import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;


/** 
 * ZoomPanGraphMouse is a PluggableGraphMouse class that includes
 * zoom via the mouse wheel, and pan via a mouse drag
 * 
 *  
 * @author Tom Nelson
 */
public class ZoomPanGraphMouse extends PluggableGraphMouse {

    protected TranslatingGraphMousePlugin translatingPlugin;
    protected ScalingGraphMousePlugin scalingPlugin;

	/**
	 * create an instance with default zoom in/out values
     * @deprecated no need to pass a VisualizationViewer in constructor
	 * @param vv the VisualizationViewer not used
	 */
	public ZoomPanGraphMouse(VisualizationViewer vv) {
	    this(1.1f, 1/1.1f);
	}

	/**
	 * create an instance with passed zoom in/out values
     * @deprecated no need to pass a VisualizationViewer
	 * @param vv the VisualizationViewer - not used
	 * @param in zoom in value
	 * @param out zoom out value
	 */
	public ZoomPanGraphMouse(VisualizationViewer vv, float in, float out) {
	    this(in, out);
	}
    
    public ZoomPanGraphMouse() {
        this(1.1f, 1/1.1f);
    }
    
    public ZoomPanGraphMouse(float in, float out) {
        translatingPlugin = new TranslatingGraphMousePlugin();
        scalingPlugin = new ScalingGraphMousePlugin(new LayoutScalingControl(), 0, in, out);
        add(translatingPlugin);
        add(scalingPlugin);
    }

    /**
     * @param zoomAtMouse The zoomAtMouse to set.
     */
    public void setZoomAtMouse(boolean zoomAtMouse) {
        scalingPlugin.setZoomAtMouse(zoomAtMouse);
    }
}
