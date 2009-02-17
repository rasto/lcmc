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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import edu.uci.ics.jung.exceptions.FatalException;
import edu.uci.ics.jung.graph.DirectedEdge;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedEdge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.Indexer;
import edu.uci.ics.jung.utils.Pair;
import edu.uci.ics.jung.utils.PredicateUtils;
import edu.uci.ics.jung.utils.UserData;
import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * A file reader for GraphML files. Currently, there is only support for directed and undirected graphs.
 * The elements <port>, <hyperedge>, <endpoint>, and <locator> are simply ignored. <p>
 *
 * What follows are the native GraphML attributes that are recognized: <ul>
 * <li> graph: edgedefault (takes values {"undirected","directed"}; determines whether the graph is directed or undirected)
 * <li> node: id (Can be any string value; only used to connect up edges)
 * <li> edge: source, target (take ids; both are used to created either a directed or undirected edge depending
 * on the type of graph) </ul> <br>
 * These attributes are not stored as explicit UserDatum instances. All other attributes are  read in and stored as
 * UserDatum String values with the corresponding graph object, i.e. graph, node, or edge. <p>
 *
 * A sample file looks like this: <br>
 * &lt;?xml version="1.0" encoding="iso-8859-1" ?&gt; <br>
 * &lt;?meta name="GENERATOR" content="XML::Smart 1.3.1" ?&gt; <br>
 * &lt;?meta name="GENERATOR" content="XML::Smart 1.3.1" ?&gt; <br>
 * &lt;graph edgedefault="directed" year="1983"&gt; <br>
 * &lt;node id="1" name="V1" color="red"/&gt; <br>
 * &lt;node id="2" name="V2" color="blue"/&gt;  <br>
 * &lt;node id="3" name="V3" color="green"/&gt;  <br>
 * &lt;edge source="1" target="2" day="Monday"/&gt; <br>
 * &lt;edge source="1" target="3" day="Tuesday"/&gt; <br>
 *&lt;edge source="2" target="3" day="Friday"/&gt;  <br>
 * &lt;/graph&gt; <br>
 * Note: In this example, year, color, and day are user-defined attributes that get stored in the object's UserData
 *
 * Assuming we have a Graph g created from the above XML file we can print out the days of
 * the week for each node as follows:
 * <pre>
 * for (Iterator eIt = g.getEdges().iterator(); eIt.hasNext(); ) {
 *   Edge v = (Edge) eIt.next();
 *   System.out.println(e.getUserDatum("day");
 * }
 * </pre><br>
 *
 * @see "http://graphml.graphdrawing.org/"
 * @author Scott White, John Yesberg
 */
public class GraphMLFile implements GraphFile {
    private GraphMLFileHandler mFileHandler;
    protected boolean directed;
    protected boolean undirected;

    /**
     * Default constructor which uses default GraphMLFileHandler to parse the graph
     */
    public GraphMLFile() {
        mFileHandler = new GraphMLFileHandler();
    }

    /**
     * Constructors which allows a subclass of GraphMLFileHandler to be used to parse the graph
     * @param handler the user-provided GraphML file handler
     */
    public GraphMLFile(GraphMLFileHandler handler) {
        mFileHandler = handler;
    }

    /**
     * Loads a graph from a GraphML file.
     * @param filename the fully specified file name
     * @return the constructed graph
     */
    public Graph load(String filename) {

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            // Parse the input
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(new File(filename), mFileHandler);

        } catch (Exception e) {
            throw new FatalException("Error loading graphml file: " + filename, e);
        }

        return mFileHandler.getGraph();
    }

    /**
     * Loads a graph from a GraphML input stream.
     * @param stream the input stream which contains the GraphML data
     * @return the constructed graph
     * @deprecated generally, InputStreams are less robust than Readers
     */
    public Graph load(InputStream stream) {

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            // Parse the input
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(stream, mFileHandler);

        } catch (Exception e) {
            throw new FatalException("Error loading graphml file", e);
        }

        return mFileHandler.getGraph();
    }
    
    public Graph load( Reader reader ) {

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            // Parse the input
            SAXParser saxParser = factory.newSAXParser();
            InputSource is = new InputSource( reader );
            saxParser.parse(is, mFileHandler);

        } catch (Exception e) {
            throw new FatalException("Error loading graphml file", e);
        }

        return mFileHandler.getGraph();

    }

    /**
     * Loads in a list of graphs whose corresponding filenames pass the file filter and are located in the
     * specified directory
     * @param dirName the directory containing the set of files that are to be screened through the file filter
     * @param filter the file filter
     * @return a list of graphs
     */
    public List loadGraphCollection(String dirName, FilenameFilter filter) {
        File dir = new File(dirName);
        if (!dir.isDirectory()) {
            throw new FatalException("Parameter dirName must be a directory");
        }

        String[] files = dir.list(filter);

        List graphCollection = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            String currentFile = dirName + File.separatorChar + files[i];
            GraphMLFile graphmlFile = new GraphMLFile(mFileHandler);
            Graph graph = graphmlFile.load(currentFile);
            //System.out.println("Graph loaded with " + graph.numVertices() + " nodes and " + graph.numEdges() + " edges.");
            graphCollection.add(graph);
        }

        return graphCollection;
    }

    public void save(Graph g, String filename) {
        PrintStream out;
        try {
            out = new PrintStream(new FileOutputStream(filename, false));
        } catch (Exception e) {
            throw new FatalException("Could not open file \"" + filename + "\" for writing. " + e);
        }
        save(g, out);
        out.close();
    }
    


    public void save(Graph g, PrintStream out) {		
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns/graphml\"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  ");
		out.println("xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns/graphml\">");
        out.print("<graph edgedefault=\"");
        boolean directed = PredicateUtils.enforcesEdgeConstraint(g, Graph.DIRECTED_EDGE);
        boolean undirected = PredicateUtils.enforcesEdgeConstraint(g, Graph.UNDIRECTED_EDGE);
        if (directed)
            out.print("directed\" ");
        else if (undirected)
            out.print("undirected\" ");
        else // default for mixed graphs
        {
            directed = true;
            out.print("directed\" ");
        }
