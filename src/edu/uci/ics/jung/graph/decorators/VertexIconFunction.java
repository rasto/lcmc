/*
 * Created on May 5, 2004
 *
 * Copyright (c) 2004, the JUNG Project and the Regents of the University 
 * of California
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * http://jung.sourceforge.net/license.txt for a description.
 */
package edu.uci.ics.jung.graph.decorators;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.text.html.HTMLEditorKit;

import edu.uci.ics.jung.graph.ArchetypeVertex;


/*
 * An interface for classes that provide a way to fetch a label
 * for a specified vertex.
 * 
 * @author Tom Nelson - RABA Technologies
 */
public interface VertexIconFunction {
    
    Icon BROKEN_IMAGE =
        new ImageIcon(HTMLEditorKit.class.getResource("icons/image-failed.gif"));
    Icon getIcon(ArchetypeVertex v);
}
