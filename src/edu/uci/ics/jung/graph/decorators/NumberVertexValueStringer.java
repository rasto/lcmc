/*
 * Created on Nov 7, 2004
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

import java.text.NumberFormat;

import edu.uci.ics.jung.graph.ArchetypeEdge;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * Returns the values specified by a <code>NumberVertexValue</code>
 * instance as <code>String</code>s.
 * 
 * @author Joshua O'Madadhain
 */
public class NumberVertexValueStringer implements VertexStringer
{
    protected NumberVertexValue nvv;
    protected final static NumberFormat nf = NumberFormat.getInstance();
    
    public NumberVertexValueStringer(NumberVertexValue nev)
    {
        this.nvv = nev;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgeStringer#getLabel(ArchetypeEdge)
     */
    public String getLabel(ArchetypeVertex v)
    {
        return nf.format(nvv.getNumber(v));
    }
}
