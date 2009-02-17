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

import  java.io.Serializable;
//import java.util.Collection;
//import java.util.Iterator;


/** ImmutableGraphEltSet provides access to the elements of GraphElementSet
  * that does not allow for addition or deletion of nodes or edges.
  *
  * @author   Alexander Shapiro
  * @version  1.22-jre1.1  $Id: ImmutableGraphEltSet.java,v 1.2 2002/09/20 14:03:14 ldornbusch Exp $
  */
public interface ImmutableGraphEltSet {

    /** Return the number of Nodes in the cumulative Vector. */
    public int nodeCount();

    /** Return the current Node count.
      * @deprecated        this method has been replaced by the <tt>nodeCount()</tt> method.
      */
    public int nodeNum();

    /** Return an iterator over the Nodes in the cumulative Vector, null if it is empty. */
 //   public Iterator getNodes();

    /** Return the number of Edges in the cumulative Vector. */
    public int edgeCount();

    /** Return the current Edge count.
      * @deprecated        this method has been replaced by the <tt>edgeCount()</tt> method.
      */
    public int edgeNum();

    /** Return an iterator over the Edges in the cumulative Vector, null if it is empty. */
 //   public Iterator getEdges();

    /** Return the Node whose ID matches the String <tt>id</tt>, null if no match is found. */
    public Node findNode( String id );

    /** Return a Collection of all Nodes whose label matches the String <tt>label</tt>,
      * null if no match is found. */
 //   public Collection findNodesByLabel( String label );

   /** Return the first Nodes whose label contains the String <tt>substring</tt>,
     * null if no match is found. */
    public Node findNodeLabelContaining( String substring );

    /** Return an Edge spanning Node <tt>from</tt> to Node <tt>to</tt>. */
    public Edge findEdge( Node from, Node to );

    /** Returns a random node, or null if none exist (for making random graphs). */
    public Node getRandomNode();

    /** Return the first Node, null if none exist. */
    public Node getFirstNode();

    /** iterates through all the nodes. */
    public void forAllNodes( TGForEachNode fen );

    /** iterates through pairs of Nodes. */
    public void forAllNodePairs( TGForEachNodePair fenp );

    /** iterates through Edges. */
    public void forAllEdges( TGForEachEdge fee );

} // end com.touchgraph.graphlayout.graphelements.ImmutableGraphEltSet
