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

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * A generalized interface is a mechanism for returning (x,y) coordinates 
 * from vertices. In general, most of these methods are used to both control and
 * get information from the layout algorithm.
 * <p>
 * Some of the additional complexity comes from the desire to give
 * allow users to click on particular nodes and "lock" them in place.
 * Therefore, there are calls to query the nodes for the nearest to a
 * particular location, and calls to label a node as locked.
 * <p>
 * Many layout algorithms are incremental, and place nodes based on
 * their previous location. This system supports them; it progresses
 * the visualization only if <tt>isIncremental</tt> is <tt>true</tt>.
 * <p>
 * The Layout concept is also prepared to deal with filtered subgraphs.
 * Once the Layout was constructed with a Graph, it can accept calls to
 * <tt>{@link #applyFilter(Graph) applyFilter(Graph)}</tt>, which set the 
 * set the current graph to a subset. The Layout is responsible for handling
 * this new, smaller set by one of several strategies:
 * <ul>
 * <li>Ignore the subset, and continue to place all the nodes. The renderer will
 * 	only draw the subset.</li>
 * <li>Lay out only the subset.</li>
 * </ul>
 * 
 * @author danyelf
 */
public interface Layout extends VertexLocationFunction {
    
	/**
	 * Initializes fields in the node that may not have
	 * been set during the constructor. Must be called before
	 * the iterations begin.
	 */
	public void initialize( Dimension currentSize);

	/**
	 * Returns the x coordinate of vertex v at this stage in the
	 * iteration.
	 * 
	 * @param v	The vertex being examined
	 * @return		the x coordinate of that vertex
	 */
	public double getX(Vertex v);

	/**
	 * Returns the y coordinate of vertex v at this stage in the
	 * iteration.
	 * 
	 * @param v	The vertex being examined
	 * @return		the y coordinate of that vertex
	 */
	public double getY(Vertex v);
	
	public Point2D getLocation(ArchetypeVertex v);

	/**
	 * Sets this filtered graph to be the applicable graph. It
	 * is an error for the subgraph to not be a subgraph of
	 * the main graph.
	 * @param subgraph	a filtered graph that is a subgraph of the Graph returned by {@link #getGraph() getGraph}
	 */
	public void applyFilter(Graph subgraph);

	/**
	 * Returns the current status of the sytem, or null if there
	 * is no particular status to report. Useful for reporting things
	 * like number of iterations passed, temperature, and so on.
	 * @return	the status, as a string
	 */
	public String getStatus();

	/**
     * Resets the vertex positions to their initial locations.
	 */
	public void restart();

	/**
	 * Finds the closest vertex to an input (x,y) coordinate. Useful for mouse clicks.
	 * @deprecated Use PickSupport instead
	 * @param x		The x coordinate of the input
	 * @param y		The y coordinate of the input
	 * @return		The nearest vertex. It is up to the user to check if it is satisfactorily close.
	 */
	public Vertex getVertex(double x, double y);

	/**
	 * Finds the closest vertex to an input (x,y) coordinate. Useful for mouse clicks.
	 * @deprecated Use PickSupport instead
	*
	 * @param x		The x coordinate of the input
	 * @param y		The y coordinate of the input
	 * @param maxDistance The maximum acceptable distance. Beyond this, vertices are ignored.
	 * @return		The nearest vertex. It is up to the user to check if it is satisfactorily close.
	 */
	public Vertex getVertex(double x, double y, double maxDistance);

	/**
	 * Returns the full graph (the one that was passed in at 
	 * construction time) that this Layout refers to.
	 * 
	 */
	public Graph getGraph();
	
	/**
	 * Resets the size of the visualization. One cannot count on
	 * a Visualizaton knowing its own size correctly until the
	 * following sequence has been called:
	 * <pre>
	 * Layout l = new XXXLayout( g )
	 * l.initialize();
	 * l.resize( this.getSize() );
	 * </pre>
	 * @param d
	 */
	public void resize(Dimension d);

	/**
	 * Advances an incremental visualization.
	 * Many visualizations are incremental--that is, they get better
	 * over time and recalculations. This moves it forward one step.
	 */
	public void advancePositions();

	/**
	 * Indicates whether this visualization has an incremental mode. If
	 * so, it may be good to increment a bunch of times before showing.
	 * If not, the containing program may not wish to call increment.
	 *
	 */
	public boolean isIncremental();

	/**
	 * If this visualization is incremental, tells whether it has
	 * stabilized at a satisfactory spot yet.
	 */
	public boolean incrementsAreDone();

	/**
	 * Sets a flag which fixes this vertex in place.
     * 
	 * @param v	vertex
     * @see #unlockVertex(Vertex)
     * @see #isLocked(Vertex)
	 */
	public void lockVertex(Vertex v);

	/**
	 * Allows this vertex to be moved.
     * 
	 * @param v	vertex
     * @see #lockVertex(Vertex)
     * @see #isLocked(Vertex)
	 */
	public void unlockVertex(Vertex v);

    /**
     * Returns <code>true</code> if the position of vertex <code>v</code>
     * is locked.
     * @see #lockVertex(Vertex)
     * @see #unlockVertex(Vertex)
     */
    public boolean isLocked(Vertex v);

	/**
	 * Forces a node to be moved to location x,y
	 * @param picked
	 * @param x
	 * @param y
	 */
	public void forceMove(Vertex picked, double x, double y);

	/**
	 * Returns all currently showing edges
     * @deprecated Use of this facility is discouraged and will be removed
     * in future.  For an alternative, see the <code>PluggableRenderer</code> method
     * <code>setEdgeIncludePredicate</code>.
	 */
	public Set getVisibleEdges();

	/**
	 * Returns all currently visible vertices
     * @deprecated Use of this facility is discouraged and will be removed
     * in future.  For an alternative, see the <code>PluggableRenderer</code> method
     * <code>setVertexIncludePredicate</code>.
	 */
	public Set getVisibleVertices();

	/**
	 * Returns the current size of the visualization's space.
	 */
	public Dimension getCurrentSize();

}
