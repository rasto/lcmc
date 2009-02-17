/*
 * Copyright (c) 2003, the JUNG Project and the Regents of the University of
 * California All rights reserved.
 * 
 * This software is open-source under the BSD license; see either "license.txt"
 * or http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.Iterator;

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
public abstract class UserData implements UserDataContainer {

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = getUserDatumKeyIterator(); iter.hasNext();) {
            Object key = (Object) iter.next();
            sb.append(key);
            sb.append("=");
            sb.append(getUserDatum(key));
            if( iter.hasNext()) {
                sb.append(", ");
            }
        }
        return "USERDATA [" + sb  + "]";
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
    
    /**
     * A CopyAction that clones UserData--that is, it uses the Java
     * {@link java.lang.Object#clone() clone()}call to clone the object. Throws
     * a <tt>CloneNotSupportedException</tt> if clone isn't allowed.
     */
    public static final CopyAction CLONE = new CopyAction.Clone();

    /**
     * A CopyAction that links UserData--that is, points to the original data.
     * At that point, both UserDataContainers will contain references to the
     * same UserData, and, if that data is mutable, will both see changes to it.
     * (In the case of immutable user data, such as Strings, they will
     * disconnect if one or the other attempts to change its value: this is the
     * normal behavior with <code>
     *  String s = "X";
     *  String t = s;
     *  s = "Y";
     *  System.out.pritnln( t ); // will still contain X.
     * </code>
     */
    public static final CopyAction SHARED = new CopyAction.Shared();

    /**
     * Causes the userdata not to be copied over, and instead returns null.
     * Useful for temporary userdata that isn't meant to be used.
     */
    public static final CopyAction REMOVE = new CopyAction.Remove();

}