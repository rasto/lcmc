/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Apr 16, 2005
 */

package edu.uci.ics.jung.visualization;

import edu.uci.ics.jung.graph.decorators.EdgeShapeFunction;
import edu.uci.ics.jung.graph.decorators.VertexShapeFunction;

/**
 * Interface used to tag classes that can provide Shapes for
 * graph elements. PluggableRenderer implements this interface
 * 
 * @author Tom Nelson - RABA Technologies
 */
public interface HasShapeFunctions {
    
    VertexShapeFunction getVertexShapeFunction();
    
    EdgeShapeFunction getEdgeShapeFunction();

}
