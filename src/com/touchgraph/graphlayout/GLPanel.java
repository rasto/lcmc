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
import com.touchgraph.graphlayout.interaction.*;
import com.touchgraph.graphlayout.graphelements.*;

import  java.awt.*;
import  java.awt.event.*;
//import  javax.swing.*;
import  java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Enumeration;
import java.net.URL;
import java.io.InputStreamReader;

/** GLPanel contains code for adding scrollbars and interfaces to the TGPanel
  * The "GL" prefix indicates that this class is GraphLayout specific, and
  * will probably need to be rewritten for other applications.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: GLPanel.java,v 1.3 2002/09/23 18:45:56 ldornbusch Exp $
  */
public class GLPanel extends Panel {

    public String zoomLabel = "Zoom"; // label for zoom menu item
    public String rotateLabel = "Rotate"; // label for rotate menu item
	public String localityLabel = "Radius"; // label for locality menu item
	public String hyperLabel = "Hyperbolic"; // label for Hyper menu item

    public HVScroll hvScroll;
    public ZoomScroll zoomScroll;
    public HyperScroll hyperScroll; // unused
    public RotateScroll rotateScroll;
    public LocalityScroll localityScroll;
    public PopupMenu glPopup;
    public Hashtable scrollBarHash; //= new Hashtable();

    protected TGPanel tgPanel;
    protected TGLensSet tgLensSet;
    protected TGUIManager tgUIManager;

    private Scrollbar currentSB =null;


  private Color defaultBackColor = new Color(0x01,0x11,0x44);
  private Color defaultBorderBackColor = new Color(0x02,0x35,0x81);
  private Color defaultForeColor = new Color((float)0.95,(float)0.85,(float)0.55);

  // ............


   /** Default constructor.
     */
    public GLPanel() {
        this.setBackground(defaultBorderBackColor);
        this.setForeground(defaultForeColor);
        scrollBarHash = new Hashtable();
        tgLensSet = new TGLensSet();
        tgPanel = new TGPanel();
        tgPanel.setBackColor(defaultBackColor);
        hvScroll = new HVScroll(tgPanel, tgLensSet);
        zoomScroll = new ZoomScroll(tgPanel);
		hyperScroll = new HyperScroll(tgPanel);
        rotateScroll = new RotateScroll(tgPanel);
        localityScroll = new LocalityScroll(tgPanel);
        initialize();
    }


   /** Initialize panel, lens, and establish a random graph as a demonstration.
     */
    public void initialize() {
        buildPanel();
        buildLens();
        tgPanel.setLensSet(tgLensSet);
        addUIs();
      //tgPanel.addNode();  //Add a starting node.
        try {
            randomGraph();
        } catch ( TGException tge ) {
            System.err.println(tge.getMessage());
            tge.printStackTrace(System.err);
        }
        setVisible(true);
    }

    /** Return the TGPanel used with this GLPanel. */
    public TGPanel getTGPanel() {
        return tgPanel;
    }

  // navigation .................

    /** Return the HVScroll used with this GLPanel. */
    public HVScroll getHVScroll()
    {
        return hvScroll;
    }

    /** Return the HyperScroll used with this GLPanel. */
    public HyperScroll getHyperScroll()
    {
        return hyperScroll;
    }

    /** Sets the horizontal offset to p.x, and the vertical offset to p.y
      * given a Point <tt>p<tt>.
      */
    public void setOffset( Point p ) {
        hvScroll.setOffset(p);
    };

    /** Return the horizontal and vertical offset position as a Point. */
    public Point getOffset() {
        return hvScroll.getOffset();
    };

  // rotation ...................

    /** Return the RotateScroll used with this GLPanel. */
    public RotateScroll getRotateScroll()
    {
        return rotateScroll;
    }

    /** Set the rotation angle of this GLPanel (allowable values between 0 to 359). */
     public void setRotationAngle( int angle ) {
        rotateScroll.setRotationAngle(angle);
    }

    /** Return the rotation angle of this GLPanel. */
    public int getRotationAngle() {
        return rotateScroll.getRotationAngle();
    }

  // locality ...................

    /** Return the LocalityScroll used with this GLPanel. */
    public LocalityScroll getLocalityScroll()
    {
        return localityScroll;
    }

    /** Set the locality radius of this TGScrollPane
      * (allowable values between 0 to 4, or LocalityUtils.INFINITE_LOCALITY_RADIUS).
      */
    public void setLocalityRadius( int radius ) {
        localityScroll.setLocalityRadius(radius);
    }

