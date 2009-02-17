/*
 * Created on Jun 22, 2004
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

import edu.uci.ics.jung.graph.Vertex;

/**
 * 
 * @author Joshua O'Madadhain
 */
public interface VertexAspectRatioFunction
{
    public float getAspectRatio(Vertex v);
}
