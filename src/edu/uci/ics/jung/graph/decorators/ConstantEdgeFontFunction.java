/*
 * Created on Oct 21, 2004
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

import java.awt.Font;

import edu.uci.ics.jung.graph.Edge;

/**
 * 
 * @author Joshua O'Madadhain
 */
public class ConstantEdgeFontFunction implements EdgeFontFunction
{
    protected Font font;
    
    public ConstantEdgeFontFunction(Font f)
    {
        this.font = f;
    }
    
    public Font getFont(Edge e)
    {
        return this.font;
    }
}