    /** Return the locality radius of this GLPanel. */
    public int getLocalityRadius() {
        return localityScroll.getLocalityRadius();
    }

  // zoom .......................

    /** Return the ZoomScroll used with this GLPanel. */
    public ZoomScroll getZoomScroll()
    {
        return zoomScroll;
    }

    /** Set the zoom value of this GLPanel (allowable values between -100 to 100). */
    public void setZoomValue( int zoomValue ) {
        zoomScroll.setZoomValue(zoomValue);
    }

    /** Return the zoom value of this GLPanel. */
    public int getZoomValue() {
        return zoomScroll.getZoomValue();
    }

  // ....

    public PopupMenu getGLPopup()
    {
        return glPopup;
    }

    public void buildLens() {
        tgLensSet.addLens(hvScroll.getLens());
        tgLensSet.addLens(zoomScroll.getLens());
		tgLensSet.addLens(hyperScroll.getLens());
        tgLensSet.addLens(rotateScroll.getLens());
        tgLensSet.addLens(tgPanel.getAdjustOriginLens());
    }

    public void buildPanel() {
        final Scrollbar horizontalSB = hvScroll.getHorizontalSB();
        final Scrollbar verticalSB = hvScroll.getVerticalSB();
        final Scrollbar zoomSB = zoomScroll.getZoomSB();
        final Scrollbar rotateSB = rotateScroll.getRotateSB();
		final Scrollbar localitySB = localityScroll.getLocalitySB();
		final Scrollbar hyperSB = hyperScroll.getHyperSB();

        setLayout(new BorderLayout());

        Panel scrollPanel = new Panel();
        scrollPanel.setBackground(defaultBackColor);
        scrollPanel.setForeground(defaultForeColor);
        scrollPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();


        Panel modeSelectPanel = new Panel();
        modeSelectPanel.setBackground(defaultBackColor);
        modeSelectPanel.setForeground(defaultForeColor);
        modeSelectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0,0));

        final Panel topPanel = new Panel();
        topPanel.setBackground(defaultBorderBackColor);
        topPanel.setForeground(defaultForeColor);
        topPanel.setLayout(new GridBagLayout());
        c.gridy=0; c.fill=GridBagConstraints.HORIZONTAL;

        c.gridx=0;c.weightx=0;

        c.insets=new Insets(0,0,0,0);
        c.gridy=0;c.weightx=1;

        scrollBarHash.put(zoomLabel, zoomSB);
        scrollBarHash.put(rotateLabel, rotateSB);
		scrollBarHash.put(localityLabel, localitySB);
		scrollBarHash.put(hyperLabel, hyperSB);

        Panel scrollselect = scrollSelectPanel(new String[] {zoomLabel, rotateLabel /*, localityLabel*/, hyperLabel});
        scrollselect.setBackground(defaultBorderBackColor);
        scrollselect.setForeground(defaultForeColor);
        topPanel.add(scrollselect,c);

        add(topPanel, BorderLayout.SOUTH);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 1; c.weightx = 1; c.weighty = 1;
        scrollPanel.add(tgPanel,c);

        c.gridx = 1; c.gridy = 1; c.weightx = 0; c.weighty = 0;
