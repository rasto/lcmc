/*
* Copyright (c) 2003, the JUNG Project and the Regents of the University 
* of California
* All rights reserved.
*
* This software is open-source under the BSD license; see either
* "license.txt" or
* http://jung.sourceforge.net/license.txt for a description.
*/
package edu.uci.ics.jung.io;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.impl.DirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.SparseGraph;
import edu.uci.ics.jung.graph.impl.SparseVertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.utils.UserData;

/**
 * The default GraphML file handler to use to parse the xml file
 * @author Scott White
 */
public class GraphMLFileHandler extends DefaultHandler {
    private Graph mGraph;
    private StringLabeller mLabeller;
    private boolean default_directed;

    /**
     * The default constructor
     */
    public GraphMLFileHandler() {
    }

    protected Graph getGraph() {
        return mGraph;
    }

    protected StringLabeller getLabeller() {
        return mLabeller;
    }

    private Map getAttributeMap(Attributes attrs) {
        Map map = new HashMap();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                map.put(attrs.getQName(i), attrs.getValue(i));
            }
        }
        return map;
    }

    protected Edge createEdge(Map attributeMap) {
        if (mGraph == null) {
            throw new FatalException("Error parsing graph. Graph element must be specified before edge element.");
        }

        String sourceId = (String) attributeMap.remove("source");
        Vertex sourceVertex =
                mLabeller.getVertex(sourceId);

        String targetId = (String) attributeMap.remove("target");
        Vertex targetVertex =
                 mLabeller.getVertex(targetId);

        String direction = (String) attributeMap.remove("directed");
        boolean directed;
        if (direction == null)
        {
            // use default_directed
            directed = default_directed;
        }
        else
        {
            // use specified direction
            if (direction.equals("true"))
                directed = true;
            else if (direction.equals("false"))
                directed = false;
            else
                throw new FatalException("Error parsing graph: 'directed' tag has invalid value: " + direction);
        }
//        Edge e = GraphUtils.addEdge(mGraph, sourceVertex, targetVertex);
        Edge e;
        if (directed)
            e = mGraph.addEdge(new DirectedSparseEdge(sourceVertex, targetVertex));
        else
            e = mGraph.addEdge(new UndirectedSparseEdge(sourceVertex, targetVertex));

        for (Iterator keyIt = attributeMap.keySet().iterator();
             keyIt.hasNext();
                ) {
            Object key = keyIt.next();
            Object value = attributeMap.get(key);
            e.setUserDatum(key, value, UserData.SHARED);
        }

        return e;
    }

    protected void createGraph(Map attributeMap) {
        String edgeDefaultType =
                (String) attributeMap.remove("edgedefault");
        mGraph = new SparseGraph();
        if (edgeDefaultType.equals("directed"))
        {
            default_directed = true;
//          mGraph = new DirectedSparseGraph();
        } 
        else if (edgeDefaultType.equals("undirected")) 
        {
            default_directed = false;
//            mGraph = new UndirectedSparseGraph();
        } 
        else {
            throw new FatalException("Error parsing graph. Edge default type not specified.");
        }

        mLabeller = StringLabeller.getLabeller(mGraph);

        for (Iterator keyIt = attributeMap.keySet().iterator(); keyIt.hasNext();) {
            Object key = keyIt.next();
            Object value = attributeMap.get(key);
            mGraph.setUserDatum(key, value, UserData.SHARED);
        }

    }

    protected ArchetypeVertex createVertex(Map attributeMap) {
        if (mGraph == null) {
            throw new FatalException("Error parsing graph. Graph element must be specified before node element.");
        }

        ArchetypeVertex vertex = mGraph.addVertex(new SparseVertex());
        String idString = (String) attributeMap.remove("id");

        try {
            mLabeller.setLabel((Vertex) vertex,idString);
        } catch (StringLabeller.UniqueLabelException ule) {
            throw new FatalException("Ids must be unique");

        }

        for (Iterator keyIt = attributeMap.keySet().iterator();
             keyIt.hasNext();
                ) {
            Object key = keyIt.next();
            Object value = attributeMap.get(key);
            vertex.setUserDatum(key, value, UserData.SHARED);
        }
        return vertex;
    }

    public void startElement(
            String namespaceURI,
            String lName,
            // local name
            String qName, // qualified name
            Attributes attrs) throws SAXException {

        Map attributeMap = getAttributeMap(attrs);

        if (qName.toLowerCase().equals("graph")) {
            createGraph(attributeMap);

        } else if (qName.toLowerCase().equals("node")) {
            createVertex(attributeMap);

        } else if (qName.toLowerCase().equals("edge")) {
            createEdge(attributeMap);

        }
    }

}
