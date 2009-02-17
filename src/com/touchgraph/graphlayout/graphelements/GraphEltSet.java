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

//import  java.util.Collection;
//import  java.util.Iterator;
import  java.util.Vector;
import  java.util.Hashtable;
import java.util.Enumeration;
//import java.util.Iterator;
//import java.util.Collection;

/**  GraphEltSet contains data about the graph's components. Currently
  *  the only components are edges and nodes.
  *
  * @author   Alexander Shapiro
  * @author   Murray Altheim (added support for IDs)
  * @version  1.22-jre1.1  $Id: GraphEltSet.java,v 1.2 2002/09/20 14:03:29 ldornbusch Exp $
  */
public class GraphEltSet implements ImmutableGraphEltSet {

    protected Vector nodes;
    protected Vector edges;

    /** The Hashtable containing references to the Node IDs of the current graph.
      */
    protected Hashtable nodeIDRegistry = null;

  // ...........

    /** Default constructor. */
    public GraphEltSet() {
        nodes = new Vector();
        edges = new Vector();
        nodeIDRegistry = new Hashtable(); // registry of Node IDs
    }

  // Node manipulation ...........................

    /** Return the Node at int <tt>index</tt>, null if none are available. */
    protected Node nodeAt( int i ) {
        if ( nodes.size() == 0 ) return null;
        return (Node)nodes.elementAt(i);
    }

    /** Return the number of Nodes in the cumulative Vector.
      * @deprecated        this method has been replaced by the <tt>nodeCount()</tt> method.
      */
    public int nodeNum() {
        return nodes.size();
    }

    /** Return the number of Nodes in the cumulative Vector. */
    public int nodeCount() {
        return nodes.size();
    }

    /** Return an iterator over the Nodes in the cumulative Vector, null if it is empty. */
/*    public Iterator getNodes() {
        if ( nodes.size() == 0 ) return null;
        return nodes.iterator();
    }*/

    /** Registers the Node <tt>node</tt> via its ID String <tt>id</tt>.
      *
      * @param id the ID of the object.
      * @param node the Node to be registered.
      */
    //protected void registerNode( String id, Node node ) {
  // FIXME
    //}

    /** Add the Node <tt>node</tt> to the graph, and
      * registers the Node via its ID. If no ID exists, no registration occurs. */
    public synchronized void addNode( Node node ) throws TGException {
        String id = node.getID();
        if ( id != null ) {
            if ( findNode(id) == null ) { // doesn't already exist
                nodeIDRegistry.put(id,node);
                nodes.addElement(node);
            } else throw new TGException(TGException.NODE_EXISTS,"node ID '"+id+"' already exists.");
        } else {
            String label = node.getLabel().trim();
            if (label == null) label = "";
            if (!label.equals("") && findNode(node.getLabel()) == null ) {
                id = label;
            } else {
                int i;
                for( i = 1; findNode( label +"-"+ i ) != null; i++ );
                id = label + "-" + i;
            }
            node.setID(id);
            nodeIDRegistry.put(id,node);
            nodes.addElement(node);
        }
        //} else throw new TGException(TGException.NODE_NO_ID,"node has no ID."); // could be ignored?
    }

    /** Returns true if the graph contains the Node <tt>node</tt>. */
    public boolean contains( Node node ) {
        return nodes.contains(node);
    }

  // Edge manipulation ...........................

    /** Return the Edge at int <tt>index</tt>, null if none are available. */
    protected Edge edgeAt( int index ) {
        if ( edges.size() == 0 ) return null;
        return (Edge)edges.elementAt(index);
    }

    /** Return the number of Edges in the cumulative Vector.
      * @deprecated        this method has been replaced by the <tt>edgeCount()</tt> method.
      */
    public int edgeNum() {
        return edges.size();
    }

    /** Return the number of Edges in the cumulative Vector. */
    public int edgeCount() {
        return edges.size();
    }

    /** Return an iterator over the Edges in the cumulative Vector, null if it is empty. */
/*
    public Iterator getEdges() {
        if ( edges.size() == 0 ) return null;
        else return edges.iterator();
    }
*/

    /** Add the Edge <tt>edge</tt> to the graph. */
    public void addEdge( Edge edge ) {
        if ( edge == null ) return;
        if(!contains(edge)) {
            edges.addElement(edge);
            edge.from.addEdge(edge);
            edge.to.addEdge(edge);
        }
    }

    /** Add an Edge from Node <tt>from</tt> to Node <tt>to</tt>,
      * with tension of int <tt>tension</tt>, returning the Edge.
      */
    public Edge addEdge( Node from, Node to, int tension ) {
        Edge edge = null;
        if ( from != null && to != null ) {
             edge = new Edge(from,to,tension);
             addEdge(edge);
         }
         return edge;
    }

    /** Returns true if the graph contains the Edge <tt>edge</tt>. */
    public boolean contains( Edge edge ) {
        return edges.contains(edge);
    }

    /** Return the Node whose ID matches the String <tt>id</tt>, null if no match is found. */
    public Node findNode( String id ) {
        if ( id == null ) return null; // ignore
        return (Node)nodeIDRegistry.get(id);
    }

  /** Return the Node whose URL matches the String <tt>strURL</tt>, null if no match is found. */
    public Node findNodeByURL( String strURL ) {
      Node  retVal=null;
      if ( strURL == null )
        return null; // ignore

      Enumeration myEnum=nodeIDRegistry.elements();
      while (myEnum.hasMoreElements()) {
        Node node = (Node) myEnum.nextElement();
        if (node.getURL().equalsIgnoreCase(strURL)){
          retVal=node;
          break;
        }
      }
      return retVal;
    }

