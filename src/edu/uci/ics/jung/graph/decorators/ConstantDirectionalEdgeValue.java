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

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.UndirectedEdge;

/**
 * Returns the constructor-specified value for each edge type.
 * 
 * @author Joshua O'Madadhain
 */
public class ConstantDirectionalEdgeValue implements NumberEdgeValue
{
    protected Double undirected_closeness;
    protected Double directed_closeness;

    /**
     * 
     * @param undirected
     * @param directed
     */
    public ConstantDirectionalEdgeValue(double undirected, double directed)
    {
        this.undirected_closeness = new Double(undirected);
        this.directed_closeness = new Double(directed);
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue#getNumber(ArchetypeEdge)
     */
    public Number getNumber(ArchetypeEdge e)
    {
        if (e instanceof UndirectedEdge)
            return undirected_closeness;
        else if (e instanceof DirectedEdge)
            return directed_closeness;
        else
            throw new IllegalArgumentException("Edge type must be DirectedEdge or UndirectedEdge");
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.NumberEdgeValue#setNumber(edu.uci.ics.jung.graph.ArchetypeEdge, java.lang.Number)
     */
    public void setNumber(ArchetypeEdge e, Number n)
    {
        throw new UnsupportedOperationException();
    }

}
