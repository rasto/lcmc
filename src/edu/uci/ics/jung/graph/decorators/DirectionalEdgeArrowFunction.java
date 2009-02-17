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

import java.awt.Shape;

import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.visualization.ArrowFactory;

/**
 * Returns wedge arrows for undirected edges and notched arrows
 * for directed edges, of the specified dimensions.
 * 
 * @author Joshua O'Madadhain
 */
public class DirectionalEdgeArrowFunction implements EdgeArrowFunction
{
    protected Shape undirected_arrow;
    protected Shape directed_arrow;
    
    public DirectionalEdgeArrowFunction(int length, int width, int notch_depth)
    {
        directed_arrow = ArrowFactory.getNotchedArrow(width, length, notch_depth);
        undirected_arrow = ArrowFactory.getWedgeArrow(width, length);
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgeArrowFunction#getArrow(edu.uci.ics.jung.graph.Edge)
     */
    public Shape getArrow(Edge e)
    {
        if (e instanceof DirectedEdge)
            return directed_arrow;
        else if (e instanceof UndirectedEdge)
            return undirected_arrow;
        else
            throw new IllegalArgumentException("Unrecognized edge type");
    }

}
