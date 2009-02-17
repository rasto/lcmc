/*
 * Copyright (c) 2005, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 *
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 *
 * Created on Aug 1, 2005
 */

package edu.uci.ics.jung.graph.decorators;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import edu.uci.ics.jung.graph.ArchetypeVertex;

/**
 * A simple, stateful VertexIconFunction.
 * Stores icons in a Map keyed on the Vertex
 * 
 * @author Tom Nelson - RABA Technologies
 *
 *
 */
public class DefaultVertexIconFunction implements VertexIconFunction {
     
    /**
     * icon storage
     */
     protected Map iconMap = new HashMap();

     /**
      * Returns the icon storage as a <code>Map</code>.
      */
    public Map getIconMap() {
		return iconMap;
	}

    /**
     * Sets the icon storage to the specified <code>Map</code>.
     */
	public void setIconMap(Map iconMap) {
		this.iconMap = iconMap;
	}

    /**
     * Returns the <code>Icon</code> associated with <code>v</code>.
     */
	public Icon getIcon(ArchetypeVertex v) {
		return (Icon)iconMap.get(v);
	}
}
