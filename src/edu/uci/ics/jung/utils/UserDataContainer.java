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

import java.lang.reflect.Method;
import java.util.Iterator;

import edu.uci.ics.jung.exceptions.FatalException;

/**
 * The generic interface for storing {@link UserData UserData}. All graphs,
 * vertices, and edges implement this interface and can therefore store custom 
 * local data.
 * 
 * @author Joshua O'Madadhain, Scott White, Danyel Fisher
 */
public interface UserDataContainer extends Cloneable
{

    public Object clone() throws CloneNotSupportedException;
    
    /**
	 * Adds the specified data with the specified key to this object's
	 * user data repository, with the specified CopyAction.
	 *
	 * @param key      the key of the datum being added
	 * @param datum    the datum being added
	 * @param copyAct  the CopyAction of the datum being added
	 */
	public void addUserDatum(Object key, Object datum, CopyAction copyAct);

	/**
	 * Takes the user data stored in udc and copies it to this object's 
	 * user data repository, respecting each datum's CopyAction.
	 * 
	 * @param udc  the source of the user data to be copied into this container
	 */
	public void importUserData(UserDataContainer udc);

    /**
	 * Provides an iterator over this object's user data repository key set.
	 */
	public Iterator getUserDatumKeyIterator();

	/**
	 * Retrieves the CopyAction for the object stored in this object's
	 * user data repository to which key refers.
	 *
	 * @param key          the key of the datum whose CopyAction is requested
	 * @return CopyAction  the requested CopyAction
	 */
	public CopyAction getUserDatumCopyAction(Object key);

    /**
	 * Retrieves the object in this object's user data repository to which key
	 * refers.
	 *
	 * @param key      the key of the datum to retrieve
	 * @return Object  the datum retrieved
	 */
	public Object getUserDatum(Object key);

	/**
	 * If key refers to an existing user datum in this object's repository, 
	 * that datum is replaced by the specified datum.  Otherwise this is equivalent to 
	 * addUserDatum(key, data, copyAct).
	 * 
	 * @param key      the key of the datum being added/modified
	 * @param datum    the replacement/new datum
	 * @param copyAct  the CopyAction for the new (key, datum) pair
	 */
	public void setUserDatum(Object key, Object datum, CopyAction copyAct);

	/**
	 * Retrieves the object in this object's user data repository to which key
	 * refers, and removes it from the repository.
	 * 
	 * @param key      the key of the datum to be removed
	 * @return Object  the datum removed
	 */
	public Object removeUserDatum(Object key);

    /**
     * Reports whether <code>key</code> is a key of this user data container.
     * 
     * @param key   the key to be queried
     * @return      true if <code>key</code> is present in this user data container
     */
    public boolean containsUserDatumKey(Object key);
    
	/**
	 * Decides what to do when a user datum is copied. Some of the
	 * more common responses might include returning null, returning the
	 * object, or returning a clone of the object.
	 * 
	 * @author danyelf
	 */
	static interface CopyAction {
		/**
		 * The callback triggered when a UserDatum is copied. Implement this 
		 * method to create your own CopyAction.
  		 *
		 * @param value		The item of UserData that is being copied
         * @param source	The UserDataContainer that holds this datum
         * @param target	The UserDataContainer that will hold the new datum
		 * @return Object	The copy of the UserData
		 */
		public Object onCopy(Object value, UserDataContainer source, UserDataContainer target);

		/**
		 * Implements UserData.CLONE
		 * @see UserData#CLONE
		 * @author danyelf
		 */
		static class Clone implements CopyAction {
            public Object onCopy(Object value, UserDataContainer source, UserDataContainer target) {

				try {
					if (! (value instanceof Cloneable)) {
						throw new CloneNotSupportedException("Not cloneable interface: This used to just return a shared reference.");
					}

					Method cloneMethod =
						value.getClass().getMethod("clone", null);

					if (cloneMethod != null) {
						return cloneMethod.invoke(value, null);
					} else {
						throw new CloneNotSupportedException("No clone method implemented: This used to just return a shared reference.");
					}

				} catch (Exception e) {
					throw new FatalException("Cloning failure", e);
				}

			}
		}

		/**
		 * Implements UserData.SHARED
		 * @see UserData#SHARED
		 * @author danyelf
		 */
		static class Shared implements CopyAction {
			public Object onCopy(Object value, UserDataContainer source, UserDataContainer target) {
				return value;
			}
		}

		/**
		 * Implements UserData.REMOVE
		 * @see UserData#REMOVE
		 * @author danyelf
		 */
		static class Remove implements CopyAction {
			public Object onCopy(Object value, UserDataContainer source, UserDataContainer target) {
				return null;
			}
		}
	}
}
