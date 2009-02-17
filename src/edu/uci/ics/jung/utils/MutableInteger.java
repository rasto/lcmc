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
 * The <code>MutableInteger</code> class wraps a value of the primitive type <code>int</code> in a mutable object. An object of type <code>MutableInteger</code> contains a single field whose type is <code>int</code>.
 * This allows the system to not pile up large
 * sets of temporary "numbers" and reduces object creation when doing math.
 * <p>
 *  In addition, this class provides several methods for converting a <code>int</code> to a String and a String to a <code>int</code>.
 *  <p>
 * Warning: It is important to not modify Mutable values when they are in a
 * sorted data structure, such as a TreeSet! They will fall out of order and 
 * cause the set to be inconsistent
 * 
 * @author Scott White
 */
public class MutableInteger extends Number implements Comparable {
	private int mInteger;

	/**
	 * Constructs a new MutableInteger with default value 0.
	 */
	public MutableInteger() {
		setInteger(0);
	}

	/**
	 * Returns the integer value of this object.
	 */
	public int intValue() {
		return mInteger;
	}

	/**
	 * Returns the integer value of this object, expressed as a long.
	 */
	public long longValue() {
		return mInteger;
	}

	/**
	 * Returns the integer value of this object, expressed as a float.
	 */
	public float floatValue() {
		return mInteger;
	}

	/**
	 * Returns the integer value of this object, expressed as a double.
	 */
	public double doubleValue() {
		return mInteger;
	}

	/**
	 * Increases the <tt>int</tt>'s value by <tt>value</tt>. 
	 * The object will, after this call, contain the value
	 * <code>(int) ( intValue() + value ) </code>.
	 * 
	 * @param value	the amount to add
	 * @return this object, for convenience in chaining operations
	 */
	public MutableInteger add(double value) {
		mInteger += value;
		return this;
	}

	/**
	 * Increases the <tt>int</tt>'s value by <tt>value</tt>. 
	 * The object will, after this call, contain the value
	 * <code>(int) ( intValue() - value ) </code>.
	 * 
	 * @param value	the amount to subtract
	 * @return this object, for convenience in chaining operations
	 */
	public MutableInteger subtract(double value) {
		mInteger -= value;
		return this;
	}

	public MutableInteger(int initialValue) {
		setInteger(initialValue);
	}

	/**
	 * @see java.lang.Comparable
	 */
	public int compareTo(java.lang.Object o) {
		int thisVal = this.intValue();
		int anotherVal = ((MutableInteger) o).intValue();
		return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
	}

	/**
	 * Compares this object to the specified object.
	 * The result is <code>true</code> if and only if the argument is not
	 * <code>null</code> and is an <code>MutableInteger</code> object that contains
	 * the same <code>int</code> value as this object.
	 *
	 * @param   obj   the object to compare with.
	 * @return  <code>true</code> if the objects are the same;
	 *          <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof MutableInteger)) {
			return intValue() == ((MutableInteger) obj).intValue();
		}
		return false;
	}

	/**
	 * Returns a hashcode for this Integer.
	 *
	 * @return  a hash code value for this object, equal to the
	 *          primitive <tt>int</tt> value represented by this
	 *          <tt>MutableInteger</tt> object.
	 */
	public int hashCode() {
		return GeneralUtils.hash(mInteger);
	}

	/**
	 * Sets the value of this object to <tt>newInteger</tt>.
	 */
	public void setInteger(int newInteger) {
		mInteger = newInteger;
	}

	/**
	 * Adds one to the contained integer value.
	 * @return this, to assist in chaining.
	 */
	public MutableInteger increment() {
		mInteger++;
		return this;
	}

	/**
	 * Subtracts one from the contained integer value.

	 */
	public void decrement() {
		mInteger--;
	}

	public String toString() {
		return String.valueOf(mInteger);
	}
}