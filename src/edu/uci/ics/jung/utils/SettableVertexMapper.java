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
package edu.uci.ics.jung.utils;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * An interface for user-mutable <code>VertexMapper</code>s.
 * 
 * @author Joshua O'Madadhain
 */
public interface SettableVertexMapper extends VertexMapper
{
    public void map(ArchetypeVertex v1, ArchetypeVertex v2);
}
