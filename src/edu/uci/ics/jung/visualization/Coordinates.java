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

import java.awt.geom.Point2D;

/**
 * 
 * Stores coordinates (X,Y) for vertices being visualized. 
 * 
 * @author Scott White
 */
public class Coordinates extends Point2D.Float {

     public Coordinates() {
         super();
    }

    public Coordinates(double x, double y) {
        super((float)x,(float)y);
    }

	/**
	 * Initializes this coordinate to the value of the passed-in
	 * coordinate.
	 * @param coordinates
	 */
    public Coordinates(Coordinates coordinates) {
        this(coordinates.getX(), coordinates.getY());
    }

	/**
	 * Sets the x value to be d;
	 * @param d
	 */
    public void setX(double d) {
        setLocation(d, getY());
    }

	/**
	 * Sets the y value to be d;
	 * @param d
	 */
    public void setY(double d) {
        setLocation(getX(), d);
    }

	/**
	 * Increases the x and y values of this
	 * scalar by (x, y).
	 * @param x
	 * @param y
	 */
    public void add(double x, double y) {
        addX(x);
        addY(y);
    }

	/**
	 * Increases the x value by d.
	 * @param d
	 */
    public void addX(double d) {
        setX(getX()+d);
    }

	/**
	 * Increases the y value by d.
	 * @param d
	 */
    public void addY(double d) {
        setY(getY()+d);
    }

	/**
	 * Multiplies a coordinate by scalar x and y values.
	 * @param x	A scalar to multiple x by
	 * @param y	A scalar to multiply y by
	 */
    public void mult(double x, double y) {
        multX(x);
        multY(y);
    }

	/**
	 * Multiplies the X coordinate by a scalar value.
     * <P>
	 * For example, (3, 10) x-scaled by 2 returns (6, 10).
	 * @param d	the scalar value by which x will be multiplied
	 */
    public void multX(double d) {
        setX(getX()*d);
    }

	/**
	 * Multiplies the Y coordinate by a scalar value.
     * <P>
	 * For example, (3, 10) y-scaled by 2 returns (3, 20).
     * @param d the scalar value by which y will be multiplied
	 */
    public void multY(double d) {
        setY(getY()*d);
    }

    /**
     * Computes the euclidean distance between two coordinates
     * @param o another coordinates
     * @return the euclidean distance
     */
    public double distance(Coordinates o) {
        return super.distance(o);
    }

    /**
     * Computes the midpoint between the two coordinates
     * @param o another coordinates
     * @return the midpoint
     */
    public Coordinates midpoint(Coordinates o) {
        double midX = (this.getX() + o.getX()) / 2.0;
        double midY = (this.getY() + o.getY()) / 2.0;
        Coordinates midpoint = new Coordinates(midX, midY);
        return midpoint;
    }

}
