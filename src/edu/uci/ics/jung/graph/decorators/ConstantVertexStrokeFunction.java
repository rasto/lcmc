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

import edu.uci.ics.jung.graph.Vertex;

/**
 * 
 * @author Joshua O'Madadhain
 */
public class ConstantVertexStrokeFunction implements VertexStrokeFunction
{
    protected Stroke stroke;
    
    public ConstantVertexStrokeFunction(float thickness)
    {
        this.stroke = new BasicStroke(thickness);
    }

    public ConstantVertexStrokeFunction(Stroke s)
    {
        this.stroke = s;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.VertexStrokeFunction#getStroke(edu.uci.ics.jung.graph.Vertex)
     */
    public Stroke getStroke(Vertex v)
    {
        return stroke;
    }

}
