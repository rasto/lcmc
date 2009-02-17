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

import java.util.Set;
import java.util.Iterator;

/**
 * @author Scott White
 */
public class UserDataUtils {

    public static void cleanup(Set userDataContainers, Object key) {
        for (Iterator udcIt=userDataContainers.iterator(); udcIt.hasNext();) {
            ((UserDataContainer) udcIt.next()).removeUserDatum(key);
        }
    }

    public static void cleanup(Set userDataContainers, Object key1, Object key2) {
        for (Iterator udcIt=userDataContainers.iterator(); udcIt.hasNext();) {
            UserDataContainer udc = (UserDataContainer) udcIt.next();
            udc.removeUserDatum(key1);
            udc.removeUserDatum(key2);
        }
    }

    public static void cleanup(Set userDataContainers, Object key1, Object key2, Object key3) {
        for (Iterator udcIt=userDataContainers.iterator(); udcIt.hasNext();) {
            UserDataContainer udc = (UserDataContainer) udcIt.next();
            udc.removeUserDatum(key1);
            udc.removeUserDatum(key2);
            udc.removeUserDatum(key3);
        }
    }
}
