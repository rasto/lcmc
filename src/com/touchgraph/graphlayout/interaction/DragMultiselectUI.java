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

/** DragMultiselectUI contains code for selecting a group on nodes
  * by enclosing them in a dotted box.
  *   
  * @author   Alexander Shapiro                                        
  * @version  1.22-jre1.1  $Id: DragMultiselectUI.java,v 1.1 2002/09/19 15:58:21 ldornbusch Exp $
  */
public class DragMultiselectUI extends TGAbstractDragUI implements TGPaintListener {
        
    TGPoint2D mousePos=null;
    TGPoint2D startPos = null;
    
    DragMultiselectUI( TGPanel tgp ) {
        super(tgp); 
    }
    
    public void preActivate() {    
        startPos = null;
        mousePos = null;
        tgPanel.addPaintListener(this);
    }
    
    public void preDeactivate() {
        tgPanel.removePaintListener(this);
        tgPanel.repaint();
    };
        
          
    public void mousePressed(MouseEvent e) {
        startPos = new TGPoint2D(e.getX(), e.getY());
        mousePos = new TGPoint2D(startPos);
    }    
    
        
    public void mouseReleased(MouseEvent e) {}    

    public void mouseDragged(MouseEvent e) {    
        mousePos.setLocation(e.getX(), e.getY());
        tgPanel.multiSelect(startPos,mousePos);
        tgPanel.repaint();
    }
    

    
    public void paintFirst(Graphics g) {};
    public void paintAfterEdges(Graphics g) {};
    
    public void paintLast(Graphics g) {

        if(mousePos==null) return;

        g.setColor(Color.black);
        
        int x,y,w,h;
        
        if (startPos.x<mousePos.x) {
            x=(int) startPos.x;
            w=(int) (mousePos.x-startPos.x);
        }
        else {
            x=(int) mousePos.x;
            w=(int) (startPos.x-mousePos.x);
        }

        if (startPos.y<mousePos.y) {
            y=(int) startPos.y;
            h=(int) (mousePos.y-startPos.y);
        }
        else {
            y=(int) mousePos.y;
            h=(int) (startPos.y-mousePos.y);
        }
        
        //God, where are the line styles when you need them
        for(int horiz = x;horiz<x+w;horiz+=2){
            g.drawLine(horiz,y,horiz,y);      //Drawing lines because there is no way
            g.drawLine(horiz,y+h,horiz,y+h);  //to draw a single pixel.
        }
        for(int vert = y;vert<y+h;vert+=2){
            g.drawLine(x,vert,x,vert);
            g.drawLine(x+w,vert,x+w,vert);
        }  
        
    }

} // end
