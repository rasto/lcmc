/*
 * Created on Apr 28, 2005
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
 * An implementation of <code>Hyperedge</code> that stores its
 * list of incident vertices internally as a <code>List</code>.
 * 
 * @author Joshua O'Madadhain
 */
public class ListHyperedge extends CollectionHyperedge
{
    public ListHyperedge()
    {
        initialize();
//        this(new HashSet());
    }
    
    protected void initialize()
    {
        vertices = new LinkedList();
        super.initialize();
    }
}
