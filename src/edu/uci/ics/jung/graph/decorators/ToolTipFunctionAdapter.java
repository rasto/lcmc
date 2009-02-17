/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Jul 1, 2005
 */

package edu.uci.ics.jung.graph.decorators;

import java.awt.event.MouseEvent;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * A convenience implementation of ToolTipFunction which provides
 * no tool tips. Only the desired methods need be overridden to
 * get desired behavior.
 * 
 * @author Tom Nelson - RABA Technologies
 *
 * 
 */
public class ToolTipFunctionAdapter implements ToolTipFunction {

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.graph.decorators.ToolTipFunction#getToolTipText(edu.uci.ics.jung.graph.Vertex)
     */
    public String getToolTipText(Vertex v) {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.graph.decorators.ToolTipFunction#getToolTipText(edu.uci.ics.jung.graph.Edge)
     */
    public String getToolTipText(Edge e) {
        return null;
    }

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.VisualizationViewer.ToolTipListener#getToolTipText(java.awt.event.MouseEvent)
     */
    public String getToolTipText(MouseEvent event) {
        return null;
    }

}
