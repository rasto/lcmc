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

import java.util.regex.Pattern;

/**
 * Contains general-purpose utility functions.
 * 
 * @author Scott White
 * @author Joshua O'Madadhain
 */
public class GeneralUtils {
    private static final int K = 10; // M==1024
    private static final int INT_SIZE = 32;
    private static final int KnuthsAValue = (int) 2654435769L;
    
    private static final Pattern IS_NUMERIC = Pattern.compile("\\d*\\.?\\d*");
    
    public static int hash(int value)
	{
        return (value * KnuthsAValue) >>> (INT_SIZE - K);
    }
    
    public static boolean isNumeric(String s)
    {
        return s.length() > 0 && IS_NUMERIC.matcher(s).matches();
    }
}
