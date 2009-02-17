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
import  java.util.Vector;

/** TGUIManager switches between major user interfaces, and allows
  * them to be referred to by name.  This will probably come in handy
  * when switching user interfaces from menus.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: TGUIManager.java,v 1.1 2002/09/19 15:58:21 ldornbusch Exp $
  */
public class TGUIManager {

    Vector userInterfaces;

  // ............

   /** Default constructor.
     */
    public TGUIManager() {
        userInterfaces = new Vector();
    }

    class NamedUI {
        TGUserInterface ui;
        String name;

        NamedUI( TGUserInterface ui, String n ) {
            this.ui = ui;
            name = n;
        }
    }

    public void addUI( TGUserInterface ui, String name ) {
        userInterfaces.addElement(new NamedUI(ui,name));
    }

    public void addUI( TGUserInterface ui ) {
        addUI(ui,null);
    }

    public void removeUI( String name ) {
        for (int i=0;i<userInterfaces.size();i++)
            if (((NamedUI) userInterfaces.elementAt(i)).name.equals(name)) userInterfaces.removeElementAt(i);

    }

    public void removeUI( TGUserInterface ui ) {
        for (int i=0;i<userInterfaces.size();i++)
            if (((NamedUI) userInterfaces.elementAt(i)).ui==ui) userInterfaces.removeElementAt(i);

    }

    public void activate( String name ) {
        for (int i=0;i<userInterfaces.size();i++) {
            NamedUI namedInterf = (NamedUI) userInterfaces.elementAt(i);
            TGUserInterface ui=namedInterf.ui;
            if (((NamedUI) userInterfaces.elementAt(i)).name.equals(name)) ui.activate();
            else ui.deactivate();
        }
    }

    public void activate( TGUserInterface ui ) {
        for (int i=0;i<userInterfaces.size();i++) {
            if (((NamedUI) userInterfaces.elementAt(i)).ui==ui) ui.activate();
            else ui.deactivate();
        }
    }

} // end com.touchgraph.graphlayout.interaction.TGUIManager
