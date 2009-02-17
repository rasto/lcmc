/*
 * Created on Apr 8, 2005
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
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.geom.Point2D;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.visualization.HasGraphLayout;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.transform.LayoutTransformer;

/**
 * Creates <code>GradientPaint</code> instances which can be used
 * to paint an <code>Edge</code>.  For <code>DirectedEdge</code>s, 
 * the color will blend from <code>c1</code> (source) to 
 * <code>c2</code> (destination); for <code>UndirectedEdge</code>s,
 * the color will be <code>c1</code> at each end and <code>c2</code>
 * in the middle.
 * 
 * @author Joshua O'Madadhain
 */
public class GradientEdgePaintFunction extends AbstractEdgePaintFunction
{
    protected Color c1;
    protected Color c2;
    HasGraphLayout vv;
    LayoutTransformer transformer;
    
    public GradientEdgePaintFunction(Color c1, Color c2, 
            HasGraphLayout vv, LayoutTransformer transformer)
    {
        this.c1 = c1;
        this.c2 = c2;
        this.vv = vv;
        this.transformer = transformer;
    }
    
    public Paint getDrawPaint(Edge e)
    {
        Layout layout = vv.getGraphLayout();
        Pair p = e.getEndpoints();
        Vertex b = (Vertex)p.getFirst();
        Vertex f = (Vertex)p.getSecond();
        Point2D pb = transformer.layoutTransform(layout.getLocation(b));
        Point2D pf = transformer.layoutTransform(layout.getLocation(f));
        float xB = (float) pb.getX();
        float yB = (float) pb.getY();
        float xF = (float) pf.getX();
        float yF = (float) pf.getY();
        if (e instanceof UndirectedEdge ) 
        {
            xF = (xF + xB) / 2;
            yF = (yF + yB) / 2;
        }
        if(xB == xF && yB == yF)
        {
        	// hack so loop edges don't 'vanish'
        	xB -= 10;
        	yB -= 10;
        	xF += 10;
        	yF += 10;
        }

        return new GradientPaint(xB, yB, getColor1(e), xF, yF, getColor2(e), true);
    }
    
    /**
     * Returns <code>c1</code>.  Subclasses may override
     * this method to enable more complex behavior (e.g., for
     * picked edges).
     */
    protected Color getColor1(Edge e)
    {
        return c1;
    }

    /**
     * Returns <code>c2</code>.  Subclasses may override
     * this method to enable more complex behavior (e.g., for
     * picked edges).
     */
    protected Color getColor2(Edge e)
    {
        return c2;
    }
}
