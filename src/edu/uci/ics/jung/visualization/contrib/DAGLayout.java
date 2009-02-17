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
 * Created on Dec 4, 2003
 */
package edu.uci.ics.jung.visualization.contrib;

/**
 * @author danyelf
 */
/*
 * Created on 4/12/2003
 *  
 */

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.visualization.Coordinates;
import edu.uci.ics.jung.visualization.SpringLayout;

/**
 * @author John Yesberg
 * 
 * DAGLayout is a layout algorithm which is suitable for tree-like directed
 * acyclic graphs. Parts of it will probably not terminate if the graph is
 * cyclic! The layout will result in directed edges pointing generally upwards.
 * Any vertices with no successors are considered to be level 0, and tend
 * towards the top of the layout. Any vertex has a level one greater than the
 * maximum level of all its successors.
 * 
 * Note: had to make minor access changes to SpringLayout to make this work.
 * FORCE_CONSTANT, LengthFunction, SpringVertexData, and SpringEdgeData were
 * all made "protected".
 */
public class DAGLayout extends SpringLayout {

	protected static final String MINIMUMLEVELKEY = "DAGLayout.minimumLevel";
	// Simpler than the "pair" technique.
	static int graphHeight;
	static int numRoots;
	final double SPACEFACTOR = 1.3;
	// How much space do we allow for additional floating at the bottom.
	final double LEVELATTRACTIONRATE = 0.8;

	/*
	 * A bunch of parameters to help work out when to stop quivering.
	 * 
	 * If the MeanSquareVel(ocity) ever gets below the MSV_THRESHOLD, then we
	 * will start a final cool-down phase of COOL_DOWN_INCREMENT increments. If
	 * the MeanSquareVel ever exceeds the threshold, we will exit the cool down
	 * phase, and continue looking for another opportunity.
	 */
	final double MSV_THRESHOLD = 10.0;
	static double meanSquareVel;
	static boolean stoppingIncrements = false;
	static int incrementsLeft;
	final int COOL_DOWN_INCREMENTS = 200;
	/*
	 * @param g
	 */
	public DAGLayout(Graph g) {
		super(g);
	}

	/*
	 * Each vertex has a minimumLevel. Any vertex with no successors has
	 * minimumLevel of zero. The minimumLevel of any vertex must be strictly
	 * greater than the minimumLevel of its parents. (Vertex A is a parent of
	 * Vertex B iff there is an edge from B to A.) Typically, a vertex will
	 * have a minimumLevel which is one greater than the minimumLevel of its
	 * parent's. However, if the vertex has two parents, its minimumLevel will
	 * be one greater than the maximum of the parents'. We need to calculate
	 * the minimumLevel for each vertex. When we layout the graph, vertices
	 * cannot be drawn any higher than the minimumLevel. The graphHeight of a
	 * graph is the greatest minimumLevel that is used. We will modify the
	 * SpringLayout calculations so that nodes cannot move above their assigned
	 * minimumLevel.
	 */

	/**
	 * setRoot calculates the level of each vertex in the graph. Level 0 is
	 * allocated to any vertex with no successors. Level n+1 is allocated to
	 * any vertex whose successors' maximum level is n.
	 */

	public static void setRoot(Graph g) {
		numRoots = 0;
		Set verts = g.getVertices();
		Iterator iter = verts.iterator();
		Vertex v;
		Set successors;
		while (iter.hasNext()) {
			v = (Vertex) iter.next();
			successors = v.getSuccessors();
			if (successors.size() == 0) {
				setRoot(v);
				numRoots++;
			}
		}
	}

	/**
	 * Set vertex v to be level 0.
	 */

	public static void setRoot(Vertex v) {
		v.setUserDatum(MINIMUMLEVELKEY, new Integer(0), UserData.REMOVE);
		//
		// Iterate through now, setting all the levels.
		propagateMinimumLevel(v);
	}

	/**
	 * A recursive method for allocating the level for each vertex. Ensures
	 * that all predecessors of v have a level which is at least one greater
	 * than the level of v.
	 * 
	 * @param v
	 */

