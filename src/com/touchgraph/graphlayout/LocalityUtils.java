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

import  com.touchgraph.graphlayout.graphelements.*;

import  java.util.*;

/** LocalityUtils:  Utilities for switching locality.  Animation effects
  * require a reference to TGPanel.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: LocalityUtils.java,v 1.2 2002/09/20 14:00:35 ldornbusch Exp $
  */
public class LocalityUtils {

    TGPanel tgPanel;
    Locality locality;

    public static final int INFINITE_LOCALITY_RADIUS = Integer.MAX_VALUE;

    ShiftLocaleThread shiftLocaleThread;
    boolean fastFinishShift=false;  // If finish fast is true, quickly wrap up animation

    public LocalityUtils(Locality loc, TGPanel tgp) {
        locality = loc;
        tgPanel = tgp;
    }

    public void fastFinishAnimation() {
        fastFinishShift = true;
    }

    /** Mark for deletion nodes not contained within distHash. */
    private synchronized boolean markDistantNodes(final Hashtable subgraph) {//Collection subgraph) {
        final boolean[] someNodeWasMarked = new boolean[1];
        someNodeWasMarked[0] = false;
//        Boolean x;
        TGForEachNode fen = new TGForEachNode() {
            public void forEachNode(Node n) {
//							if(!subgraph.contains(n)) {
                if(!subgraph.containsKey(n)) {
                    n.markedForRemoval=true;
                    someNodeWasMarked[0] = true;
                }
            }
        };

        locality.forAllNodes(fen);
        return someNodeWasMarked[0];
    }

    private synchronized void removeMarkedNodes() {
        final Vector nodesToRemove = new Vector();

        TGForEachNode fen = new TGForEachNode() {
            public void forEachNode(Node n) {
                if(n.markedForRemoval)  {
                    nodesToRemove.addElement(n);
                    n.markedForRemoval=false;
                    n.massfade=1;
                }
            }
        };
        synchronized(locality) {
            locality.forAllNodes(fen);
            locality.removeNodes(nodesToRemove);
        }
    }

    /** Add to locale nodes within radius distance of a focal node. */
    private synchronized void addNearNodes(Hashtable distHash, int radius) throws TGException {
        for ( int r=0; r<radius+1; r++ ) {
            Enumeration localNodes = distHash.keys();
            while (localNodes.hasMoreElements()) {
                Node n = (Node)localNodes.nextElement();
                if(!locality.contains(n) && ((Integer)distHash.get(n)).intValue()<=r) {
                    n.massfade=1;
                    n.justMadeLocal = true;                    
                    locality.addNodeWithEdges(n);
                    if (!fastFinishShift) {                        
                        try { 
                        	if(radius==1) Thread.currentThread().sleep(50); 
                        	else Thread.currentThread().sleep(50); 
                        }
                        catch (InterruptedException ex) {}                        
                    }
                }
            }
        }
    }

    private synchronized void unmarkNewAdditions() {
        TGForEachNode fen = new TGForEachNode() {
            public void forEachNode(Node n) {
                n.justMadeLocal=false;
            }
        };
        locality.forAllNodes(fen);
    }

    /** The thread that gets instantiated for doing the locality shift animation. */
    class ShiftLocaleThread extends Thread {
        Hashtable distHash;
        Node focusNode;
        int radius;
        int maxAddEdgeCount;
        int maxExpandEdgeCount;
        boolean unidirectional;

        ShiftLocaleThread(Node n, int r, int maec, int meec, boolean unid) {
            focusNode = n;
            radius = r;
            maxAddEdgeCount = maec;
            maxExpandEdgeCount = meec;
            unidirectional = unid;
            start();

        }

        public void run() {        	
            synchronized (LocalityUtils.this) {            
                if (!locality.getCompleteEltSet().contains(focusNode)) return;
                tgPanel.stopDamper();
                distHash = GESUtils.calculateDistances(
                             locality.getCompleteEltSet(),focusNode,radius,maxAddEdgeCount,maxExpandEdgeCount,unidirectional);
                try {                	
                	if (radius==1) {
                		addNearNodes(distHash,radius);
                    	for (int i=0;i<4&&!fastFinishShift;i++) {
                        	Thread.currentThread().sleep(100);
                    	}                     	                   	
                		unmarkNewAdditions();
                		for (int i=0;i<4&&!fastFinishShift;i++) {
                        	Thread.currentThread().sleep(100);
                    	}
                	}
                    if (markDistantNodes(distHash)){//.keySet())) {// markDistantNodes will use a Collection..
                         for (int i=0;i<8&&!fastFinishShift;i++) {
                             Thread.currentThread().sleep(100);
                         }
                    }                    
                    removeMarkedNodes();                    
                    for (int i=0;i<1&&!fastFinishShift;i++) {
                        if(radius>1) Thread.currentThread().sleep(100);
                    }
                    if(radius!=1) {
                    	addNearNodes(distHash,radius);
                    	for (int i=0;i<4&&!fastFinishShift;i++) {
                        	Thread.currentThread().sleep(100);
                    	}
                    	unmarkNewAdditions();
                	}
                    
                } catch ( TGException tge ) {
                    System.err.println("TGException: " + tge.getMessage());
                } catch (InterruptedException ex) {}
                tgPanel.resetDamper();
            }
        }
    }

