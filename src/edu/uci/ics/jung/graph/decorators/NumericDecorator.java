/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.graph.decorators;

import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * Decorator for any value type that extends the java.lang.Number class
 * @author Scott White
 */
public class NumericDecorator extends Decorator {

    /**
     * Constructs and initializes the decorator
     * @param key
     * @param copyAction
     */
    public NumericDecorator(Object key,UserDataContainer.CopyAction copyAction) {
       super(key,copyAction);
    }

    /**
     * Retrieves the decorated value for the given graph/vertex/edge as an integer
     * @param udc the graph/vertex/edge
     * @return the integer value
     */
    public int intValue(UserDataContainer udc) {
        return ((Number) udc.getUserDatum(getKey())).intValue();
    }

    /**
     * Returns the decorated value as Number
     * @param udc the graph/vertex/edge
     * @return the value
     */
    public Number getValue(UserDataContainer udc) {
        return (Number) udc.getUserDatum(getKey());
    }

    /**
     * Sets the value for a given graph/vertex/edge
     * @param value the value to be stored
     * @param udc the graph/vertex/edge being decorated
     */
    public void setValue(Number value, UserDataContainer udc) {
        udc.setUserDatum(getKey(),value,getCopyAction());
    }
}
