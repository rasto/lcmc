/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
 * $Id: MatchGenerator.java,v 1.5 2004/02/16 22:26:45 minchau Exp $
 */

package org.apache.xalan.xsltc.compiler.util;

import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.Type;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public final class MatchGenerator extends MethodGenerator {
    private static int CURRENT_INDEX = 1;

    private int _iteratorIndex = INVALID_INDEX;

    private final Instruction _iloadCurrent;
    private final Instruction _istoreCurrent;
    private Instruction _aloadDom;
    
    public MatchGenerator(int access_flags, Type return_type, 
			  Type[] arg_types, String[] arg_names, 
			  String method_name, String class_name,
			  InstructionList il, ConstantPoolGen cp) {
	super(access_flags, return_type, arg_types, arg_names, method_name, 
	      class_name, il, cp);
	
	_iloadCurrent = new ILOAD(CURRENT_INDEX);
	_istoreCurrent = new ISTORE(CURRENT_INDEX);
    }

    public Instruction loadCurrentNode() {
	return _iloadCurrent;
    }

    public Instruction storeCurrentNode() {
	return _istoreCurrent;
    }
    
    public int getHandlerIndex() {
	return INVALID_INDEX;		// not available
    }

    /**
     * Get index of the register where the DOM is stored.
     */
    public Instruction loadDOM() {
	return _aloadDom;
    }

    /**
     * Set index where the reference to the DOM is stored.
     */
    public void setDomIndex(int domIndex) {
	_aloadDom = new ALOAD(domIndex);
    }

    /**
     * Get index of the register where the current iterator is stored.
     */
    public int getIteratorIndex() {
	return _iteratorIndex;
    }

    /**
     * Set index of the register where the current iterator is stored.
     */
    public void setIteratorIndex(int iteratorIndex) {
	_iteratorIndex = iteratorIndex;
    }

    public int getLocalIndex(String name) {
	if (name.equals("current")) {
	    return CURRENT_INDEX;
	}
	return super.getLocalIndex(name);
    }
}
