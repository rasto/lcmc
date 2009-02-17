/*
 * Created on Jul 22, 2005
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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;

import edu.uci.ics.jung.graph.ArchetypeVertex;

public class VertexLocationUtils
{
    public static VertexLocationFunction scale(VertexLocationFunction vld, double dx, double dy)
    {
        SettableVertexLocationFunction out = new DefaultSettableVertexLocationFunction();
        // get the max x and max y locations
        double max_x = 0;
        double max_y = 0;
        for (Iterator iter = vld.getVertexIterator(); iter.hasNext(); )
        {
            ArchetypeVertex v = (ArchetypeVertex)iter.next();
            Point2D location = vld.getLocation(v);
            max_x = Math.max(max_x, location.getX());
            max_y = Math.max(max_y, location.getY());
        }
        AffineTransform at = AffineTransform.getScaleInstance(dx / max_x, dy / max_y);
        for (Iterator iter = vld.getVertexIterator(); iter.hasNext(); )
        {
            ArchetypeVertex v = (ArchetypeVertex)iter.next();
            Point2D location = vld.getLocation(v);
            Point2D new_location = new Point2D.Double();
            at.transform(location, new_location);
            out.setLocation(v, new_location);
        }
        return out;
    }
}
