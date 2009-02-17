/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.exceptions;

public class FatalException extends RuntimeException {
    public FatalException(String s) {
        super(s);
    }

	public FatalException(String s, Throwable cause) {
		super(s, cause);
	}

}
