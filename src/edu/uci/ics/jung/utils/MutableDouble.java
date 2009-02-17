/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.utils;


/**
 * The <code>MutableDouble</code> class wraps a value of the primitive type <code>double</code> in a mutable object. An object of type <code>MutableDouble</code> contains a single field whose type is <code>double</code>.
 * This allows the system to not pile up large
 * sets of temporary "numbers" and reduces object creation when doing math.
 * <p>
 *  In addition, this class provides several methods for converting a <code>double</code> to a String and a String to a <code>double</code>.
 *  <p>
 * Warning: It is important to not modify Mutable values when they are in a
 * sorted data structure, such as a TreeSet! They will fall out of order and
 * cause the set to be inconsistent
 *
 * @author Scott White
 */
public class MutableDouble extends Number implements Comparable {
	private double mDouble;

	/**
	 * Constructs a new MutableDouble with a default value of 0
     * assigned
	 */
	public MutableDouble() {
		super();
        mDouble = 0;
	}

	/**
	 * Constructs a new MutableDouble with the input value.
	 */
	public MutableDouble(double initialValue) {
		setDoubleValue(initialValue);
	}

	/**
	 * Returns the floor integer value, accomplished by casting the
	 * contained double to <code>int</code>.
	 */
	public int intValue() {
		return (int) mDouble;
	}

	/**
	 * Returns the floor integer value as a long, accomplished by casting the
	 * contained double to <code>long</code>.
	 */
	public long longValue() {
		return (long) mDouble;
	}

	/**
	 * Returns the nearest float value, accomplished by casting the
	 * contained double to <code>float</code>.
	 */
	public float floatValue() {
		return (float) mDouble;
	}

	/**
	 * Returns the value as a double, accomplished by returning the
	 * primitive contained double.
	 */
	public double doubleValue() {
		return mDouble;
	}

	/**
	 * @see java.lang.Comparable
	 */
	public int compareTo(java.lang.Object o) {
		double thisVal = this.doubleValue();
		double anotherVal = ((MutableDouble) o).doubleValue();
		return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
	}

	/**
	 * Compares this object to the specified object.
	 * The result is <code>true</code> if and only if the argument is not
	 * <code>null</code> and is an <code>MutableDouble</code> object that contains
	 * the same <code>double</code> value as this object.
	 *
	 * @param   obj   the object to compare with.
	 * @return  <code>true</code> if the objects are the same;
	 *          <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof MutableDouble)) {
			return doubleValue() == ((MutableDouble) obj).doubleValue();
		}
		return false;
	}

	/**
	 * Returns a hashcode for this Integer.
	 *
	 * @return  a hash code value for this object, equal to the
	 *          primitive <tt>int</tt> value represented by this
	 *          <tt>MutableDouble</tt> object.
	 */
	public int hashCode() {
		long bits = Double.doubleToLongBits(mDouble);
		return (int) (bits ^ (bits >>> 32));
	}

	/**
	 * Sets the double value to a new value.
	 */
	public void setDoubleValue(double newDouble) {
		mDouble = newDouble;
	}

	/**
	 * Increases the <tt>double</tt>'s value by <tt>value</tt>.
	 * The object will, after this call, contain the value
	 * <code>doubleValue() + value</code>.
	 *
	 * @param value	the amount to add
	 * @return this object, for convenience in chaining operations
	 */
	public MutableDouble add(double value) {
		mDouble += value;
		return this;
	}

	/**
	 * Decreases the <tt>double</tt>'s value by <tt>value</tt>.
	 * The object will, after this call, contain the value
	 * <code>doubleValue() - value</code>.
	 *
	 * @param value	the amount to subtract
	 * @return this object, for convenience in chaining operations
	 */
	public MutableDouble subtract(double value) {
		mDouble -= value;
		return this;
	}

	/**
	 * Uses the default String converter to return the value of this
	 * as a string.
	 */
	public String toString() {
		return String.valueOf(mDouble);
	}

}