	public static void propagateMinimumLevel(Vertex v) {
		int level = ((Integer) v.getUserDatum(MINIMUMLEVELKEY)).intValue();
		Set predecessors = v.getPredecessors();
		Iterator iter = predecessors.iterator();
		Vertex child; // odd to use predecessors for child, isn't it. Sorry!
		while (iter.hasNext()) {
			child = (Vertex) iter.next();
			int oldLevel, newLevel;
			Object o = child.getUserDatum(MINIMUMLEVELKEY);
			if (o != null)
				oldLevel = ((Integer) o).intValue();
			else
				oldLevel = 0;
			newLevel = Math.max(oldLevel, level + 1);
			child.setUserDatum(
				MINIMUMLEVELKEY,
				new Integer(newLevel),
				UserData.REMOVE);
			if (newLevel > graphHeight)
				graphHeight = newLevel;
			propagateMinimumLevel(child);
		}
	}

	/**
	 * Sets random locations for a vertex within the dimensions of the space.
	 * This overrides the method in AbstractLayout
	 * 
	 * @param coord
	 * @param d
	 */
	protected void initializeLocation(
		Vertex v,
		Coordinates coord,
		Dimension d) {
		//if (v.getUserDatum(MINIMUMLEVELKEY)==null) setRoot(getGraph());
		int level = ((Integer) v.getUserDatum(MINIMUMLEVELKEY)).intValue();
		int minY = (int) (level * d.getHeight() / (graphHeight * SPACEFACTOR));
		double x = Math.random() * d.getWidth();
		double y = Math.random() * (d.getHeight() - minY) + minY;
		coord.setX(x);
		coord.setY(y);
	}

	/**
	 * Had to override this one as well, to ensure that setRoot() is called.
	 */
	protected void initialize_local() {
		for (Iterator iter = getGraph().getEdges().iterator();
			iter.hasNext();
			) {
			Edge e = (Edge) iter.next();
			SpringEdgeData sed = getSpringData(e);
			if (sed == null) {
				sed = new SpringEdgeData(e);
				e.addUserDatum(getSpringKey(), sed, UserData.REMOVE);
			}
			calcEdgeLength(sed, lengthFunction);
		}
		setRoot(getGraph());
	}

	/**
	 * Override the moveNodes() method from SpringLayout. The only change we
	 * need to make is to make sure that nodes don't float higher than the minY
	 * coordinate, as calculated by their minimumLevel.
	 */
	protected void moveNodes() {
		// Dimension d = currentSize;
		double oldMSV = meanSquareVel;
		meanSquareVel = 0;

		synchronized (getCurrentSize()) {

			// int showingNodes = 0;

			for (Iterator i = getVisibleVertices().iterator(); i.hasNext();) {
				Vertex v = (Vertex) i.next();
				if (isLocked(v))
					continue;
				SpringLayout.SpringVertexData vd = getSpringData(v);
				Coordinates xyd = getCoordinates(v);

				int width = getCurrentSize().width;
				int height = getCurrentSize().height;

				// (JY addition: three lines are new)
				int level =
					((Integer) v.getUserDatum(MINIMUMLEVELKEY)).intValue();
				int minY = (int) (level * height / (graphHeight * SPACEFACTOR));
				int maxY =
					level == 0
						? (int) (height / (graphHeight * SPACEFACTOR * 2))
						: height;

				// JY added 2* - double the sideways repulsion.
				vd.dx += 2 * vd.repulsiondx + vd.edgedx;
				vd.dy += vd.repulsiondy + vd.edgedy;

				// JY Addition: Attract the vertex towards it's minimumLevel
				// height.
				double delta = xyd.getY() - minY;
				vd.dy -= delta * LEVELATTRACTIONRATE;
				if (level == 0)
					vd.dy -= delta * LEVELATTRACTIONRATE;
				// twice as much at the top.

				// JY addition:
				meanSquareVel += (vd.dx * vd.dx + vd.dy * vd.dy);

				// keeps nodes from moving any faster than 5 per time unit
				xyd.addX(Math.max(-5, Math.min(5, vd.dx)));
				xyd.addY(Math.max(-5, Math.min(5, vd.dy)));

				if (xyd.getX() < 0) {
					xyd.setX(0);
				} else if (xyd.getX() > width) {
					xyd.setX(width);
				}

				// (JY addition: These two lines replaced 0 with minY)
				if (xyd.getY() < minY) {
					xyd.setY(minY);
					// (JY addition: replace height with maxY)
				} else if (xyd.getY() > maxY) {
					xyd.setY(maxY);
				}

				// (JY addition: if there's only one root, anchor it in the
				// middle-top of the screen)
				if (numRoots == 1 && level == 0) {
					xyd.setX(width / 2);
					//xyd.setY(0);
				}

			}
		}
		//System.out.println("MeanSquareAccel="+meanSquareVel);
		if (!stoppingIncrements
			&& Math.abs(meanSquareVel - oldMSV) < MSV_THRESHOLD) {
			stoppingIncrements = true;
			incrementsLeft = COOL_DOWN_INCREMENTS;
		} else if (
			stoppingIncrements
				&& Math.abs(meanSquareVel - oldMSV) <= MSV_THRESHOLD) {
			incrementsLeft--;
			if (incrementsLeft <= 0)
				incrementsLeft = 0;
		}
	}

