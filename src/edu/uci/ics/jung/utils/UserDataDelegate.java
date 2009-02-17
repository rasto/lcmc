/*
 * Created on Jul 30, 2005
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.utils;

import java.util.Iterator;


public class UserDataDelegate implements UserDataContainer, Cloneable
{
    protected UserDataContainer udc_delegate;
    protected static UserDataFactory factory = (UserDataFactory)new DefaultUserData();
//    protected static UserDataFactory factory = (UserDataFactory)new UnifiedUserData();
    
    public UserDataDelegate()
    {
        this.udc_delegate = factory.getInstance();
    }
    
    public static void setUserDataFactory(UserDataFactory udf)
    {
        factory = udf;
    }

    public Object clone() throws CloneNotSupportedException
    {
        UserDataDelegate udd = (UserDataDelegate)super.clone();
        udd.udc_delegate = (UserDataContainer)udc_delegate.clone();
        return udd;
    }
    
    public void addUserDatum(Object key, Object datum, CopyAction copyAct)
    {
        udc_delegate.addUserDatum(key, datum, copyAct);
    }

    public void importUserData(UserDataContainer udc)
    {
        udc_delegate.importUserData(udc);
    }

    public Iterator getUserDatumKeyIterator()
    {
        return udc_delegate.getUserDatumKeyIterator();
    }

    public CopyAction getUserDatumCopyAction(Object key)
    {
        return udc_delegate.getUserDatumCopyAction(key);
    }

    public Object getUserDatum(Object key)
    {
        return udc_delegate.getUserDatum(key);
    }

    public void setUserDatum(Object key, Object datum, CopyAction copyAct)
    {
        udc_delegate.setUserDatum(key, datum, copyAct);
    }

    public Object removeUserDatum(Object key)
    {
        return udc_delegate.removeUserDatum(key);
    }

    public boolean containsUserDatumKey(Object key)
    {
        return udc_delegate.containsUserDatumKey(key);
    }
}
