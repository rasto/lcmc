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

import  java.awt.event.*;
import java.awt.*;
//import  javax.swing.*;

/** ZoomScroll:  Contains code for enlarging the graph by zooming in.
  *
  * @author   Alexander Shapiro
  * @version  1.21  $Id: ZoomScroll.java,v 1.2 2002/09/23 18:45:48 ldornbusch Exp $
  */
public class ZoomScroll implements GraphListener {

    protected ZoomLens zoomLens;
    private Scrollbar zoomSB;
    private TGPanel tgPanel;

  // ............

   /** Constructor with TGPanel <tt>tgp</tt>.
     */
    public ZoomScroll( TGPanel tgp ) {
        tgPanel=tgp;
        zoomSB = new Scrollbar(Scrollbar.HORIZONTAL, -4, 7, -31, 19);
        zoomSB.addAdjustmentListener(new zoomAdjustmentListener());
        zoomLens=new ZoomLens();
        tgPanel.addGraphListener(this);
    }

    public Scrollbar getZoomSB() { return zoomSB; }

    public ZoomLens getLens() { return zoomLens; }

    public void graphMoved() {} //From GraphListener interface
    public void graphReset() { zoomSB.setValue(-10); } //From GraphListener interface

    public int getZoomValue() {
        double orientedValue = zoomSB.getValue()-zoomSB.getMinimum();
        double range = zoomSB.getMaximum()-zoomSB.getMinimum()-zoomSB.getVisibleAmount();
        return (int) ((orientedValue/range)*200-100);
    }

    public void setZoomValue(int value) {
        double range = zoomSB.getMaximum()-zoomSB.getMinimum()-zoomSB.getVisibleAmount();
        zoomSB.setValue((int) ((value+100)/200.0 * range+0.5)+zoomSB.getMinimum());
    }
    
    private class zoomAdjustmentListener implements AdjustmentListener {
        public void adjustmentValueChanged(AdjustmentEvent e) {
        tgPanel.repaintAfterMove();
        }
    }

    class ZoomLens extends TGAbstractLens {
        protected void applyLens(TGPoint2D p) {
            p.x=p.x*Math.pow(2,zoomSB.getValue()/10.0);
            p.y=p.y*Math.pow(2,zoomSB.getValue()/10.0);

        }

        protected void undoLens(TGPoint2D p) {
            p.x=p.x/Math.pow(2,zoomSB.getValue()/10.0);
            p.y=p.y/Math.pow(2,zoomSB.getValue()/10.0);
        }
    }

} // end com.touchgraph.graphlayout.interaction.ZoomScroll
