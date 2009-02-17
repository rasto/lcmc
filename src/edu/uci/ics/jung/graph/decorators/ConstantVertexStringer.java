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

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * Returns the specified label for all vertices.  Useful for
 * specifying "no label".
 * 
 * @author Joshua O'Madadhain
 */
public class ConstantVertexStringer implements VertexStringer
{
    protected String label;
    
    public ConstantVertexStringer(String label) 
    {
        this.label = label;
    }

    /**
     * @see edu.uci.ics.jung.graph.decorators.VertexStringer#getLabel(ArchetypeVertex)
     */
    public String getLabel(ArchetypeVertex v)
    {
        return label;
    }
}
