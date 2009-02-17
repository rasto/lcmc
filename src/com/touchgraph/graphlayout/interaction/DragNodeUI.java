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
import  java.applet.*;
import  java.awt.event.*;

/** DragNodeUI contains code for dragging nodes.
  *
  * <p><b>
  * Parts of this code build upon Sun's Graph Layout example.
  * http://java.sun.com/applets/jdk/1.1/demo/GraphLayout/Graph.java
  * </b></p>
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: DragNodeUI.java,v 1.2 2002/09/20 19:41:06 ldornbusch Exp $
  */
public class DragNodeUI extends TGAbstractDragUI{

   /** Stores the distance between the cursor and the center of the node
     * when dragging occurs so that the cursor remains at the same position
     * on the node otherwise, the cursor jumps to the center of the node.
     */
    public Point dragOffs;

  // ............

    /** Constructor with TGPanel <tt>tgp</tt>.
      */
    public DragNodeUI( TGPanel tgp ) {
        super(tgp);
    }

    public void preActivate() {
        if (dragOffs ==null) dragOffs=new Point(0,0);
    }

    public void preDeactivate() {};

    public void mousePressed( MouseEvent e ) {
        Node mouseOverN = tgPanel.getMouseOverN();
        Point mousePos;

        if (e!=null) mousePos = e.getPoint(); //e can be null if the wrong activate() method was used
        else mousePos= new Point((int) mouseOverN.drawx,(int) mouseOverN.drawy);

        if ( mouseOverN != null) { //Should never be null if TGUIManager works properly
            tgPanel.setDragNode(mouseOverN);

            dragOffs.setLocation((int) (mouseOverN.drawx-mousePos.x), //For when you click to the side of
                    (int)(mouseOverN.drawy-mousePos.y));//the node, but you still want to drag it
        }
    }

    public void mouseReleased( MouseEvent e ) {
        tgPanel.setDragNode(null);
        tgPanel.repaintAfterMove();
        tgPanel.startDamper();
    }


    public synchronized void mouseDragged( MouseEvent e ) {

        Node dragNode = tgPanel.getDragNode();
        dragNode.drawx = e.getX()+dragOffs.x;
        dragNode.drawy = e.getY()+dragOffs.y;
        tgPanel.updatePosFromDraw(dragNode);
        tgPanel.repaintAfterMove();
        tgPanel.stopDamper(); //Keep the graph alive while dragging.
        e.consume();

    }

} // end com.touchgraph.graphlayout.interaction.DragNodeUI
