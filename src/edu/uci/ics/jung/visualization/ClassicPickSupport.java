/*
 * Created on Apr 11, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import edu.uci.ics.jung.graph.Edge;

/**
 * <code>PickSupport</code> implementation that emulates the picking behavior
 * of versions of <code>VisualizationViewer</code> prior to version 1.6.
 * (<code>VisualizationViewer</code> still has this behavior by default, but
 * the picking behavior can now be changed.)
 * 
 * @see ShapePickSupport
 * 
 * @author Tom Nelson
 * @author Joshua O'Madadhain
 */
class ClassicPickSupport extends RadiusPickSupport implements PickSupport {
    
    public ClassicPickSupport()
    {
        super();
    }
    
    /** 
     * @return null ClassicPickSupport does not do edges
     */
    public Edge getEdge(double x, double y) {
        return null;
    }
}