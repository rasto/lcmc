/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
/*
 * Created on May 5, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.uci.ics.jung.visualization;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.GraphUtils;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * Implements a pass-through visaulizer that fades out 
 * nodes that have been removed, and fades in nodes that 
 * have appeared. Adds a field FADINGNODEVIZ to the 
 * nodes, which directs the _Renderer to view those nodes
 * as faded.
 * <p>
 * In order to use this class, create a FadingNodeLayout
 * that takes as an arguemnt the Layout you actually wish
 * to use:
 * <pre>
 * 	Layout v= new FadingNodeLayout( 10, new SpringLayout( g ));
 * </pre>
 * In order to operate, this implementation tracks the vertices that
 * are visible before and after each call to <tt>applyFilte</tt>.
 * <ul>
 * <li><em>Nodes that had been visible, but are now filtered out</em>
 * get the field <tt>FADINGNODEVIZ</tt> added, and have the level
 * set to 0.</li>
 * <li><em>Nodes that been filtered out, but are now visible</em>,
 * have the field <tt>FADINGNODEVIZ</tt> removed.</li>
 * </ul>
 * Other nodes are not altered. However, each time that
 * <tt>advancePositions</tt> is called, all nodes that
 * are currently hidden have their fading level incremented.
 * <p>
 * In this documentaiton, code that is labelled as a <tt>passthrough</tt>
 * has no functionality except to pass the data through to the contained
 * layout.
 * <p>
 * Be sure to use a <tt>_Renderer</tt> that knows to pay attention to
 * the Fading information. In particular, it must know that the
 * FADINGNODEVIZ field gives information about the fade level.
 * 
 * @author danyelf
 * @deprecated If you are using this code, PLEASE CONTACT US
 */
public class FadingVertexLayout implements Layout 
{
	/**
	 * @see edu.uci.ics.jung.visualization.Layout#getCurrentSize()
	 */
	public Dimension getCurrentSize() {
		return layout.getCurrentSize();
	}

	AffineTransform transform;
	AffineTransform inverse;
	
	private Dimension currentSize;
	//	private Graph graph;
	private Layout layout;
	private int fadelevels;

	private Set hiddenNodes;

	/**
	 * Adds user data to every vertex in the graph. Initially, no
	 * nodes are hidden.
	 * @param fadelevels	The number of levels through which
	 * 		a vertex should fade once it is removed.
	 * @param l	The layout that is responsible for
	 * 		fading the information.
	 */
	public FadingVertexLayout(int fadelevels, Layout layout) {
		this.fadelevels = fadelevels;
		this.layout = layout;
		this.hiddenNodes = new HashSet();
	}

	/**
	 * A pass-through to the contained Layout
	 */
	public void initialize(Dimension d) {

		layout.initialize(d);

		Graph graph = layout.getGraph();
		for (Iterator iter = graph.getVertices().iterator(); iter.hasNext();) {
			ArchetypeVertex vert = (ArchetypeVertex) iter.next();
			vert.addUserDatum(
				getFadingKey(),
				new FadingVertexLayoutData(),
				UserData.REMOVE);
		}

	}

	public String getStatus() {
		return layout.getStatus();
	}

	/** 
	 * Returns *all* edges. Note that this is unusual: for example
	 * @see edu.uci.ics.jung.visualization.Layout#getVisibleEdges()
	 */
	public Set getVisibleEdges() {
		return layout.getVisibleEdges();
	}

	/**
	 * A pass-through.
	 * @see Layout#getGraph()
	 */
	public Graph getGraph() {
		return layout.getGraph();
	}

	/** 
	 * @deprecated Use PickSupport instead
	 * A pass-through.
	 * @see edu.uci.ics.jung.visualization.Layout#getVertex(double, double)
	 */
	public Vertex getVertex(double x, double y) {
		return layout.getVertex(x, y);
	}

	/** 
	 * @deprecated Use PickSupport instead
	 * A pass-through.
	 * @see edu.uci.ics.jung.visualization.Layout#getVertex(double, double, double)
	 */
	public Vertex getVertex(double x, double y, double maxDistance) {
		return layout.getVertex(x, y, maxDistance);
	}

	/** In addition to being a passthrough, this also advances
	 * the fade function by calling <tt>{@link #tick() tick}</tt>
	 * 
	 * @see edu.uci.ics.jung.visualization.Layout#advancePositions()
	 */
	public void advancePositions() {
		tick();
		layout.advancePositions();
	}

