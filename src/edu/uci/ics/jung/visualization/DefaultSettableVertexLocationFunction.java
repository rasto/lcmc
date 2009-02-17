/*
 * Created on Jul 21, 2005
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A <code>Map</code>-based implementation of 
 * <code>SettableVertexLocationFunction</code>.
 * 
 * @author Joshua O'Madadhain
 */
public class DefaultSettableVertexLocationFunction implements
        SettableVertexLocationFunction
{
    protected Map v_locations;
    protected boolean normalized;
    
    public DefaultSettableVertexLocationFunction()
    {
        v_locations = new HashMap();
    }
    
    public DefaultSettableVertexLocationFunction(VertexLocationFunction vlf) {
        v_locations = new HashMap();
        for(Iterator iterator=vlf.getVertexIterator(); iterator.hasNext(); ) {
            ArchetypeVertex v = (ArchetypeVertex)iterator.next();
            v_locations.put(v, vlf.getLocation(v));
        }
    }
    
    public void setLocation(ArchetypeVertex v, Point2D location)
    {
        v_locations.put(v, location);
    }
    
    public Point2D getLocation(ArchetypeVertex v)
    {
        return (Point2D)v_locations.get(v);
    }

    public void reset()
    {
        v_locations.clear();
    }
    
    public Iterator getVertexIterator()
    {
        return v_locations.keySet().iterator();
    }
}
