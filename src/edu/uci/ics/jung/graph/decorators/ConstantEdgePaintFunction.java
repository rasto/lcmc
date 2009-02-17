/*
 * Created on Mar 10, 2005
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.decorators;

import java.awt.Paint;

import edu.uci.ics.jung.graph.Edge;

/**
 * Provides the same <code>Paint</code> for any specified edge.
 * 
 * @author Tom Nelson - RABA Technologies
 * @author Joshua O'Madadhain
 */
public class ConstantEdgePaintFunction implements EdgePaintFunction {

    protected Paint draw_paint;
    protected Paint fill_paint;

    /**
     * Sets both draw and fill <code>Paint</code> instances to <code>paint</code>.
     */
    public ConstantEdgePaintFunction(Paint paint) 
    {
        this.draw_paint = paint;
        this.fill_paint = paint;
    }

    /**
     * Sets the drawing <code>Paint</code> to <code>draw_paint</code> and
     * the filling <code>Paint</code> to <code>fill_paint</code>.
     */
    public ConstantEdgePaintFunction(Paint draw_paint, Paint fill_paint) 
    {
        this.draw_paint = draw_paint;
        this.fill_paint = fill_paint;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgePaintFunction#getDrawPaint(edu.uci.ics.jung.graph.Edge)
     */
    public Paint getDrawPaint(Edge e) {
        return draw_paint;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgePaintFunction#getFillPaint(edu.uci.ics.jung.graph.Edge)
     */
    public Paint getFillPaint(Edge e) {
        return fill_paint;
    }
}
