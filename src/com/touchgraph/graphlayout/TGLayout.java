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

/**  TGLayout is the thread responsible for graph layout.  It updates
  *  the real coordinates of the nodes in the graphEltSet object.
  *  TGPanel sends it resetDamper commands whenever the layout needs
  *  to be adjusted.  After every adjustment cycle, TGLayout triggers
  *  a repaint of the TGPanel.
  *
  * ********************************************************************
  *  This is the heart of the TouchGraph application.  Please provide a
  *  Reference to TouchGraph.com if you are influenced by what you see
  *  below.  Your cooperation will insure that this code remains
  *  opensource
  * ********************************************************************
  *
  * <p><b>
  *  Parts of this code build upon Sun's Graph Layout example.
  *  http://java.sun.com/applets/jdk/1.1/demo/GraphLayout/Graph.java
  * </b></p>
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: TGLayout.java,v 1.1 2002/09/19 15:58:08 ldornbusch Exp $
  */
public class TGLayout implements Runnable {

    //private ImmutableGraphEltSet graphEltSet;
    private TGPanel tgPanel;
    private Thread relaxer;
    private double damper=0.0;      // A low damper value causes the graph to move slowly
    private double maxMotion=0;     // Keep an eye on the fastest moving node to see if the graph is stabilizing
    private double lastMaxMotion=0;
    private double motionRatio = 0; // It's sort of a ratio, equal to lastMaxMotion/maxMotion-1
    private boolean damping = true; // When damping is true, the damper value decreases

    private double rigidity = 1;    // Rigidity has the same effect as the damper, except that it's a constant
                                    // a low rigidity value causes things to go slowly.
                                    // a value that's too high will cause oscillation
    private double newRigidity = 1;

    Node dragNode=null;

  // ............

  /** Constructor with a supplied TGPanel <tt>tgp</tt>.
    */
    public TGLayout( TGPanel tgp ) {
        tgPanel = tgp;
      //graphEltSet = tgPanel.getGES();
        relaxer = null;
    }

    void setRigidity(double r) {
        newRigidity = r;  //update rigidity at the end of the relax() thread
    }

    void setDragNode(Node n) {
        dragNode = n;
    }


    //relaxEdges is more like tense edges up.  All edges pull nodes closes together;
    private synchronized void relaxEdges() {
        TGForEachEdge fee = new TGForEachEdge() {
            public void forEachEdge(Edge e) {

                   double vx = e.to.x - e.from.x;
                double vy = e.to.y - e.from.y;
                double len = Math.sqrt(vx * vx + vy * vy);

                double dx=vx*rigidity;  //rigidity makes edges tighter
                double dy=vy*rigidity;

                dx /=(e.getLength()*100);
                dy /=(e.getLength()*100);

                // Edges pull directly in proportion to the distance between the nodes. This is good,
                // because we want the edges to be stretchy.  The edges are ideal rubberbands.  They
                // They don't become springs when they are too short.  That only causes the graph to
                // oscillate.

				
                if (e.to.justMadeLocal || e.to.markedForRemoval || (!e.from.justMadeLocal && !e.from.markedForRemoval)) {
                	e.to.dx -= dx*len;
                    e.to.dy -= dy*len;
                } else {
                	double massfade = (e.from.markedForRemoval ? e.from.massfade : 1-e.from.massfade);
                	massfade*=massfade;
                    e.to.dx -= dx*len*massfade;
                    e.to.dy -= dy*len*massfade;
                }
                if (e.from.justMadeLocal || e.from.markedForRemoval || (!e.to.justMadeLocal && !e.to.markedForRemoval)) {                
                    e.from.dx += dx*len;
                    e.from.dy += dy*len;
                } else {
                	double massfade = (e.to.markedForRemoval ? e.to.massfade : 1-e.to.massfade);
                	massfade*=massfade;
                    e.from.dx += dx*len*massfade;
                    e.from.dy += dy*len*massfade;
                }
            }
        };

        tgPanel.getGES().forAllEdges(fee);
    }

/*
    private synchronized void avoidLabels() {
        for (int i = 0 ; i < graphEltSet.nodeNum() ; i++) {
            Node n1 = graphEltSet.nodeAt(i);
            double dx = 0;
            double dy = 0;

            for (int j = 0 ; j < graphEltSet.nodeNum() ; j++) {
                if (i == j) {
                    continue; // It's kind of dumb to do things this way. j should go from i+1 to nodeNum.
                }
                Node n2 = graphEltSet.nodeAt(j);
                double vx = n1.x - n2.x;
                double vy = n1.y - n2.y;
                double len = vx * vx + vy * vy; // so it's length squared
                if (len == 0) {
                    dx += Math.random(); // If two nodes are right on top of each other, randomly separate
                    dy += Math.random();
                } else if (len <600*600) { //600, because we don't want deleted nodes to fly too far away
                    dx += vx / len;  // If it was sqrt(len) then a single node surrounded by many others will
                    dy += vy / len;  // always look like a circle.  This might look good at first, but I think
                                     // it makes large graphs look ugly + it contributes to oscillation.  A
                                     // linear function does not fall off fast enough, so you get rough edges
                                     // in the 'force field'

                }
            }
            n1.dx += dx*100*rigidity;  // rigidity makes nodes avoid each other more.
            n1.dy += dy*100*rigidity;  // I was surprised to see that this exactly balances multiplying edge tensions
                                       // by the rigidity, and thus has the effect of slowing the graph down, while
                                       // keeping it looking identical.

        }
    }
*/

