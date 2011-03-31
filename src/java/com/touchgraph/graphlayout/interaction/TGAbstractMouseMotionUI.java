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

import  com.touchgraph.graphlayout.*;

import  java.awt.*;
import  java.awt.event.*;

/** TGAbstractMouseMotionUI allows one to write user interfaces that handle
  * what happends when a mouse is moved over the screen
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: TGAbstractMouseMotionUI.java,v 1.1 2002/09/19 15:58:21 ldornbusch Exp $
  */
public abstract class TGAbstractMouseMotionUI extends TGUserInterface{
	TGPanel tgPanel;

	private AMMUIMouseMotionListener mml;

// ............

 /** Constructor with TGPanel <tt>tgp</tt>.
	 */
	public TGAbstractMouseMotionUI( TGPanel tgp ) {
			tgPanel=tgp;
			mml=new AMMUIMouseMotionListener();
	}

	 public final void activate() {
			tgPanel.addMouseMotionListener(mml);
	 }

	public final void deactivate() {
			tgPanel.removeMouseMotionListener(mml);
			super.deactivate(); //To activate parentUI from TGUserInterface
	}

	public abstract void mouseMoved(MouseEvent e);

	public abstract void mouseDragged(MouseEvent e);

	private class AMMUIMouseMotionListener extends MouseMotionAdapter {
			public void mouseMoved(MouseEvent e) {
						TGAbstractMouseMotionUI.this.mouseMoved(e);
			}

			public void mouseDragged(MouseEvent e) {
						TGAbstractMouseMotionUI.this.mouseMoved(e);
			}
	}
} // end com.touchgraph.graphlayout.interaction.TGAbstractMouseMotionUI
