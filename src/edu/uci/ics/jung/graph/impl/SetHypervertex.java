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

import java.util.HashSet;

/**
 * An implementation of <code>Hypervertex</code> that maintains
 * independent <code>Set</code>s of incident edges and neighbors.
 * This allows query methods (e.g. <code>isIncident</code>) to 
 * execute in O(1) time, but <code>findEdge</code> still requires
 * time proportional to the degree of this vertex.
 * 
 * @author Joshua O'Madadhain
 */
public class SetHypervertex extends CollectionHypervertex 
{
    public SetHypervertex()
    {
//        super();
        initialize();
    }
    
    protected void initialize()
    {
        incident_edges = new HashSet();
        super.initialize();
    }
}
