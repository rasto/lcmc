/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Apr 16, 2005
 */

package edu.uci.ics.jung.visualization;

/**
 * Interface to tag classes that can provide a graph
 * Layout to a caller. VisualizationViewer implements this
 * and thru it, will always provide the most up-to-date
 * Layout to a caller. This is important in cases where
 * the Layout has been changed by a previous action.
 * 
 * @author Tom Nelson - RABA Technologies
 */
public interface HasGraphLayout {
    
    Layout getGraphLayout();

}
