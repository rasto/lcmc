/*
 * Created on Apr 3, 2004
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

import edu.uci.ics.jung.graph.Vertex;


/**
 * This general interface defines a factory to produce vertices. 
 * This is an alternative to explicilty creating vertices. 
 * 
 * @author Joshua O'Madadhain
 */
public interface VertexGenerator
{
    public static final String TAG = "edu.uci.ics.jung.utils.VertexGenerator";
    
    public Vertex create();
}
