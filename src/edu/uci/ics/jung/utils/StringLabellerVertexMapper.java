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
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;

/**
 * Maps one vertex to another based on their common label in specified
 * <code>StringLabeller</code> instances.
 *  
 * @author Joshua O'Madadhain
 */
public class StringLabellerVertexMapper extends AbstractVertexMapper
{
    protected StringLabeller sl_d;
    protected StringLabeller sl_s;
    
    public StringLabellerVertexMapper(ArchetypeGraph dest)
    {
        super(dest);
        this.sl_d = StringLabeller.getLabeller((Graph)dest);
        this.sl_s = null;
    }

    public StringLabellerVertexMapper(StringLabeller sl_src, 
        StringLabeller sl_dest)
    {
        super(null);
        this.sl_d = sl_dest;
        this.sl_s = sl_src;
    }
    
    /**
     * see VertexMapper#getMappedVertex(edu.uci.ics.jung.graph.ArchetypeVertex)
     */
    public ArchetypeVertex getMappedVertex(ArchetypeVertex v)
    {
        if (sl_s == null)
            sl_s = StringLabeller.getLabeller((Graph)v.getGraph());
        String label = sl_s.getLabel((Vertex)v);
        return sl_d.getVertex(label);
    }
}