	/**
	* This method advances each node that is fading away.
	* Each vertex's "fade" count is advanced
	* one towards {@link #getMaxLevel() getMaxLevel()}, and then moved
	* outward.
	*/
	protected void tick() {
		Graph graph = layout.getGraph();
		for (Iterator iter = graph.getVertices().iterator(); iter.hasNext();) {
			Vertex av = (Vertex) iter.next();
			FadingVertexLayoutData fnvd =
				(FadingVertexLayoutData) av.getUserDatum(getFadingKey());
			if (fnvd.level < fadelevels) {
				fnvd.level++;
				if (fnvd.isHidden) {
					moveOutward(av, getX(av), getY(av), 1.01);
				}
			}
		}
	}

	public class FadingVertexLayoutData {
		public boolean isHidden = false;
		public int level = 0;
	}

	private static final String FADINGNODEVIZX =
		"edu.uci.ics.jung.FadingNodeLayoutKey";

	private Object key = null;
	public Object getFadingKey() {
		if (key == null)
			key = new Pair(this, FADINGNODEVIZX);
		return key;
	}

	/**
	 * Tracks the changes in the set of visible vertices from the set of
	 * actual vertices. <br>
	 * Vertices that have been REMOVED will be faded out through FADELEVELS
	 * steps; vertices that have been ADDED will be faded in through FADELEVELS
	 * steps. All hidden vertices will be labelled as hidden; all showing
	 * vertices will be labeled as nonhidden.
	 * @see edu.uci.ics.jung.visualization.Layout#applyFilter(edu.uci.ics.jung.graph.Graph)
	 */
	public void applyFilter(Graph g_int) {
		layout.applyFilter(g_int);
		Graph graph = layout.getGraph();
		Set nowShowingNodes =
			GraphUtils.getEqualVertices(g_int.getVertices(), graph);

		int newlyShowing = 0;
		int newlyHidden = 0;

		for (Iterator iter = graph.getVertices().iterator(); iter.hasNext();) {
			ArchetypeVertex vert = (ArchetypeVertex) iter.next();
			if (nowShowingNodes.contains(vert) && hiddenNodes.contains(vert)) {

				// v is now NEWLY SHOWING
				newlyShowing++;
				hiddenNodes.remove(vert);
				FadingVertexLayoutData fnvd =
					(FadingVertexLayoutData) vert.getUserDatum(getFadingKey());
				fnvd.isHidden = false;
				fnvd.level = 0;
				// the node should also be moved to near its neighbors
				moveVertexPrettily((Vertex) vert.getEqualVertex(g_int));
			} else if (
				!nowShowingNodes.contains(vert)
					&& !hiddenNodes.contains(vert)) {
				//				System.out.println(v + " " + v.getClass() );
				newlyHidden++;
				hiddenNodes.add(vert);
				FadingVertexLayoutData fnvd =
					(FadingVertexLayoutData) vert.getUserDatum(getFadingKey());
				fnvd.isHidden = true;
				fnvd.level = 0;
				// the node should gradually surf away from its neighbors
			}
		}
	}

	/**
	 * This code is called when a Vertex is being brought
	 * back onto the page. Currently, it merely examines the
	 * vertex' neighbors, picks an average point, and moves the
	 * vertex nearby, assuming that the neighbors will be placed
	 * correctly.
	 * @param vert
	 */
	protected void moveVertexPrettily(Vertex vert) {
		// vertex neighbors
		Set s = vert.getNeighbors();
		if (s.size() == 0)
			return;
		double x = 0;
		double y = 0;
		// average location
		for (Iterator iter = s.iterator(); iter.hasNext();) {
			Vertex neighbor = (Vertex) iter.next();
			x += layout.getX(neighbor);
			y += layout.getY(neighbor);
		}
		x /= s.size();
		y /= s.size();
		moveOutward(vert, x, y, 0.9);
	}

	/**
	 * Moves a vertex outward, toward the outer edge of the screen
	 * by calling {@link #forceMove(Vertex, double, double) forceMove} on the vertex.
	 *
	 * @param vert
	 * @param x		The desired origin X coordinate 
	 * @param y		The desired origin Y coordinate
	 * @param speed	The speed with which the vertex moves outward
	 */
	protected void moveOutward(Vertex vert, double x, double y, double speed) {
		x -= currentSize.width / 2;
		y -= currentSize.height / 2;
		x *= speed;
		y *= speed;
		x += currentSize.width / 2;
		y += currentSize.height / 2;
		forceMove(vert, x, y);
	}