//        scrollPanel.add(verticalSB,c);    // For WDR We do not need scrollbars

        c.gridx = 0; c.gridy = 2;
 //       scrollPanel.add(horizontalSB,c);  // For WDR We do not need scrollbars

        add(scrollPanel,BorderLayout.CENTER);

        glPopup = new PopupMenu();
	      add(glPopup);	// needed by JDK11 Popupmenu..

        MenuItem menuItem = new MenuItem("Toggle Controls");
        ActionListener toggleControlsAction = new ActionListener() {
                boolean controlsVisible = true;
                public void actionPerformed(ActionEvent e) {
                    controlsVisible = !controlsVisible;
                    horizontalSB.setVisible(controlsVisible);
                    verticalSB.setVisible(controlsVisible);
                    topPanel.setVisible(controlsVisible);
                    GLPanel.this.doLayout();
                }
            };
        menuItem.addActionListener(toggleControlsAction);
        glPopup.add(menuItem);
    }

    protected Panel scrollSelectPanel(final String[] scrollBarNames) {
      final Panel sbp = new Panel(new GridBagLayout());

//    UI: Scrollbarselector via Radiobuttons.................................

      sbp.setBackground(defaultBorderBackColor);
      sbp.setForeground(defaultForeColor);

      Panel firstRow=new Panel(new GridBagLayout());

      final CheckboxGroup bg = new CheckboxGroup();
      
      int cbNumber = scrollBarNames.length;
      Checkbox checkboxes[] = new Checkbox[cbNumber];
      
      GridBagConstraints c = new GridBagConstraints();      
      c.anchor=GridBagConstraints.WEST;
      c.gridy = 0; c.weightx= 0; c.fill = GridBagConstraints.HORIZONTAL;

      for (int i=0;i<cbNumber;i++) {
      	checkboxes[i] = new Checkbox(scrollBarNames[i],true,bg);
        c.gridx = i; 
        firstRow.add(checkboxes[i],c);
      }
      checkboxes[0].setState(true);
      
      c.gridx=cbNumber;c.weightx=1;
      Label lbl = new Label("     Right-click nodes and background for more options");
      firstRow.add(lbl,c);
      	
      class radioItemListener implements ItemListener{
        private String scrollBarName;
        public radioItemListener(String str2Act){
          this.scrollBarName=str2Act;
        }
        public void itemStateChanged(ItemEvent e){
          Scrollbar selectedSB = (Scrollbar) scrollBarHash.get((String) bg.getSelectedCheckbox().getLabel());		 
          if (e.getStateChange()==ItemEvent.SELECTED){
            for (int i = 0;i<scrollBarNames.length;i++) {
                Scrollbar sb = (Scrollbar) scrollBarHash.get(scrollBarNames[i]);
                sb.setVisible(false);
            }
            selectedSB.setBounds(currentSB.getBounds());
            if (selectedSB!=null)
              selectedSB.setVisible(true);
              currentSB = selectedSB;
            sbp.invalidate();
          }
        }
      };

      for (int i=0;i<cbNumber;i++) {      
        checkboxes[i].addItemListener(new radioItemListener(scrollBarNames[0]));
      }
      
      c.anchor = GridBagConstraints.NORTHWEST;
      c.insets=new Insets(1,5,1,5);
      c.gridx = 0; c.gridy = 0; c.weightx = 10;
      c.gridwidth=3;   //Radiobutton UI
      c.gridheight=1;
      c.fill=GridBagConstraints.NONE;
      c.anchor=GridBagConstraints.WEST;
      sbp.add(firstRow,c);
      
      c.gridy=1;
      c.fill=GridBagConstraints.HORIZONTAL;
      for (int i = 0;i<scrollBarNames.length;i++) {
          Scrollbar sb = (Scrollbar) scrollBarHash.get(scrollBarNames[i]);          
          if(sb==null) continue;
          if(currentSB==null) currentSB = sb;
          sbp.add(sb,c);
      }
      
      return sbp;
    }

    public void addUIs() {
        tgUIManager = new TGUIManager();
        GLEditUI editUI = new GLEditUI(this);
        GLNavigateUI navigateUI = new GLNavigateUI(this);
        tgUIManager.addUI(editUI,"Edit");
        tgUIManager.addUI(navigateUI,"Navigate");
        tgUIManager.activate("Navigate");
    }

	public void randomGraph() throws TGException {
        Node n1= tgPanel.addNode();
        n1.setType(0);
        for ( int i=0; i<249; i++ ) {
        	tgPanel.addNode();
    	}
        
        TGForEachNode fen = new TGForEachNode() {
            public void forEachNode(Node n) {
				for(int i=0;i<5;i++) {
				    Node r = tgPanel.getGES().getRandomNode();
				    if(r!=n && tgPanel.findEdge(r,n)==null) 
					    tgPanel.addEdge(r,n,Edge.DEFAULT_LENGTH);	
			    }
			}
		};    	
		tgPanel.getGES().forAllNodes(fen);
		
        tgPanel.setLocale(n1,1);
        tgPanel.setSelect(n1);
        try {
       	    Thread.currentThread().sleep(2000); 
        } catch (InterruptedException ex) {}                    				

        getHVScroll().slowScrollToCenter(n1);
    }    

    public static void main(String [] args) {

        final Frame frame;
        final GLPanel glPanel = new GLPanel();
        frame = new Frame("TouchGraph GraphLayout");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
              frame.remove(glPanel);
              frame.dispose();
            }
        });
        frame.add("Center", glPanel);
        frame.setSize(800,600);
        frame.setVisible(true);
    }


} // end com.touchgraph.graphlayout.GLPanel
