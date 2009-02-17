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

import edu.uci.ics.jung.visualization.VertexShapeFactory;



/**
 * 
 * @author Joshua O'Madadhain
 */
public abstract class AbstractVertexShapeFunction implements SettableVertexShapeFunction
{
    protected VertexSizeFunction vsf;
    protected VertexAspectRatioFunction varf;
    protected VertexShapeFactory factory;
    public final static int DEFAULT_SIZE = 8;
    public final static float DEFAULT_ASPECT_RATIO = 1.0f;
    
    public AbstractVertexShapeFunction(VertexSizeFunction vsf, VertexAspectRatioFunction varf)
    {
        this.vsf = vsf;
        this.varf = varf;
        factory = new VertexShapeFactory(vsf, varf);
    }

    public AbstractVertexShapeFunction()
    {
        this(new ConstantVertexSizeFunction(DEFAULT_SIZE), 
                new ConstantVertexAspectRatioFunction(DEFAULT_ASPECT_RATIO));
    }
    
    public void setSizeFunction(VertexSizeFunction vsf)
    {
        this.vsf = vsf;
        factory = new VertexShapeFactory(vsf, varf);
    }
    
    public void setAspectRatioFunction(VertexAspectRatioFunction varf)
    {
        this.varf = varf;
        factory = new VertexShapeFactory(vsf, varf);
    }
}
