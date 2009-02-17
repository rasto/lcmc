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

import javax.swing.JComponent;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Returns the results of toString on Vertices and Edges
 * Used mainly in demos
 * 
 * @author Tom Nelson - RABA Technologies
 *
 * 
 */
public class DefaultToolTipFunction extends ToolTipFunctionAdapter {
    /**
     * @param v the Vertex
     * @return toString on the passed Vertex
     */
    public String getToolTipText(Vertex v) {
        return v.toString();
    }
    
    /**
     * @param e the Edge
     * @return toString on the passed Edge
     */
    public String getToolTipText(Edge e) {
        return e.toString();
    }
    
    public String getToolTipText(MouseEvent e) {
        return ((JComponent)e.getSource()).getToolTipText();
    }
}
