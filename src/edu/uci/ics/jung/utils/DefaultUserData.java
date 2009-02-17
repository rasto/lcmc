/*
 * Created on Jul 31, 2005
 *
 * Copyright (c) 2005, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.IteratorUtils;

import edu.uci.ics.jung.exceptions.FatalException;

/**
 * Represents custom user- and system-level information to extend the definition
 * of a node. This is the easiest way to extend the class without subclassing.
 * 
 * This works as a dictionary in order to help ensure that there are
 * possibilities for extending user information to a variety of different sorts
 * of data. (Each provider of information can register their own enhanced
 * information without interfering with other providers.)
 * 
 * Some suggested uses of UserData include
 * <ul>
 * <li/>Underlying data references, such as pointers to data sources</li>
 * <li/>Raw data which can be analyzed or used by the constraint and filter systems
 * <li/>Temporary enhanced information which can be used for visualization
 * </ul>
 * 
 * Consider a series of nodes that has, among other things, enhanced information
 * about 3D coordinates. This might be stored in the 3DData data structure,
 * which generates itself with an input from a node.
 * 
 * Thus the relevant call might be <code>n.setUserInfo ("3DData", new 3DData (
 * ))</code>.
 * Later, to access this information, the call might be <code>3DData dd =
 * (3DData) n.getUserInfo("3DData").</code>
 * 
 * <h3>Shared and Individual Data</h3>
 * Note that the there are no required semantics for the key or the information.
 * However, it is necessary to specify information that is used for SHARED and
 * for INDIVIDUAL data elements. When a new View of a graph is
 * generated, the Node elements inside it are all shallow-copied. The UserInfo
 * that they use, however, is <em>not</em> copied, by default. This is the
 * correct and logical behavior if the UserInfo contains source information.
 * 
 * But what when the UserInfo contains transient information, specific to the
 * view, such as graph metrics or coordinates? In that case, the UserInfo would
 * be quite inappropriate to share that information between copies.
 * 
 * The solution to this is to add a third flag, "shared", which tells whether
 * the currect data is shared or not. This flag is assigned when the data is
 * added.
 */
public class DefaultUserData extends UserData implements UserDataFactory
{
    // maps a Key to a Pair( UserData, CopyAction )
    private Map userDataStorage;

    private Map getStorage() {
        if (userDataStorage == null) {
            userDataStorage = new HashMap();
        }
        return userDataStorage;
    }

    /**
     * This class actually clones by removing the reference to the copyAction
     * and userData
     */
    public Object clone() throws CloneNotSupportedException {
        DefaultUserData ud = (DefaultUserData) super.clone();
        ud.userDataStorage = null;
        return ud;
    }

    /**
     * Adds user-level information to the node. Throws an exception if the node
     * already has information associated with it.
     * 
     * @param key
     *            A unique (per type, not per node) key into the information
     * @param value
     *            The extended information associated with the node
     */
    public void addUserDatum(Object key, Object value, CopyAction shared) {
        if (key == null)
            throw new IllegalArgumentException("Key must not be null");
        if (!getStorage().containsKey(key)) {
            getStorage().put(key, new Pair(value, shared));
        } else {
            throw new IllegalArgumentException("Key <" + key
                    + "> had already been added to an object with keys "
                    + getKeys());
        }
    }

    /**
     * @return
     */
    private Set getKeys() {
        return getStorage().keySet();
    }

    /**
     * Uses the CopyAction to determine how each of the user datum elements in
     * udc should be carried over to the this UserDataContiner
     * 
     * @param udc
     *            The UserDataContainer whose user data is being imported
     */
    public void importUserData(UserDataContainer udc) {
        for (Iterator keyIt = udc.getUserDatumKeyIterator(); keyIt.hasNext();) {
            Object key = keyIt.next();
            Object value = udc.getUserDatum(key);
            CopyAction action = udc.getUserDatumCopyAction(key);
            Object newValue = action.onCopy(value, udc, this);
            try {
                if (newValue != null) addUserDatum(key, newValue, action);

            } catch (IllegalArgumentException iae) {
                List userDataKeys = IteratorUtils.toList(udc
                        .getUserDatumKeyIterator());
                throw new FatalException("Copying <" + key + "> of "
                        + userDataKeys
                        + " into a container that started with some keys ",
                        iae);
            }
        }

    }

    /**
     * Changes the user-level information to the object. Equivalent to calling
     * 
     * <pre>
     * 
     *  
     *   removeUserDatum( key );      
     *   addUserDatum(key, value) 
     *   
     *  
     * </pre>
     * 
     * @param key
     * @param value
     */
    public void setUserDatum(Object key, Object value, CopyAction shared) {
        getStorage().put(key, new Pair(value, shared));
    }

    /**
     * Returns UserInfo (if known) for this key, or <em>null</em> if not
     * known.
     * 
     * @param key
     */
    public Object getUserDatum(Object key) {
        Pair p = (Pair) getStorage().get(key);
        if (p == null) return null;
        return p.getFirst();
    }

    /**
     * Removes the Datum (if known) for this key, and returns it.
     * 
     * @param key
     */
    public Object removeUserDatum(Object key) {
        Object o = getUserDatum(key);
        getStorage().remove(key);
        return o;
    }

    /**
     * Iterates through the keys to all registered data. Note: there's no easy
     * way to know, looking at a piece of data, whether it is or is not shared.
     * 
     * @return Iterator
     */
    public Iterator getUserDatumKeyIterator() {
        return getStorage().keySet().iterator();
    }

    /**
     * @see UserDataContainer#containsUserDatumKey(Object)
     */
    public boolean containsUserDatumKey(Object key)
    {
        return getStorage().containsKey(key);
    }
    
    /**
     * Returns the CopyAction associated with this key.
     * 
     * @param key
     * @return CopyAction
     */
    public CopyAction getUserDatumCopyAction(Object key) {
        Pair p = (Pair) getStorage().get(key);
        return (CopyAction) p.getSecond();
    }

    public UserDataContainer getInstance()
    {
        return new DefaultUserData();
    }

}
