/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 23, 2005
 */

package edu.uci.ics.jung.visualization.subLayout;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.LayoutDecorator;

/**
 * Extends the base decorator class and overrides methods to 
 * cause the location methods to check with the sublayouts
 * for location information
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class SubLayoutDecorator extends LayoutDecorator {

    final protected Collection subLayouts = new LinkedHashSet();
    
    public SubLayoutDecorator(Layout delegate) {
        super(delegate);
    }
    
    public void addSubLayout(SubLayout subLayout) {
        subLayouts.add(subLayout);
        fireStateChanged();
    }
    
    public boolean removeSubLayout(SubLayout subLayout) {
        boolean wasThere = subLayouts.remove(subLayout);
        fireStateChanged();
        return wasThere;
    }
    
    public void removeAllSubLayouts() {
        subLayouts.clear();
        fireStateChanged();
    }
    
    protected Point2D getLocationInSubLayout(ArchetypeVertex v) {
        Point2D location = null;
        for(Iterator iterator=subLayouts.iterator(); iterator.hasNext(); ) {
            SubLayout subLayout = (SubLayout)iterator.next();
            location = subLayout.getLocation(v);
            if(location != null) {
                break;
            }
        }
        return location;
    }
    
    public Point2D getLocation(ArchetypeVertex v) {
        Point2D p = getLocationInSubLayout(v);
        if(p != null) {
            return p;
        } else {
            return super.getLocation(v);
        }
    }
    
    public void forceMove(Vertex picked, double x, double y) {
        Point2D p = getLocationInSubLayout(picked);
        if(p != null) {
            p.setLocation(x, y);
        } else {
            super.forceMove(picked, x, y);
        }
        fireStateChanged();
    }

}
