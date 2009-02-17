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

package com.touchgraph.graphlayout.interaction;

import com.touchgraph.graphlayout.*;

import  java.awt.*;
import  java.awt.event.*;
import  java.util.Vector;

/** TGUserInterface.  A user interface that can be activated or deactivated,
  * much like a listener can be added or removed.
  *
  * If a parent UI is specified as a parameter to activate() then the parent UI
  * is temporarily disabled while the current UI is active.  Classes that extend
  * TGUserInterface must call super.deactivate() if they override this method.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: TGUserInterface.java,v 1.1 2002/09/19 15:58:21 ldornbusch Exp $
  */
public abstract class TGUserInterface {

    private TGUserInterface parentUI;

    public abstract void activate();

    /** Each user interface is responsible for properly setting this variable. */
    boolean active; 

    public boolean isActive() {
        return active; 
    }

    public void activate( TGUserInterface parent ) {
        parentUI = parent;
        parentUI.deactivate();
        activate();
    }

    public void deactivate() {
        if (parentUI!=null) parentUI.activate();
        parentUI = null;
    }

} // end com.touchgraph.graphlayout.interaction.TGUserInterface
