/*
 * Copyright 1999-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: TraceListener.java,v 1.8 2004/02/16 23:00:27 minchau Exp $
 */
package org.apache.xalan.trace;


/**
 * Interface the XSL processor calls when it matches a source node, selects a set of source nodes,
 * or generates a result node.
 * If you want an object instance to be called when a trace event occurs, use the TransformerImpl setTraceListener method.
 * @see org.apache.xalan.trace.TracerEvent
 * @see org.apache.xalan.trace.TraceManager#addTraceListener
 * @xsl.usage advanced
 */
public interface TraceListener extends java.util.EventListener
{

  /**
   * Method that is called when a trace event occurs.
   * The method is blocking.  It must return before processing continues.
   *
   * @param ev the trace event.
   */
  public void trace(TracerEvent ev);

  /**
   * Method that is called just after the formatter listener is called.
   *
   * @param ev the generate event.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public void selected(SelectionEvent ev) throws javax.xml.transform.TransformerException;

  /**
   * Method that is called just after the formatter listener is called.
   *
   * @param ev the generate event.
   */
  public void generated(GenerateEvent ev);
}
