/*
 * Created on Apr 26, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.impl;

import java.util.LinkedList;

/**
 * An implementation of <code>Hypervertex</code> that stores its
 * incident edges as a <code>List</code> internally.  This is
 * space-efficient, but most methods require time proportional
 * to the degree of the vertex, and some (<code>findEdge</code>)
 * may take more.
 * 
 * @author Joshua O'Madadhain
 */
public class ListHypervertex extends CollectionHypervertex 
{
    /**
     * 
     */
    public ListHypervertex()
    {
//        super();
//        incident_edges = new LinkedList();
        initialize();
    }
    
    protected void initialize()
    {
        incident_edges = new LinkedList();
        super.initialize();
    }
}