    private synchronized void avoidLabels() {
        TGForEachNodePair fenp = new TGForEachNodePair() {
            public void forEachNodePair(Node n1, Node n2) {
                double dx=0;
                double dy=0;
                double vx = n1.x - n2.x;
                double vy = n1.y - n2.y;
                double len = vx * vx + vy * vy; //so it's length squared
                if (len == 0) {
                    dx = Math.random(); //If two nodes are right on top of each other, randomly separate
                    dy = Math.random();
                } else if (len <600*600) { //600, because we don't want deleted nodes to fly too far away
                    dx = vx / len;  // If it was sqrt(len) then a single node surrounded by many others will
                    dy = vy / len;  // always look like a circle.  This might look good at first, but I think
                                    // it makes large graphs look ugly + it contributes to oscillation.  A
                                    // linear function does not fall off fast enough, so you get rough edges
                                    // in the 'force field'
                }			

                int repSum = n1.repulsion * n2.repulsion/100;                              
                if(n1.justMadeLocal || n1.markedForRemoval || (!n2.justMadeLocal && !n2.markedForRemoval)) {  
                    n1.dx += dx*repSum*rigidity;
                    n1.dy += dy*repSum*rigidity;
                }
                else {
                	double massfade = (n2.markedForRemoval ? n2.massfade : 1-n2.massfade);
                	massfade*=massfade;
                    n1.dx += dx*repSum*rigidity*massfade;
                    n1.dy += dy*repSum*rigidity*massfade;
                }
                if (n2.justMadeLocal || n2.markedForRemoval || (!n1.justMadeLocal && !n1.markedForRemoval)) {
                  	n2.dx -= dx*repSum*rigidity;                  		
                    n2.dy -= dy*repSum*rigidity;
                }
                else {
                    double massfade = (n1.markedForRemoval ? n1.massfade : 1-n1.massfade);
                	massfade*=massfade;
                    n2.dx -= dx*repSum*rigidity*massfade;
                    n2.dy -= dy*repSum*rigidity*massfade;
                }               
            }
        };

        tgPanel.getGES().forAllNodePairs(fenp);
    }

    public void startDamper() {
        damping = true;
    }

    public void stopDamper() {
        damping = false;
        damper = 1.0;     //A value of 1.0 means no damping
    }

    public void resetDamper() {  //reset the damper, but don't keep damping.
        damping = true;
        damper = 1.0;
    }

