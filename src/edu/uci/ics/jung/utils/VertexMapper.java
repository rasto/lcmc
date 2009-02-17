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

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * Provides a general interface for mappings from one vertex to another.
 *  
 * @author Joshua O'Madadhain
 */
public interface VertexMapper
{
    public ArchetypeVertex getMappedVertex(ArchetypeVertex v);
}
