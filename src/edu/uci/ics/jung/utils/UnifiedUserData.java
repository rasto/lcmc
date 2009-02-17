/*
 * Created on Jul 30, 2005
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
 * Thus the relevant call might be <code>n.addUserDatum("3DData", new 3DData (
 * ))</code>.
 * Later, to access this information, the call might be <code>3DData dd =
 * (3DData) n.getUserDatum("3DData").</code>
 * 
 * <h3>Shared and Individual Data</h3>
 * Note that the there are no required semantics for the key or the information.
 * However, it is necessary to specify information that is used for SHARED and
 * for INDIVIDUAL data elements. When a new View of a graph is
 * generated, the Node elements inside it are all shallow-copied. The UserData
 * that they use, however, is <em>not</em> copied, by default. This is the
 * correct and logical behavior if the UserData contains source information.
 * 
 * But what when the UserData contains transient information, specific to the
 * view, such as graph metrics or coordinates? In that case, the UserData would
 * be quite inappropriate to share that information between copies.
 * 
 * The solution to this is to add a third flag, "shared", which tells whether
 * the currect data is shared or not. This flag is assigned when the data is
 * added.
 */
public class UnifiedUserData extends UserData implements UserDataFactory
{
    // maps a Key to a Pair( UserData, CopyAction )
//    private Map userDataStorage;
    protected final static Map key_meta_map = new HashMap();
//    private final static Object NO_GRAPH = "NO_GRAPH";

//    private Map getStorage() {
//        if (userDataStorage == null) {
//            userDataStorage = new HashMap();
//        }
//        return userDataStorage;
//    }

//    /**
//     * This class actually clones by removing the reference to the copyAction
//     * and userData
//     */
//    protected Object clone() throws CloneNotSupportedException {
//        UserData ud = (UserData) super.clone();
////        ud.userDataStorage = null;
//        return ud;
//    }

    protected Map getKeyMap(Object key)
    {
        Map key_map = (Map)key_meta_map.get(key);
        if (key_map == null)
        {
            key_map = new WeakHashMap();
//            key_map = new HashMap();
            key_meta_map.put(key, key_map);
        }
        return key_map;
    }

//    protected Pair getObjectKey()
//    {
//        Object container = this;
//        if (this instanceof Element)
//        {
//            container = ((Element)this).getGraph();
//            if (container == null)
//                container = NO_GRAPH;
//        }
////        return new Pair(new WeakReference(this), new WeakReference(container));
//        return new Pair(this, container);
//    }
    
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

        Map key_map = getKeyMap(key);
        
//        Pair object_key = getObjectKey();
//        if (key_map.containsKey(object_key)) 
        if (key_map.containsKey(this))
            throw new IllegalArgumentException("Key <" + key
                    + "> had already been added to object " + this);
        
        Pair object_value = new Pair(value, shared);
        
//        key_map.put(object_key, object_value);
        key_map.put(this, object_value);
        
//        if (!getStorage().containsKey(key)) {
//            getStorage().put(key, new Pair(value, shared));
//        } else {
//            throw new IllegalArgumentException("Key <" + key
//                    + "> had already been added to an object with keys "
//                    + getKeys());
//        }
    }

    /**
     * @return
     */
//    private Set getKeys() {
//        return getStorage().keySet();
//    }

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
     *   removeUserDatum( key );      
     *   addUserDatum(key, value) 
     * </pre>
     * 
     * @param key
     * @param value
     */
    public void setUserDatum(Object key, Object value, CopyAction shared) {
//        removeUserDatum(key);
//        addUserDatum(key, value, shared);
//        getKeyMap(key).put(getObjectKey(), new Pair(value, shared));
        getKeyMap(key).put(this, new Pair(value, shared));
//        getStorage().put(key, new Pair(value, shared));
    }

    /**
     * Returns UserData (if any) for this key, or <em>null</em> if not
     * known.
     * 
     * @param key
     */
    public Object getUserDatum(Object key) 
    {
        Pair value_pair = this.getUserDatumValuePair(key);
//        Pair p = (Pair) getStorage().get(key);
        if (value_pair == null) 
            return null;
        return value_pair.getFirst();
    }

    protected Pair getUserDatumValuePair(Object key)
    {
//        return (Pair)(getKeyMap(key).get(getObjectKey()));
        return (Pair)(getKeyMap(key).get(this));
    }
    
    /**
     * Removes the Datum (if any) for this key, and returns it.
     * 
     * @param key
     */
    public Object removeUserDatum(Object key) {
        Object o = getUserDatum(key);
        Map key_map = getKeyMap(key);
//        key_map.remove(getObjectKey());
        key_map.remove(this);
        if (key_map.isEmpty())
            key_meta_map.remove(key_map);
//        getStorage().remove(key);
//        return value_pair.getFirst();
        return o;
    }

    /**
     * Returns an <code>Iterator</code> which can be used to iterate over
     * all the user data repository keys for this object.
     * 
     * @return Iterator
     */
    public Iterator getUserDatumKeyIterator() {
        List keys = new LinkedList();
//        Pair key_pair = getObjectKey();
        for (Iterator iter = key_meta_map.keySet().iterator(); iter.hasNext(); )
        {
            Object key = iter.next();
            Map key_map = getKeyMap(key);
//            if (key_map.containsKey(key_pair))
            if (key_map.containsKey(this))
                keys.add(key);
        }
        return keys.iterator();
//        return getStorage().keySet().iterator();
    }

    /**
     * @see UserDataContainer#containsUserDatumKey(Object)
     */
    public boolean containsUserDatumKey(Object key)
    {
//        return ((Map)key_meta_map.get(key)).containsKey(getObjectKey());
        return ((Map)key_meta_map.get(key)).containsKey(this);
//        return getStorage().containsKey(key);
    }
    
    /**
     * Returns the CopyAction associated with this key.
     * 
     * @param key
     * @return CopyAction
     */
    public CopyAction getUserDatumCopyAction(Object key) {
        Pair value_pair = this.getUserDatumValuePair(key);
        if (value_pair == null) return null;
//        Pair p = (Pair) getStorage().get(key);
        return (CopyAction) value_pair.getSecond();
    }

    public UserDataContainer getInstance()
    {
        return new UnifiedUserData();
    }
    
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
    
}