//            throw new IllegalArgumentException("Mixed (directed/undirected) " + 
//                "graphs not currently supported");

        saveUserData(g, out);
		out.println(" >");
        saveVerticesSection(out, g);
        saveEdgesSection(out, g);
        out.println("</graph>");
        out.println("</graphml>");

    }

    private void saveVerticesSection(PrintStream out, Graph g) {
        int numVertices = g.getVertices().size();
        Indexer id = Indexer.getIndexer(g);
        for (int i = 0; i < numVertices; i++) {
            Vertex v = (Vertex) id.getVertex(i);
            int vId = i+1;
            out.print("<node id=\"" + vId + "\" ");

            saveUserData(v, out);
			out.println("/>");
        }
    }

    private void saveEdgesSection(PrintStream out, Graph g) {
        Indexer id = Indexer.getIndexer(g);
        for (Iterator edgeIterator = g.getEdges().iterator(); edgeIterator.hasNext();)  {
            Edge e = (Edge) edgeIterator.next();
            Pair p = e.getEndpoints();
            Vertex src = (Vertex) p.getFirst();
            Vertex dest = (Vertex) p.getSecond();
            int srcId = id.getIndex(src)+1;
            out.print("<edge source=\"" + srcId + "\" ");
            int destId = id.getIndex(dest)+1;
            out.print("target=\"" + destId + "\" ");

            // tag the edges that don't match the default
            if (directed)
            {
                if (e instanceof UndirectedEdge)
                    out.print("directed=\"false\" ");
            }
            else // undirected
                if (e instanceof DirectedEdge)
                    out.print("directed=\"true\" ");
            
            saveUserData(e, out);
			out.println("/>");
        }
    }

    private void saveUserData(UserDataContainer udc, PrintStream out) {
        Iterator udki = udc.getUserDatumKeyIterator();
        while (udki.hasNext()) {
            Object key_obj = udki.next();
            if (udc.getUserDatumCopyAction(key_obj) == UserData.REMOVE)
                continue;
            String key = key_obj.toString();
            if (invalidXMLData(key)) continue;
            Object o = udc.getUserDatum(key);
            if (o == null)
                continue;
            String datum = o.toString();
			if (invalidXMLData(datum)) continue;
            out.print(key + "=\"" + datum + "\" ");
        }
    }
    
    private boolean invalidXMLData(String str) {
    	if (str.indexOf("&") >= 0) return true;
		if (str.indexOf("<") >= 0) return true;
		if (str.indexOf(">") >= 0) return true;
		if (str.indexOf("\'") >= 0) return true;
		if (str.indexOf("\"") >= 0) return true;
		return false;
    }

    /**
     * Allows the user to provide his/her own subclassed GraphML file handerl
     * @param fileHandler
     */
    public void setGraphMLFileHandler(GraphMLFileHandler fileHandler) {
        mFileHandler = fileHandler;
    }
}
