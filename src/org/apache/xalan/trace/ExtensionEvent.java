/*
 * Copyright 2002-2004 The Apache Software Foundation.
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
 * $Id: ExtensionEvent.java,v 1.2 2004/02/16 23:00:27 minchau Exp $
 */
 
package org.apache.xalan.trace;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.apache.xalan.transformer.TransformerImpl;

/**
 * An event representing an extension call.
 */
public class ExtensionEvent {

	public static final int DEFAULT_CONSTRUCTOR = 0;
	public static final int METHOD = 1;
	public static final int CONSTRUCTOR = 2;
	
	public final int m_callType; 
	public final TransformerImpl m_transformer;
	public final Object m_method;
	public final Object m_instance;
	public final Object[] m_arguments;
	
	
	public ExtensionEvent(TransformerImpl transformer, Method method, Object instance, Object[] arguments) {
		m_transformer = transformer;
		m_method = method;
		m_instance = instance;
		m_arguments = arguments;
		m_callType = METHOD;
	}		

	public ExtensionEvent(TransformerImpl transformer, Constructor constructor, Object[] arguments) {
		m_transformer = transformer;
		m_instance = null;		
		m_arguments = arguments;
		m_method = constructor;
		m_callType = CONSTRUCTOR;		
	}

	public ExtensionEvent(TransformerImpl transformer, Class clazz) {
		m_transformer = transformer;
		m_instance = null;
		m_arguments = null;
		m_method = clazz;
		m_callType = DEFAULT_CONSTRUCTOR;
	}

}

