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

/**
 * Returns the values specified by a <code>NumberEdgeValue</code>
 * instance as <code>String</code>s.
 * 
 * @author Joshua O'Madadhain
 */
public class NumberEdgeValueStringer implements EdgeStringer
{
    protected NumberEdgeValue nev;
    protected final static NumberFormat nf = NumberFormat.getInstance();
    
    public NumberEdgeValueStringer(NumberEdgeValue nev)
    {
        this.nev = nev;
    }
    
    /**
     * @see edu.uci.ics.jung.graph.decorators.EdgeStringer#getLabel(ArchetypeEdge)
     */
    public String getLabel(ArchetypeEdge e)
    {
        return nf.format(nev.getNumber(e));
    }

}