	/**
	 * Override incrementsAreDone so that we can eventually stop.
	 */
	public boolean incrementsAreDone() {
		if (stoppingIncrements && incrementsLeft == 0)
			return true;
		else
			return false;
	}

	/**
	 * Override forceMove so that if someone moves a node, we can re-layout
	 * everything.
	 */
	public void forceMove(Vertex picked, int x, int y) {
		Coordinates coord = getCoordinates(picked);
		coord.setX(x);
		coord.setY(y);
		stoppingIncrements = false;
	}

	/**
	 * Overridden relaxEdges. This one reduces the effect of edges between
	 * greatly different levels.
	 *  
	 */

	protected void relaxEdges() {
		for (Iterator i = getVisibleEdges().iterator(); i.hasNext();) {
			Edge e = (Edge) i.next();

			Vertex v1 = getAVertex(e);
			Vertex v2 = e.getOpposite(v1);

			Point2D p1 = getLocation(v1);
			Point2D p2 = getLocation(v2);
			double vx = p1.getX() - p2.getX();
			double vy = p1.getY() - p2.getY();
			double len = Math.sqrt(vx * vx + vy * vy);

			// JY addition.
			int level1 =
				((Integer) v1.getUserDatum(MINIMUMLEVELKEY)).intValue();
			int level2 =
				((Integer) v2.getUserDatum(MINIMUMLEVELKEY)).intValue();

			double desiredLen = getLength(e);
			// desiredLen *= Math.pow( 1.1, (v1.degree() + v2.degree()) );

			// round from zero, if needed [zero would be Bad.].
			len = (len == 0) ? .0001 : len;

			// force factor: optimal length minus actual length,
			// is made smaller as the current actual length gets larger.
			// why?

			// System.out.println("Desired : " + getLength( e ));
			double f = force_multiplier * (desiredLen - len) / len;

			f = f * Math.pow(stretch / 100.0, (v1.degree() + v2.degree() - 2));

			// JY addition. If this is an edge which stretches a long way,
			// don't be so concerned about it.
			if (level1 != level2)
				f = f / Math.pow(Math.abs(level2 - level1), 1.5);

			// f= Math.min( 0, f );

			// the actual movement distance 'dx' is the force multiplied by the
			// distance to go.
			double dx = f * vx;
			double dy = f * vy;
			SpringVertexData v1D, v2D;
			v1D = getSpringData(v1);
			v2D = getSpringData(v2);

			SpringEdgeData sed = getSpringData(e);
			sed.f = f;

			v1D.edgedx += dx;
			v1D.edgedy += dy;
			v2D.edgedx += -dx;
			v2D.edgedy += -dy;
		}
	}

}
