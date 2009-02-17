/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on Jul 6, 2004
 */
package edu.uci.ics.jung.utils;

import edu.uci.ics.jung.graph.ArchetypeGraph;
import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A <code>VertexMapper</code> implementation that respects the vertex
 * equality reflected by <code>ArchetypeVertex.getEqualVertex</code>.
 *  
 * @author Joshua O'Madadhain
 */
public class CopyVertexMapper extends AbstractVertexMapper
{
    public CopyVertexMapper(ArchetypeGraph dest)
    {
        super(dest);
    }
    
    /**
     * @see VertexMapper#getMappedVertex(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public ArchetypeVertex getMappedVertex(ArchetypeVertex v)
    {
        return v.getEqualVertex(dest);
    }
}
