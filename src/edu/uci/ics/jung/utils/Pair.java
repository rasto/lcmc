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

import edu.uci.ics.jung.exceptions.FatalException;

/**
 * Stores a pair of values together. Access either one by directly
 * getting the fields. Pairs are not mutable, respect <tt>equals</tt>
 * and may be used as indices.<p>
 * Note that they do not protect from malevolent behavior: if one or another
 * object in the tuple is mutable, then that can be changed with the usual bad
 * effects.
 * 
 * @author scott white and Danyel Fisher
 */
final public class Pair {
	private final Object value1;
	private final Object value2;

	public Pair(Object value1, Object value2) {
		if ( value1 == null  || value2 == null)
			throw new FatalException("A Pair can't hold nulls.");
		this.value1 = value1;
		this.value2 = value2;
	}

	public Object getFirst() {
		return value1;
	}
	public Object getSecond() {
		return value2;
	}
	
	public boolean equals( Object o ) {
		if (o instanceof Pair) {
			Pair tt = (Pair) o;
            Object first = tt.getFirst();
            Object second = tt.getSecond();
            return ((first == value1 || first.equals(value1)) &&
                    (second == value2 || second.equals(value2)));
		} else {
			return false;
		}
	}
	
	public int hashCode() 
    {
		return value1.hashCode() + value2.hashCode();
	}
    
    public String toString()
    {
        return "<" + value1.toString() + ", " + value2.toString() + ">";
    }
}
