/*
 * Created on Jul 16, 2004
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

import java.awt.Color;

import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.visualization.PickedInfo;


public class ConstantVertexColorFunction implements VertexColorFunction
{
    protected Color fore_color;
    protected Color back_color;
    protected Color picked_color;
    protected PickedInfo pi;
    
    public ConstantVertexColorFunction(PickedInfo pi, Color fore_color, Color back_color, Color picked_color)
    {
        this.pi = pi;
        this.fore_color = fore_color;
        this.back_color = back_color;
        this.picked_color = picked_color;
    }

    public Color getForeColor(Vertex v)
    {
        return fore_color;
    }

    public Color getBackColor(Vertex v)
    {
        if (pi.isPicked(v))
            return picked_color;
        else
            return back_color;
    }
}