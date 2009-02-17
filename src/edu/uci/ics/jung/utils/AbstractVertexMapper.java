/*
 * Copyright (c) 2004, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Jul 6, 2004
 */
package edu.uci.ics.jung.utils;

import edu.uci.ics.jung.graph.ArchetypeGraph;

/**
 * An abstract class based on <code>VertexMapper</code> that specifies
 * a destination graph.
 *  
 * @author Joshua O'Madadhain
 */
public abstract class AbstractVertexMapper implements VertexMapper
{
    protected ArchetypeGraph dest;
    /**
     * 
     */
    public AbstractVertexMapper(ArchetypeGraph dest)
    {
        this.dest = dest;
    }
}
