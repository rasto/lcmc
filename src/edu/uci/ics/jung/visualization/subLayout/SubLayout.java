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

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * 
 * A special purpose helper for a Layout, that will
 * provide locations foe Vertices. See CircularCluster
 * and ClusteringLayout for an example of its use.
 * 
 * @author Tom Nelson - RABA Technologies
 */
public interface SubLayout {
    
    Point2D getLocation(ArchetypeVertex v);

}