    public void setLocale(Node n, final int radius, final int maxAddEdgeCount, final int maxExpandEdgeCount,
                          final boolean unidirectional) throws TGException {
        if (n==null || radius<0) return;
        if(shiftLocaleThread!=null && shiftLocaleThread.isAlive()) {
            fastFinishShift=true; //This should cause last locale shift to finish quickly
            while(shiftLocaleThread.isAlive())
                try { Thread.currentThread().sleep(100); }
                catch (InterruptedException ex) {}
        }
        if (radius == INFINITE_LOCALITY_RADIUS || n==null) {
            addAllGraphElts();
            tgPanel.resetDamper();
            return;
        }

        fastFinishShift=false;
        shiftLocaleThread=new ShiftLocaleThread(n, radius, maxAddEdgeCount, maxExpandEdgeCount, unidirectional);
    }

    public void setLocale(Node n, final int radius) throws TGException {
        setLocale(n,radius,1000,1000, false);
    }

    public synchronized void addAllGraphElts() throws TGException {
        locality.addAll();
    }

   /** Add to locale nodes that are one edge away from a given node.
     * This method does not utilize "fastFinishShift" so it's likely that
     * synchronization errors will occur.
     */
    public void expandNode(final Node n) {
        new Thread() {
            public void run() {
                synchronized (LocalityUtils.this) {
                    if (!locality.getCompleteEltSet().contains(n)) return;
                    tgPanel.stopDamper();
                    for(int i=0;i<n.edgeCount();i++) {
                        Node newNode = n.edgeAt(i).getOtherEndpt(n);
                        if (!locality.contains(newNode)) {
                            newNode.justMadeLocal = true;
                            try {
                                locality.addNodeWithEdges(newNode);
                                Thread.currentThread().sleep(50);
                            } catch ( TGException tge ) {
                                System.err.println("TGException: " + tge.getMessage());
                            } catch ( InterruptedException ex ) {}
                        }
                        else if (!locality.contains(n.edgeAt(i))) {
                            locality.addEdge(n.edgeAt(i));
                        }
                    }
                    try { Thread.currentThread().sleep(200); }
                    catch (InterruptedException ex) {}
                    unmarkNewAdditions();
                    tgPanel.resetDamper();
                }
            }
        }.start();
    }

   /** Hides a node, and all the nodes attached to it. */
    public synchronized void hideNode( final Node hideNode ) {
        if (hideNode==null) return;
        new Thread() {
            public void run() {
                synchronized(LocalityUtils.this) {
                    if (!locality.getCompleteEltSet().contains(hideNode)) return;

                    locality.removeNode(hideNode); //Necessary so that node is ignored in distances calculation.
                    if (hideNode==tgPanel.getSelect()) {
                        tgPanel.clearSelect();
                    }


//									Collection subgraph = GESUtils.getLargestConnectedSubgraph(locality);
										Hashtable subgraph = GESUtils.getLargestConnectedSubgraph(locality);
                    markDistantNodes(subgraph);
                    tgPanel.repaint();                    
                    try { Thread.currentThread().sleep(200); }
                    catch (InterruptedException ex) {}
                    removeMarkedNodes();

                    tgPanel.resetDamper();
                }
            }
        }.start();
    }

   /** Opposite of expand node, works like hide node except that the selected node is not hidden.*/
    public synchronized void collapseNode( final Node collapseNode ) {
        if (collapseNode==null) return;
        new Thread() {
            public void run() {
                synchronized(LocalityUtils.this) {
                    if (!locality.getCompleteEltSet().contains(collapseNode)) return;

                    locality.removeNode(collapseNode); //Necessary so that node is ignored in distances calculation.
//										Collection subgraph = GESUtils.getLargestConnectedSubgraph(locality);
										Hashtable subgraph = GESUtils.getLargestConnectedSubgraph(locality);
                    markDistantNodes(subgraph);
                    try {
                        locality.addNodeWithEdges(collapseNode); // Add the collapsed node back in.
                    }
                    catch (TGException tge) { tge.printStackTrace(); }
                    tgPanel.repaint();
                    tgPanel.resetDamper();
                    try { Thread.currentThread().sleep(600); }
                    catch (InterruptedException ex) {}
                    removeMarkedNodes();

                    tgPanel.resetDamper();
                }
            }
        }.start();
    }
} // end com.touchgraph.graphlayout.LocalityUtils
