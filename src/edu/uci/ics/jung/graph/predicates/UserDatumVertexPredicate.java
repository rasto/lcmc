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

import edu.uci.ics.jung.graph.ArchetypeVertex;


/**
 * A predicate that checks to see whether a vertex's user
 * data repository contains 
 * the constructor-specified (key,datum) pair.  This predicate
 * may be used as a constraint.
 */
public class UserDatumVertexPredicate extends VertexPredicate
{
    public static final String message = "UserDatumVertexPredicate: ";
    private Object datum;
    private Object key;
    
    public UserDatumVertexPredicate(Object key, Object datum)
    {
        if (key == null)
            throw new IllegalArgumentException("UserDatumVertexPredicate " + 
                    "key must be non-null");
        this.datum = datum;
        this.key = key;
    }
        
    /**
     * Returns <code>true</code> if the datum stored by <code>v</code> with
     * key value <code>key</code> (in the user data repository) is 
     * <code>datum</code>.
     * 
     * @see edu.uci.ics.jung.utils.UserData
     */
    public boolean evaluateVertex(ArchetypeVertex v)
    {
        Object value = v.getUserDatum(key);
        return ((datum == null && value == null) || datum.equals(value));
    }

    public String toString()
    {
        return message + "(" + key + ", " + datum + ")";
    }
    
    /**
     * Tests equality based on underlying objects
     */
    public boolean equals( Object o ) {
        if (! (o instanceof UserDatumVertexPredicate))
            return false;
        UserDatumVertexPredicate udvp = (UserDatumVertexPredicate) o;
        return ( udvp.datum.equals( datum ) && udvp.key.equals(key)); 
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
