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

package com.touchgraph.graphlayout.graphelements;

import  com.touchgraph.graphlayout.Node;
import  com.touchgraph.graphlayout.Edge;
import  com.touchgraph.graphlayout.TGException;

import  java.util.Vector;

/**  Locality:  A way of representing a subset of a larger set of nodes.
  *  Allows for both manipulation of the subset, and manipulation of the
  *  larger set.  For instance, one can call removeNode to delete it from
  *  the subset, or deleteNode to remove it from the larger set.
  *
  *  Locality is used in conjunction with LocalityUtils, which handle
  *  locality shift animations.
  *
  *  More synchronization will almost definitely be required.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: Locality.java,v 1.1 2002/09/19 15:58:32 ldornbusch Exp $
  */
public class Locality extends GraphEltSet {

    protected GraphEltSet completeEltSet;

  // ............

  /** Constructor with GraphEltSet <tt>ges</tt>.
    */
    public Locality(GraphEltSet ges) {
        super();
        completeEltSet = ges;
    }

    public GraphEltSet getCompleteEltSet() {
        return completeEltSet;
    }

    public synchronized void addNode( Node n ) throws TGException {
        if (!contains(n)) {
            super.addNode(n);
            //If a new Node is created, and then added to Locality, then add the new edge
            //to completeEltSet as well.
            if (!completeEltSet.contains(n)) completeEltSet.addNode(n);
        }
    }

    public void addEdge( Edge e ) {
        if(!contains(e)) {
            edges.addElement(e);
            //If a new Edge is created, and then added to Locality, then add the new edge
            //to completeEltSet as well.
            if (!completeEltSet.contains(e)) completeEltSet.addEdge(e);
        }
    }

    public synchronized void addNodeWithEdges( Node n ) throws TGException {
        addNode(n);
        for (int i = 0 ; i < n.edgeCount(); i++) {
            Edge e=n.edgeAt(i);
            if(contains(e.getOtherEndpt(n))) addEdge(e);
        }

    }
    
    public synchronized void addAll() throws TGException {
        synchronized (completeEltSet) {
            for (int i = 0 ; i<completeEltSet.nodeCount(); i++) {
                addNode(completeEltSet.nodeAt(i));            
            }
            for (int i = 0 ; i<completeEltSet.edgeCount(); i++) {
                addEdge(completeEltSet.edgeAt(i));            
            }
        }
    }

    public Edge findEdge( Node from, Node to ) {
        Edge foundEdge=super.findEdge(from,to);
        if (foundEdge!=null && edges.contains(foundEdge)) return foundEdge;
        else return null;
    }

    public boolean deleteEdge( Edge e ) {
        if (e == null) return false;
        else {
            removeEdge(e);
            return completeEltSet.deleteEdge(e);
        }
    }

    public synchronized void deleteEdges( Vector edgesToDelete ) {
        removeEdges(edgesToDelete);
        completeEltSet.deleteEdges(edgesToDelete);
    }

    public boolean removeEdge( Edge e ) {
        if (e == null) return false;
            else {
                if(edges.removeElement(e)) {
                    return true;
                }
            return false;
        }
    }

    public synchronized void removeEdges( Vector edgesToRemove ) {
        for (int i=0;i<edgesToRemove.size();i++) {
            removeEdge((Edge) edgesToRemove.elementAt(i));
        }
    }

    public boolean deleteNode( Node node ) {
        if ( node == null ) return false;
        else {
            removeNode(node);
            return completeEltSet.deleteNode(node);
        }
    }

    public synchronized void deleteNodes( Vector nodesToDelete ) {
        removeNodes(nodesToDelete);
        completeEltSet.deleteNodes(nodesToDelete);
    }

    public boolean removeNode( Node node ) {
          if (node == null) return false;
          if (!nodes.removeElement(node)) return false;
		  
		  String id = node.getID();
          if ( id != null ) nodeIDRegistry.remove(id); // remove from registry
	
          for (int i = 0 ; i < node.edgeCount(); i++) {
             removeEdge(node.edgeAt(i));
         }

         return true;
    }

    public synchronized void removeNodes( Vector nodesToRemove ) {
        for (int i=0;i<nodesToRemove.size();i++) {
            removeNode((Node) nodesToRemove.elementAt(i));
        }
    }

    public synchronized void removeAll() {
        super.clearAll();        
    }

    public synchronized void clearAll() {
        removeAll();
        completeEltSet.clearAll();
    }

} // end com.touchgraph.graphlayout.graphelements.Locality
