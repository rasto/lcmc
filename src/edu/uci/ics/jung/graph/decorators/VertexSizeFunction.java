/*
 * Created on Jun 22, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.decorators;

import edu.uci.ics.jung.graph.Vertex;

/**
 * Returns the characteristic size at which the symbol for this
 * vertex should be rendered.  For example:
 * <ul>
 * <li/>circles: diameter
 * <li/>regular polygons: length of side
 * <li/>rectangles, ellipses: length of longest axis (side)
 * </ul>
 * 
 * <p>This size may vary for different vertices in the same graph.</p>
 * 
 * @author Joshua O'Madadhain
 */
public interface VertexSizeFunction
{
    public int getSize(Vertex v);
}
