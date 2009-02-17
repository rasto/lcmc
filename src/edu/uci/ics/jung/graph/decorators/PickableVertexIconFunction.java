/*
* Created on Mar 10, 2005
*
* Copyright (c) 2005, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.decorators;

import javax.swing.Icon;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.visualization.PickedInfo;

/**
 * Supplies an Icon for each vertex according to the <code>Icon</code>
 * parameters given in the constructor, so that picked and
 * non-picked vertices can be made to look different.
 */
public class PickableVertexIconFunction implements VertexIconFunction {

    protected Icon icon;
    protected Icon picked_icon;
    protected PickedInfo pi;
    
    /**
     * 
     * @param pi            specifies which vertices report as "picked"
     * @param icon    <code>Icon</code> used to represent vertices
     * @param picked_icon  <code>Icon</code> used to represent picked vertices
     */
    public PickableVertexIconFunction(PickedInfo pi, Icon icon, Icon picked_icon)
    {
        if (pi == null)
            throw new IllegalArgumentException("PickedInfo instance must be non-null");
        this.pi = pi;
        this.icon = icon;
        this.picked_icon = picked_icon;
    }

    /**
     * Returns the appropriate <code>Icon</code>, depending on picked state.
     */
	public Icon getIcon(ArchetypeVertex v) {
        if (pi.isPicked(v))
            return picked_icon;
        else
            return icon;
	}
}
