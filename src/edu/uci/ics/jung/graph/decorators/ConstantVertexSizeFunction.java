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

import edu.uci.ics.jung.graph.Vertex;


public class ConstantVertexSizeFunction implements VertexSizeFunction
{
    private int size;
    
    public ConstantVertexSizeFunction(int size)
    {
        this.size = size;
    }
    
    public int getSize(Vertex v)
    {
        return size;
    }
}