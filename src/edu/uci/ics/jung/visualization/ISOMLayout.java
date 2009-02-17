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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.UserData;

/**
 * Implements a self-organizing map layout algorithm, based on Meyer's
 * self-organizing graph methods.
 * 
 * @author Yan Biao Boey
 */
public class ISOMLayout extends AbstractLayout {

	private static final Object ISOM_KEY =
		"edu.uci.ics.jung.ISOM_Visualization_Key";

	private Object key = null;
	public Object getIsomKey() {
		if (key == null)
			key = new Pair(this, ISOM_KEY);
		return key;
	}

	private int maxEpoch;
	private int epoch;

	private int radiusConstantTime;
	private int radius;
	private int minRadius;

	private double adaption;
	private double initialAdaption;
	private double minAdaption;
    
    protected GraphElementAccessor elementAccessor;

//	private double factor;
	private double coolingFactor;

	//private double temperature;
	//private int initialJumpRadius;
	//private int jumpRadius;

	//private int delay;

	//private ISOMVertexData temp;
	private Vector queue;
	private String status = null;
	
	/**
	 * Returns the current number of epochs and execution status, as a string.
	 */
	public String getStatus() {
		return status;
	}

//	private boolean trace;
//	private boolean done;

	public ISOMLayout(Graph g) {
		super(g);
        elementAccessor = new RadiusGraphElementAccessor(this);
		queue = new Vector();
//		trace = false;
	}

	protected void initialize_local() {
//		done = false;

		maxEpoch = 2000;
		epoch = 1;

		radiusConstantTime = 100;
		radius = 5;
		minRadius = 1;

		initialAdaption = 90.0D / 100.0D;
		adaption = initialAdaption;
		minAdaption = 0;

		//factor = 0; //Will be set later on
		coolingFactor = 2;

		//temperature = 0.03;
		//initialJumpRadius = 100;
		//jumpRadius = initialJumpRadius;

		//delay = 100;
	}
	
	/**
	 * (non-Javadoc)
	 * @see edu.uci.ics.jung.visualization.AbstractLayout#initialize_local_vertex(edu.uci.ics.jung.graph.Vertex)
	 */
	protected void initialize_local_vertex(Vertex v) {
		ISOMVertexData vd = getISOMVertexData(v);
		if (vd == null) {
			vd = new ISOMVertexData();
			v.addUserDatum(getIsomKey(), vd, UserData.REMOVE);
		}
		vd.visited = false;
	}

	/**
	* Advances the current positions of the graph elements.
	*/
	public void advancePositions() {
		status = "epoch: " + epoch + "; ";
		if (epoch < maxEpoch) {
			adjust();
			updateParameters();
			status += " status: running";

		} else {
			status += "adaption: " + adaption + "; ";
			status += "status: done";
//			done = true;
		}
	}

	ISOMVertexData tempISOM;
	Coordinates tempXYD;

	private synchronized void adjust() {
		//Generate random position in graph space
		tempISOM = new ISOMVertexData();
		tempXYD = new Coordinates();

		// creates a new XY data location
		tempXYD.setX(10 + Math.random() * getCurrentSize().getWidth());
		tempXYD.setY(10 + Math.random() * getCurrentSize().getHeight());

		//Get closest vertex to random position
		Vertex winner = elementAccessor.getVertex(tempXYD.getX(), tempXYD.getY());

		while(true) {
		    try {
		        for (Iterator iter = getVisibleVertices().iterator();
		        iter.hasNext();
		        ) {
		            Vertex v = (Vertex) iter.next();
		            ISOMVertexData ivd = getISOMVertexData(v);
		            ivd.distance = 0;
		            ivd.visited = false;
		        }
		        break;
		    } catch(ConcurrentModificationException cme) {}
        }
		adjustVertex(winner);
	}

	private synchronized void updateParameters() {
		epoch++;
		double factor = Math.exp(-1 * coolingFactor * (1.0 * epoch / maxEpoch));
		adaption = Math.max(minAdaption, factor * initialAdaption);
		//jumpRadius = (int) factor * jumpRadius;
		//temperature = factor * temperature;
		if ((radius > minRadius) && (epoch % radiusConstantTime == 0)) {
			radius--;
		}
	}

	private synchronized void adjustVertex(Vertex v) {
		queue.removeAllElements();
		ISOMVertexData ivd = getISOMVertexData(v);
		ivd.distance = 0;
		ivd.visited = true;
		queue.add(v);
		Vertex current;

		while (!queue.isEmpty()) {
			current = (Vertex) queue.remove(0);
			ISOMVertexData currData = getISOMVertexData(current);
			Coordinates currXYData = getCoordinates(current);

			double dx = tempXYD.getX() - currXYData.getX();
			double dy = tempXYD.getY() - currXYData.getY();
			double factor = adaption / Math.pow(2, currData.distance);

			currXYData.addX(factor * dx);
			currXYData.addY(factor * dy);

			if (currData.distance < radius) {
			    Set s = current.getNeighbors();
			    while(true) {
			        try {
			            for (Iterator iter = s.iterator(); iter.hasNext();) {
			                Vertex child = (Vertex) iter.next();
			                ISOMVertexData childData = getISOMVertexData(child);
			                if (childData != null && !childData.visited) {
			                    childData.visited = true;
			                    childData.distance = currData.distance + 1;
			                    queue.addElement(child);
			                }
			            }
			            break;
			        } catch(ConcurrentModificationException cme) {}
			    }
			}
		}
	}

	public ISOMVertexData getISOMVertexData(Vertex v) {
		return (ISOMVertexData) (v.getUserDatum(getIsomKey()));
	}

	/**
	 * This one is an incremental visualization.
	 * @return <code>true</code> is the layout algorithm is incremental, <code>false</code> otherwise
	 */
	public boolean isIncremental() {
		return true;
	}

	/**
	 * For now, we pretend it never finishes.
	 * @return <code>true</code> is the increments are done, <code>false</code> otherwise
	 */
	public boolean incrementsAreDone() {
		return false;
	}

	public static class ISOMVertexData {
		public DoubleMatrix1D disp;

		int distance;
		boolean visited;

		public ISOMVertexData() {
			initialize();
		}

		public void initialize() {
			disp = new DenseDoubleMatrix1D(2);

			distance = 0;
			visited = false;
		}

		public double getXDisp() {
			return disp.get(0);
		}

		public double getYDisp() {
			return disp.get(1);
		}

		public void setDisp(double x, double y) {
			disp.set(0, x);
			disp.set(1, y);
		}

		public void incrementDisp(double x, double y) {
			disp.set(0, disp.get(0) + x);
			disp.set(1, disp.get(1) + y);
		}

		public void decrementDisp(double x, double y) {
			disp.set(0, disp.get(0) - x);
			disp.set(1, disp.get(1) - y);
		}
	}
}