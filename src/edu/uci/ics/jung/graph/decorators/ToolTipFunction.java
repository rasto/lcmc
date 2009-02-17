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

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.visualization.VisualizationViewer.ToolTipListener;

/**
 * An interface for supplying tooltips for elements of a jung graph
 * @author Tom Nelson - RABA Technologies
 *
 * 
 */
public interface ToolTipFunction extends ToolTipListener {
    
    String getToolTipText(Vertex v);
    String getToolTipText(Edge e);
}
