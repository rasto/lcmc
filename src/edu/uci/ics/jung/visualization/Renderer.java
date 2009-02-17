/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.visualization;

import java.awt.Graphics;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;

/**
 * Draws individual vertices and
 * edges on a display. Given a <tt>Graphics</tt> context, it paints
 * a <tt>Vertex</tt> or an <tt>Edge</tt> appropriately.
 * <p>
 * Users must provide an appropriate Renderer, if they are rendering
 * to AWT / Swing. (Presumably, a similar mechanism might be built
 * for other Graphics types; however, this class and its implementations
 * are all Swing specific).
 * <p>
 * The <tt>{@link edu.uci.ics.jung.visualization.PluggableRenderer PluggableRenderer}</tt>
 * is a good starting <code>Renderer</code> for off-the shelf use.
 * <p>
 * In general, one can expect that <code>paintVertex</code> and <code>paintEdge</code> will
 * only be called with visible edges and visible vertices.
 * 
 * @author danyelf
 */
public interface Renderer {

	public void paintVertex(Graphics g, Vertex v, int x, int y);
	public void paintEdge(Graphics g, Edge e, int x1, int y1, int x2, int y2);

	/**
	 * This call allows a Renderer to ask whether a vertex is picked
	 * or not.
     * @deprecated Use an independent PickedInfo instance.
	 */
	public void setPickedKey( PickedInfo pk );

//	/**
//	 * wiggleRoom specifies how much space around a vertex the bounding box
//	 * must be (for limited-screen refresh, and so on). if wiggleRoomX returns
//	 * 0, then the bounding box will enclose the vertices tightly; the larger
//	 * wiggleRoomX is, the larger the bounding box will be in the X direction.
//	 * If wiggleRoomX returns -1, than there will be no bounding box; the screen
//	 * will refresh entire.
//	 */
////	public int wiggleRoomX();
//
//	/**
//	 * wiggleRoom specifies how much space around a vertex the bounding box
//	 * must be (for limited-screen refresh, and so on). if wiggleRoomY returns
//	 * 0, then the bounding box will enclose the vertices vertically
//	 * tightly; the larger wiggleRoomY is, the larger the bounding box will be in the X direction.
//	 */
////	public int wiggleRoomY();

}
