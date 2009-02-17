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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;
import edu.uci.ics.jung.graph.ArchetypeVertex;

public class RandomVertexLocationDecorator implements VertexLocationFunction
{
    RandomEngine rand;
    Map v_locations = new HashMap();
    Dimension dim;
    
    public RandomVertexLocationDecorator(Dimension d) 
    {
        this.rand = new DRand((int)(new Date().getTime()));
        this.dim = d;
    }
    
    public RandomVertexLocationDecorator(Dimension d, int seed)
    {
        this.rand = new DRand(seed);
        this.dim = d;
    }
    
    /**
     * Resets all vertex locations returned by <code>getLocation</code>
     * to new (random) locations.
     */
    public void reset()
    {
        v_locations.clear();
    }
    
    public Point2D getLocation(ArchetypeVertex v)
    {
        Point2D location = (Point2D)v_locations.get(v);
        if (location == null)
        {
            location = new Point2D.Double(rand.nextDouble() * dim.width, rand.nextDouble() * dim.height);
            v_locations.put(v, location);
        }
        return location;
    }

    public Iterator getVertexIterator()
    {
        return v_locations.keySet().iterator();
    }
}