	/** Passthrough.
	 * @see edu.uci.ics.jung.visualization.Layout#resize(java.awt.Dimension)
	 */
	public void resize(Dimension d) {
		if (currentSize != null) {
			synchronized (currentSize) {
				this.currentSize = d;
			}
		} else {
			this.currentSize = d;
		}
		layout.resize(d);
	}

	/** Passthrough.
	 * @see edu.uci.ics.jung.visualization.Layout#restart()
	 */
	public void restart() {
		layout.restart();
	}

	/** Passthrough. Note that you can't get X and Y of hidden nodes.
	 * @see edu.uci.ics.jung.visualization.Layout#getX(edu.uci.ics.jung.graph.Vertex)
	 */
	public double getX(Vertex vert) {
		return layout.getX(vert);
	}

	/** Passthrough. Note that you can't get X and Y of hidden nodes.
	 * @see edu.uci.ics.jung.visualization.Layout#getX(edu.uci.ics.jung.graph.Vertex)
	 */
	public double getY(Vertex vert) {
		return layout.getY(vert);
	}

	/** 
     * Returns both the visible and the hidden vertices.
     * This function is an exception to the usual behavior of
     * <code>getVisibleVertices</code>.  Where usually only visible
	 * vertices would be passed, this function also passes the
	 * hidden ones, and counts on the _Renderer (or other calling
	 * client) to know what to do with it appropriately.  This is done 
	 * in order to ensure that fading vertices are still shown.
	 * 
	 * @see edu.uci.ics.jung.visualization.Layout#getVisibleVertices()
	 */
	public Set getVisibleVertices() {
		return layout.getGraph().getVertices();
	}

	/**
	 * Static utility function returns the fade level of a 
	 * given vertex. This vertex must be visaulized by a Fading
	 * Node Layout, or this function
	 * will throw an exception.
	 * @param v
	 */
	public int getFadeLevel(Vertex v) {
		return ((FadingVertexLayoutData) v.getUserDatum(getFadingKey())).level;
	}

	/**
	 * Static utility function returns the fade level of a 
	 * given vertex. This vertex must be visaulized by a Fading
	 * Node Layout, or this function
	 * will throw an exception.
	 * @param v
	 */
	public boolean isHidden(Vertex v) {
		return (
			(FadingVertexLayoutData) v.getUserDatum(getFadingKey())).isHidden;
	}

	/** 
     * Passthrough.
	 * @see edu.uci.ics.jung.visualization.Layout#lockVertex(edu.uci.ics.jung.graph.Vertex)
	 */
	public void lockVertex(Vertex vert) {
		layout.lockVertex(vert);

	}

	/** 
     * Passthrough.
	 * @see edu.uci.ics.jung.visualization.Layout#unlockVertex(edu.uci.ics.jung.graph.Vertex)
	 */
	public void unlockVertex(Vertex vert) {
		layout.unlockVertex(vert);
	}
    
    /**
     * Passthrough.
     * @see edu.uci.ics.jung.visualization.Layout#isLocked(Vertex)
     */
    public boolean isLocked(Vertex v)
    {
        return layout.isLocked(v);
    }

	/** Simply passes through the vertex.
	* @see edu.uci.ics.jung.visualization.Layout#forceMove(edu.uci.ics.jung.graph.Vertex, int, int)
	*/
	public void forceMove(Vertex picked, double x, double y) {
		layout.forceMove(picked, x, y);
	}

	/**
	 * Returns the number of levels that vertices fade through.
	 */
	public int getMaxLevel() {
		return fadelevels;
	}

	/** Passthrough.
	 * @see edu.uci.ics.jung.visualization.Layout#isIncremental()
	 */
	public boolean isIncremental() {
		return layout.isIncremental();
	}

	/** Passthrough.
	 * @see edu.uci.ics.jung.visualization.Layout#incrementsAreDone()
	 */
	public boolean incrementsAreDone() {
		return layout.incrementsAreDone();
	}

    /* (non-Javadoc)
     * @see edu.uci.ics.jung.visualization.HasGraphLayout#getGraphLayout()
     */
    public Layout getGraphLayout() {
        return this;
    }

    public Point2D getLocation(ArchetypeVertex v)
    {
        return layout.getLocation(v);
    }

    public Iterator getVertexIterator()
    {
        return layout.getVertexIterator();
    }

}
