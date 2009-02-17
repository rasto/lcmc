/*
 * TouchGraph LLC. Apache-Style Software License
 *
 *
 * Copyright (c) 2001-2002 Alexander Shapiro. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by 
 *        TouchGraph LLC (http://www.touchgraph.com/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "TouchGraph" or "TouchGraph LLC" must not be used to endorse 
 *    or promote products derived from this software without prior written 
 *    permission.  For written permission, please contact 
 *    alex@touchgraph.com
 *
 * 5. Products derived from this software may not be called "TouchGraph",
 *    nor may "TouchGraph" appear in their name, without prior written
 *    permission of alex@touchgraph.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL TOUCHGRAPH OR ITS CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 */

package com.touchgraph.graphlayout;

/** <p>An class for exceptions thrown during TouchGraph processing. </p>
  *
  * @author   Alexander Shapiro
  * @author   Murray Altheim
  * @version  1.22-jre1.1  $Id: TGException.java,v 1.1 2002/09/19 15:58:08 ldornbusch Exp $
  */
public class TGException extends Exception {

    /** An exception occurring when a Node already exists. */
    public final static int NODE_EXISTS       = 1;

    /** An exception occurring when a Node doesn't exist. */
    public final static int NODE_DOESNT_EXIST = 2;

    /** An exception occurring when a Node is missing its required ID. */
    public final static int NODE_NO_ID        = 3;



    /** An int containing an exception type identifier (ID). */
    protected int id = -1;

    /** The embedded Exception if tunnelling.
      * @serial The embedded exception if tunnelling, or null.
      */    
    public Exception exception = null;
    
    // .............

    /** Constructor for TGException with Exception ID.
      *
      * @param id The unique message identifier.
      */
    public TGException( int id ) 
    {
        super();
        this.id = id;
    }

    /** Constructor for TGException with Exception ID and error message String.
      *
      * @param id The unique message identifier.
      * @param message The Exception message.
      */
    public TGException( int id, String message ) 
    {
        super(message);
        this.id = id;
    }

    /** Constructor for TGException with an error message String.
      *
      * @param message The Exception message.
      */
    public TGException( String message ) 
    {
        super(message);
    }

    /** Constructor for TGException tunnelling the original Exception.
      *
      * @param exception The original Exception.
      */
    public TGException( Exception exception ) 
    {
        super();
	this.exception = exception;
    }

    /** If the message was expressed as a MessageId, return the original
      * id (e.g. "45"). 
      *
      * @return the exception identifier.
      */
    public int getId()
    {
        return id;
    }

} // end com.touchgraph.graphlayout.TGException
