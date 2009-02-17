/*
 * Created on Apr 5, 2005
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

import java.awt.BasicStroke;
import java.awt.Stroke;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction;
import edu.uci.ics.jung.graph.decorators.EdgeThicknessFunction;

/**
 * Converts an <code>EdgeThicknessFunction</code> into an <code>EdgeStrokeFunction</code>.
 * @author Joshua O'Madadhain
 */
public class EdgeThicknessToEdgeStrokeFunctionConverter implements
        EdgeStrokeFunction
{
    protected EdgeThicknessFunction etf;
    
    /**
     * 
     */
    public EdgeThicknessToEdgeStrokeFunctionConverter(EdgeThicknessFunction etf)
    {
        this.etf = etf;
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction#getStroke(edu.uci.ics.jung.graph.Edge)
     */
    public Stroke getStroke(Edge e)
    {
        return new BasicStroke(etf.getEdgeThickness(e));
    }
}
