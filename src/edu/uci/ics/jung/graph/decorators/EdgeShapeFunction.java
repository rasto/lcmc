/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 * 
 * Created on March 10, 2005
 */
package edu.uci.ics.jung.graph.decorators;

import java.awt.Shape;

import edu.uci.ics.jung.graph.Edge;

/**
 * An interface for decorators that return a 
 * <code>Shape</code> for a specified edge.
 *  
 * @author Tom Nelson
 */
public interface EdgeShapeFunction {

    /**
     * Returns the <code>Shape</code> associated with <code>e</code>.
     */
    Shape getShape(Edge e);
 }
