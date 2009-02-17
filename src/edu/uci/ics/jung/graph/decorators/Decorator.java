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
 * Abstract decorator for cases where attributes are to be stored along with the graph/edge/vertex which they describe
 * @author Scott White
 */
public abstract class Decorator {
    private Object mKey;
    private UserDataContainer.CopyAction mCopyAction;

    /**
     * Constructs and initializes the decorator
     * @param key
     * @param action
     */
    protected Decorator(Object key,UserDataContainer.CopyAction action) {
        mKey = key;
        mCopyAction = action;
    }

    /**
     * Retrieves the user datum copy action that this decorator uses when setting new values
     * @return the copy action
     */
    public UserDataContainer.CopyAction getCopyAction() { return mCopyAction; }

    /**
     * Retrieves the user datum key that this decorator uses when setting new values
     */
    public Object getKey() { return mKey; }

    /**
     * @return the hash code for the user datum key
     */
    public int hashCode() {
        return mKey.hashCode();
    }

    /**
     * Removes the values from the user data container
     * @param udc the vertex/edge/graph being whose value is being removed
     */
    public void removeValue(UserDataContainer udc) {
        udc.removeUserDatum(mKey);
    }
}