   /** Return a Collection of all Nodes whose label matches the String <tt>label</tt>,
     * null if no match is found. */
/*    public Collection findNodesByLabel( String label ) {
        Vector nodelist = new Vector();
        for ( int i = 0 ; i < nodeCount() ; i++) {
            if (nodeAt(i)!=null && nodeAt(i).getLabel().equals(label)) {
                nodelist.add(nodeAt(i));
            }
        }
        if ( nodelist.size() == 0 ) return null;
        else return (Collection)nodelist;
    }*/

   /** Return the first Nodes whose label contains the String <tt>substring</tt>,
     * null if no match is found. */
    public Node findNodeLabelContaining( String substring ) {
         for ( int i = 0 ; i < nodeCount() ; i++) {
            if (nodeAt(i)!=null && nodeAt(i).getLabel().toLowerCase().equals(substring.toLowerCase())) {
                return nodeAt(i);
            }
        }

        for ( int i = 0 ; i < nodeCount() ; i++) {
            if (nodeAt(i)!=null && nodeAt(i).getLabel().toLowerCase().indexOf(
                                        substring.toLowerCase())>-1) {
                return nodeAt(i);
            }
        }
        return null;
    }

    /** Return an Edge spanning Node <tt>from</tt> to Node <tt>to</tt>. */
    public Edge findEdge( Node from, Node to ) {
        for ( int i = 0 ; i < from.edgeCount(); i++ ) {
            Edge e = from.edgeAt(i);
            if (e.to == to) return e;
        }
        return null;
    }

    /** Delete the Edge <tt>edge</tt>. */
    public boolean deleteEdge( Edge edge ) {
        synchronized(edges) {
            if ( edge == null ) return false;
            if (!edges.removeElement(edge)) return false;
            edge.from.removeEdge(edge);
            edge.to.removeEdge(edge);
            return true;
        }
    }

    /** Delete the Edges contained within the Vector <tt>edgedToDelete</tt>. */
    public void deleteEdges( Vector edgesToDelete ) {
        synchronized(edges) {
            for (int i=0;i<edgesToDelete.size();i++) {
                deleteEdge((Edge) edgesToDelete.elementAt(i));
            }
        }
    }

   /** Delete the Edge spanning Node <tt>from</tt> to Node <tt>to</tt>,
     * returning true if successful. */
    public boolean deleteEdge( Node from, Node to ) {
        synchronized(edges) {
            Edge e = findEdge(from,to);
            if (e!=null) return deleteEdge(e);
            return false;
        }
    }

    /** Delete the Node <tt>node</tt>, returning true if successful. */
    public boolean deleteNode( Node node ) {
        synchronized (nodes) {
            if ( node == null ) return false;
            if ( !nodes.removeElement(node) ) return false;

            String id = node.getID();
            if ( id != null ) nodeIDRegistry.remove(id); // remove from registry

            for ( int i = 0 ; i < node.edgeCount(); i++ ) {
                Edge e = node.edgeAt(i);
                if ( e.from == node ) {
                    edges.removeElement(e); // Delete edge not used, because it would change the node's edges
                    e.to.removeEdge(e);     // vector which is being iterated on.
                } else if ( e.to == node ) {
                    edges.removeElement(e);
                    e.from.removeEdge(e);
                }
                //No edges are deleted from node.  Hopefully garbage collection will pick them up.
            }
         }
         return true;
    }

    /** Delete the Nodes contained within the Vector <tt>nodesToDelete</tt>. */
    public void deleteNodes( Vector nodesToDelete ) {
        synchronized (nodes) {
            for (int i=0;i<nodesToDelete.size();i++) {
                deleteNode((Node)nodesToDelete.elementAt(i));
            }
        }
    }

    /** Returns a random node, or null if none exist (for making random graphs). */
    public Node getRandomNode() {
        if ( nodes.size() == 0 ) return null;
        int r=(int) (Math.random()*nodeCount());
        return nodeAt(r);
    }

    /** Return the first Node, null if none exist. */
    public Node getFirstNode() {
        if ( nodes.size() == 0 ) return null;
        else return nodeAt(0);
    }

    /** Clear all nodes and edges. */
    public void clearAll() {
        synchronized(nodes) {
            synchronized(edges) {
                nodes.removeAllElements();
                edges.removeAllElements();
                nodeIDRegistry.clear();
            }
        }
    }

   /** A way of iterating through all the nodes.
     * Maybe too complex, and should be replaced by iterators.
     */
    public void forAllNodes( TGForEachNode fen ) {
        synchronized(nodes) {
            for (int i=0;i<nodeCount();i++) {
                Node n = nodeAt(i);
                fen.forEachNode(n);
            }
        }
    }

    /** iterates through pairs of Nodes. */
    public void forAllNodePairs( TGForEachNodePair fenp ) {
        synchronized(nodes) {
            for (int i=0;i<nodeCount();i++) {
                Node n1=nodeAt(i);
                fenp.beforeInnerLoop(n1);
                for (int j=i+1;j<nodeCount();j++)
                    fenp.forEachNodePair(n1, nodeAt(j));
                fenp.afterInnerLoop(n1);
            }
        }
    }

    /** Iterates through Edges. */
    public void forAllEdges( TGForEachEdge fee ) {
        synchronized(edges) {
            for (int i=0;i<edgeCount();i++) {
                Edge e = edgeAt(i);
                fee.forEachEdge(e);
            }
        }
    }

} // end com.touchgraph.graphlayout.graphelements.GraphEltSet