    public void stopMotion() {  // stabilize the graph, but do so gently by setting the damper to a low value
        damping = true;
        if (damper>0.3) 
            damper = 0.3;
        else
            damper = 0;
    }

    public void damp() {
        if (damping) {
            if(motionRatio<=0.001) {  //This is important.  Only damp when the graph starts to move faster
                                      //When there is noise, you damp roughly half the time. (Which is a lot)
                                      //
                                      //If things are slowing down, then you can let them do so on their own,
                                      //without damping.

                //If max motion<0.2, damp away
                //If by the time the damper has ticked down to 0.9, maxMotion is still>1, damp away
                //We never want the damper to be negative though
                if ((maxMotion<0.2 || (maxMotion>1 && damper<0.9)) && damper > 0.01) damper -= 0.01;
                //If we've slowed down significanly, damp more aggresively (then the line two below)
                else if (maxMotion<0.4 && damper > 0.003) damper -= 0.003;
                //If max motion is pretty high, and we just started damping, then only damp slightly
                else if(damper>0.0001) damper -=0.0001;
            }
        }
        if(maxMotion<0.001 && damping) {
            damper=0;
        }
    }


    private synchronized void moveNodes() {
        lastMaxMotion = maxMotion;
        final double[] maxMotionA = new double[1];
        maxMotionA[0]=0;

        TGForEachNode fen = new TGForEachNode() {
            public void forEachNode(Node n) {
                double dx = n.dx;
                double dy = n.dy;
                dx*=damper;  //The damper slows things down.  It cuts down jiggling at the last moment, and optimizes
                dy*=damper;  //layout.  As an experiment, get rid of the damper in these lines, and make a
                             //long straight line of nodes.  It wiggles too much and doesn't straighten out.

                n.dx=dx/2;   //Slow down, but don't stop.  Nodes in motion store momentum.  This helps when the force
                n.dy=dy/2;   //on a node is very low, but you still want to get optimal layout.

                double distMoved = Math.sqrt(dx*dx+dy*dy); //how far did the node actually move?

                 if (!n.fixed && !(n==dragNode) ) {
                      n.x += Math.max(-30, Math.min(30, dx)); //don't move faster then 30 units at a time.
                    n.y += Math.max(-30, Math.min(30, dy)); //I forget when this is important.  Stopping severed nodes from
                                                        //flying away?
                 }
                 maxMotionA[0]=Math.max(distMoved,maxMotionA[0]);
                
                if(!n.justMadeLocal && !n.markedForRemoval) { n.massfade=1; }
				else { 					 
					if(n.massfade>=0.004) n.massfade-=0.004; 
				}
				

            }
        };

        tgPanel.getGES().forAllNodes(fen);

        maxMotion=maxMotionA[0];
         if (maxMotion>0) motionRatio = lastMaxMotion/maxMotion-1; //subtract 1 to make a positive value mean that
         else motionRatio = 0;                                     //things are moving faster

        damp();

    }

    private synchronized void relax() {
        for (int i=0;i<10;i++) {
          relaxEdges();
          avoidLabels();
          moveNodes();
        }
        if(rigidity!=newRigidity) rigidity= newRigidity; //update rigidity
        tgPanel.repaintAfterMove();
    }

    private void myWait() { //I think it was Netscape that caused me not to use Wait, or was it java 1.1?
        try {
                Thread.sleep(100);
        } catch (InterruptedException e) {
                //break;
        }
    }

    public void run() {
        Thread me = Thread.currentThread();
//      me.setPriority(1);       //Makes standard executable look better, but the applet look worse.
        while (relaxer == me) {
            relax();
            try {
                relaxer.sleep(20);  //Delay to wait for the prior repaint command to finish.
                while(damper<0.1 && damping && maxMotion<0.001) myWait();
              //System.out.println("damping " + damping + " damp " + damper + "  maxM " + maxMotion + "  motR " + motionRatio );
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void start() {
        relaxer = new Thread(this);
        relaxer.start();
    }

    public void stop() {
        relaxer = null;
    }

} // end com.touchgraph.graphlayout.TGLayout
