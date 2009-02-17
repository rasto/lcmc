/*
 * Created on Mar 17, 2004
 *
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.predicates;

import edu.uci.ics.jung.graph.ArchetypeEdge;


/**
 * A predicate that checks to see whether an edge's user
 * data repository contains 
 * the constructor-specified (key,datum) pair.  This predicate
 * may be used as a constraint.
 */
public class UserDatumEdgePredicate extends EdgePredicate
{
    public static final String message = "UserDatumEdgePredicate: ";
    private Object datum;
    private Object key;
    
    public UserDatumEdgePredicate(Object key, Object datum)
    {
        if (key == null)
            throw new IllegalArgumentException("UserDatumEdgePredicate " + 
                    "key must be non-null");
        this.datum = datum;
        this.key = key;
    }
        
    /**
     * Returns <code>true</code> if the datum stored by <code>e</code> with
     * key value <code>key</code> (in the user data repository) is 
     * <code>datum</code>.
     * 
     * @see edu.uci.ics.jung.utils.UserData
     */
    public boolean evaluateEdge(ArchetypeEdge e)
    {
        Object value = e.getUserDatum(key);
        return ((datum == null && value == null) || datum.equals(value));
//        return (e.getUserDatum(key).equals(datum));
    }

    public String toString()
    {
        return message + "(" + key + ", " + datum + ")";
    }
    
    /**
     * Tests equality based on underlying objects
     */
    public boolean equals( Object o ) {
        if (! (o instanceof UserDatumEdgePredicate))
            return false;
        UserDatumEdgePredicate udep = (UserDatumEdgePredicate) o;
        return ( udep.datum.equals( datum ) && udep.key.equals(key)); 
    }
    
    public int hashCode() {
        return datum.hashCode() + key.hashCode();
    }
    
    /**
     * Returns the user data key which partially defines this predicate.
     */
    public Object getKey()
    {
        return key;
    }
    
    /**
     * Returns the user datum which partially defines this predicate.
     */
    public Object getDatum()
    {
        return datum;
    }
}
