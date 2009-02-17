/*
 * Created on Jul 18, 2004
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

import java.awt.BasicStroke;
import java.awt.Stroke;

import edu.uci.ics.jung.graph.Edge;

/**
 * 
 * @author Joshua O'Madadhain
 */
public class ConstantEdgeStrokeFunction implements EdgeStrokeFunction
{
    protected Stroke stroke;
    
    public ConstantEdgeStrokeFunction(Stroke s)
    {
        this.stroke = s;
    }
    
    public ConstantEdgeStrokeFunction(float thickness)
    {
        this.stroke = new BasicStroke(thickness);
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgeStrokeFunction#getStroke(edu.uci.ics.jung.graph.Edge)
     */
    public Stroke getStroke(Edge e)
    {
        return stroke;
    }

}
