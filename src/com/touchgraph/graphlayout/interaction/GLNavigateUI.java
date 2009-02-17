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
//import  javax.swing.event.*;

/** GLNavigateUI. User interface for moving around the graph, as opposed
  * to editing.
  *
  * @author   Alexander Shapiro
  * @author   Murray Altheim (abstracted GLPanel to TGScrollPane interface)
  * @version  1.22-jre1.1 $Id: GLNavigateUI.java,v 1.1 2002/09/19 15:58:21 ldornbusch Exp $
  */
public class GLNavigateUI extends TGUserInterface {

    GLPanel glPanel;
    TGPanel tgPanel;

    GLNavigateMouseListener ml;

    TGAbstractDragUI hvDragUI;
    TGAbstractDragUI rotateDragUI;
    //TGAbstractDragUI hvRotateDragUI;

    DragNodeUI dragNodeUI;
    LocalityScroll localityScroll;
    PopupMenu nodePopup;
    PopupMenu edgePopup;
    Node popupNode;
    Edge popupEdge;

    public GLNavigateUI( GLPanel glp ) {
        glPanel = glp;
        tgPanel = glPanel.getTGPanel();

        localityScroll = glPanel.getLocalityScroll();
        hvDragUI = glPanel.getHVScroll().getHVDragUI();
        rotateDragUI = glPanel.getRotateScroll().getRotateDragUI();
        //hvRotateDragUI = new HVRotateDragUI(tgPanel,
        //        glPanel.getHVScroll(), glPanel.getRotateScroll());        
        dragNodeUI = new DragNodeUI(tgPanel);

        ml = new GLNavigateMouseListener();
        setUpNodePopup(glp);
        setUpEdgePopup(glp);
    }

    public void activate() {
        tgPanel.addMouseListener(ml);
    }

    public void deactivate() {
        tgPanel.removeMouseListener(ml);
    }

    class GLNavigateMouseListener extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            Node mouseOverN = tgPanel.getMouseOverN();

            if (e.getModifiers() == MouseEvent.BUTTON1_MASK) {
                if (mouseOverN == null)
                    hvDragUI.activate(e);
                else
                    dragNodeUI.activate(e);
            }
        }

        public void mouseClicked(MouseEvent e) {
            Node mouseOverN = tgPanel.getMouseOverN();
            if (e.getModifiers() == MouseEvent.BUTTON1_MASK) {
                if ( mouseOverN != null) {
                        tgPanel.setSelect(mouseOverN);						
			            glPanel.getHVScroll().slowScrollToCenter(mouseOverN);

                        try {
                            tgPanel.setLocale(mouseOverN, localityScroll.getLocalityRadius());
                        }
                        catch (TGException ex) {
                           System.out.println("Error setting locale");
                            ex.printStackTrace();
                        }                        
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popupNode = tgPanel.getMouseOverN();
                popupEdge = tgPanel.getMouseOverE();
                if (popupNode!=null) {
                    tgPanel.setMaintainMouseOver(true);
//									nodePopup.show(e.getComponent(), e.getX(), e.getY());
									nodePopup.show(tgPanel, e.getX(), e.getY());
                }
                else if (popupEdge!=null) {
                    tgPanel.setMaintainMouseOver(true);
//									edgePopup.show(e.getComponent(), e.getX(), e.getY());
										edgePopup.show(tgPanel, e.getX(), e.getY());
                  }
                else {
//									glPanel.glPopup.show(e.getComponent(), e.getX(), e.getY());
									glPanel.glPopup.show(tgPanel, e.getX(), e.getY());
                }
            }
					else tgPanel.setMaintainMouseOver(false);
        }

    }

    private void setUpNodePopup( GLPanel glp) {
        nodePopup = new PopupMenu();
				glp.add(nodePopup);
				MenuItem menuItem;

        menuItem = new MenuItem("Expand Node");
        ActionListener expandAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(popupNode!=null) {
                        tgPanel.expandNode(popupNode);
                    }
// JDK11 Change .. because of MenuBecomesInvisible
										tgPanel.setMaintainMouseOver(false);
										tgPanel.setMouseOverN(null);
										tgPanel.repaint();
// JDK11 Change .. because of MenuBecomesInvisible
                }
            };

        menuItem.addActionListener(expandAction);
        nodePopup.add(menuItem);

        menuItem = new MenuItem("Collapse Node");
        ActionListener collapseAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(popupNode!=null) {
                        tgPanel.collapseNode(popupNode );
                    }
// JDK11 Change .. because of MenuBecomesInvisible
									tgPanel.setMaintainMouseOver(false);
									tgPanel.setMouseOverN(null);
									tgPanel.repaint();
// JDK11 Change .. because of MenuBecomesInvisible
                }
            };

        menuItem.addActionListener(collapseAction);
        nodePopup.add(menuItem);

        menuItem = new MenuItem("Hide Node");
        ActionListener hideAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(popupNode!=null) {
                        tgPanel.hideNode(popupNode );
                    }
// JDK11 Change .. because of MenuBecomesInvisible
										tgPanel.setMaintainMouseOver(false);
										tgPanel.setMouseOverN(null);
										tgPanel.repaint();
// JDK11 Change .. because of MenuBecomesInvisible
                }
            };

        menuItem.addActionListener(hideAction);
        nodePopup.add(menuItem);

      menuItem = new MenuItem("Center Node");
      ActionListener centerAction = new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  if(popupNode!=null) {
                      glPanel.getHVScroll().slowScrollToCenter(popupNode);
                  }
// JDK11 Change .. because of MenuBecomesInvisible
                  tgPanel.setMaintainMouseOver(false);
                  tgPanel.setMouseOverN(null);
                  tgPanel.repaint();
// JDK11 Change .. because of MenuBecomesInvisible
              }
          };
      menuItem.addActionListener(centerAction);
      nodePopup.add(menuItem);


			/*       nodePopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                tgPanel.setMaintainMouseOver(false);
                tgPanel.setMouseOverN(null);
                tgPanel.repaint();
            }
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
        });*/

    }

    private void setUpEdgePopup( GLPanel glp) {
        edgePopup = new PopupMenu();
				glp.add(edgePopup);
        MenuItem menuItem;

        menuItem = new MenuItem("Hide Edge");
        ActionListener hideAction = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if(popupEdge!=null) {
                        tgPanel.hideEdge(popupEdge);
                    }
// JDK11 Change .. because of MenuBecomesInvisible
									tgPanel.setMaintainMouseOver(false);
									tgPanel.setMouseOverN(null);
									tgPanel.repaint();
// JDK11 Change .. because of MenuBecomesInvisible
                }
            };

        menuItem.addActionListener(hideAction);
        edgePopup.add(menuItem);

/*        edgePopup.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuCanceled(PopupMenuEvent e) {}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                tgPanel.setMaintainMouseOver(false);
                tgPanel.setMouseOverE(null);
                tgPanel.repaint();
            }
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
        });*/
    }

} // end com.touchgraph.graphlayout.interaction.GLNavigateUI
