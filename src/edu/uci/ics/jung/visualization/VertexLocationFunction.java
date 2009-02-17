/*
 * Created on Jul 19, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.visualization;

import java.awt.geom.Point2D;
import java.util.Iterator;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * An interface for classes that return a location for
 * an <code>ArchetypeVertex</code>.
 * 
 * @author Joshua O'Madadhain
 */
public interface VertexLocationFunction
{
    public Point2D getLocation(ArchetypeVertex v);
    
    public Iterator getVertexIterator();
}
