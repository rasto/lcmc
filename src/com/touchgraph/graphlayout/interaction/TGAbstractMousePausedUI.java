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

/** TGAbstractMousePausedUI allows one to handle MousePaused events.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: TGAbstractMousePausedUI.java,v 1.1 2002/09/19 15:58:21 ldornbusch Exp $
  */
public abstract class TGAbstractMousePausedUI extends TGUserInterface {

     private AMPUIMouseMotionListener mml;
     private AMPUIMouseListener ml;
     protected TGPanel tgPanel;
     Point mousePos=null;
     PauseThread pauseThread=null;

  // ............

   /** Constructor with TGPanel <tt>tgp</tt>.
     */
     public TGAbstractMousePausedUI( TGPanel tgp ) { // Instantiate this way to keep listening
                                                     // for clicks until deactivate is called
          tgPanel=tgp;
          ml = new AMPUIMouseListener();
          mml = new AMPUIMouseMotionListener();
      }

     public final void activate() {
        preActivate();
          tgPanel.addMouseMotionListener(mml);
          tgPanel.addMouseListener(ml);
     }

    public final void deactivate() {
        tgPanel.removeMouseMotionListener(mml);
        tgPanel.removeMouseListener(ml);
        postDeactivate();
        super.deactivate(); //To activate parentUI from TGUserInterface
    }

    public void preActivate() {}
    public void postDeactivate() {}
    public abstract void mousePaused(MouseEvent e);
    public abstract void mouseMoved(MouseEvent e);
    public abstract void mouseDragged(MouseEvent e);

    class PauseThread extends Thread{
        boolean resetSleep;
        boolean cancelled;
           PauseThread() { cancelled = false; start(); }

           void reset() { resetSleep = true; cancelled = false; }
           void cancel() { cancelled = true; }

           public void run() {
               try {
                   do {  resetSleep=false; sleep(250); } while (resetSleep);
                if (!cancelled) {
                    MouseEvent pausedEvent =
                        new MouseEvent(tgPanel,MouseEvent.MOUSE_ENTERED, 0,0, mousePos.x,mousePos.y,0,false);
                    mousePaused(pausedEvent);
                }
               }
               catch (Exception e) {e.printStackTrace();}
           }
    }

    public void resetPause() {
        if (pauseThread!=null && pauseThread.isAlive()) pauseThread.reset();
        else pauseThread = new PauseThread();
    }

    public void cancelPause() {
        if (pauseThread!=null && pauseThread.isAlive()) pauseThread.cancel();
    }

    private class AMPUIMouseMotionListener implements MouseMotionListener {
        public void mouseMoved(MouseEvent e) {
              mousePos=e.getPoint();
              resetPause();
              TGAbstractMousePausedUI.this.mouseMoved(e);
        }

        public void mouseDragged(MouseEvent e) {
              mousePos=e.getPoint();
              resetPause();
              TGAbstractMousePausedUI.this.mouseDragged(e);
        }
    }

    private class AMPUIMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
              cancelPause();
        }
        public void mouseReleased(MouseEvent e) {
              cancelPause();
        }
        public void mouseExited(MouseEvent e) {
              //cancelPause();
        }
    }

} // end com.touchgraph.graphlayout.interaction.TGAbstractMousePauseUI
