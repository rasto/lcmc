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
 * $Id: AttributeSetMethodGenerator.java,v 1.7 2004/02/16 22:26:44 minchau Exp $
 */

package org.apache.xalan.xsltc.compiler.util;

import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
public final class AttributeSetMethodGenerator extends MethodGenerator {
    private static final int DOM_INDEX       = 1;
    private static final int ITERATOR_INDEX  = 2;
    private static final int HANDLER_INDEX   = 3;

    private static final org.apache.bcel.generic.Type[] argTypes =
   new org.apache.bcel.generic.Type[3];
    private static final String[] argNames = new String[3];
    
    static {
       argTypes[0] = Util.getJCRefType(DOM_INTF_SIG);
       argNames[0] = DOM_PNAME;
       argTypes[1] = Util.getJCRefType(NODE_ITERATOR_SIG);
       argNames[1] = ITERATOR_PNAME;
       argTypes[2] = Util.getJCRefType(TRANSLET_OUTPUT_SIG);
       argNames[2] = TRANSLET_OUTPUT_PNAME;
    }

    
    private final Instruction _aloadDom;
    private final Instruction _astoreDom;
    private final Instruction _astoreIterator;
    private final Instruction _aloadIterator;
    private final Instruction _astoreHandler;
    private final Instruction _aloadHandler;
    
    public AttributeSetMethodGenerator(String methodName, ClassGen classGen) {
	super(org.apache.bcel.Constants.ACC_PRIVATE,
	      org.apache.bcel.generic.Type.VOID,
	      argTypes, argNames, methodName, 
	      classGen.getClassName(),
	      new InstructionList(),
	      classGen.getConstantPool());
	
	_aloadDom       = new ALOAD(DOM_INDEX);
	_astoreDom      = new ASTORE(DOM_INDEX);
	_astoreIterator = new ASTORE(ITERATOR_INDEX);
	_aloadIterator  = new ALOAD(ITERATOR_INDEX);
	_astoreHandler  = new ASTORE(HANDLER_INDEX);
	_aloadHandler   = new ALOAD(HANDLER_INDEX);
    }

    public Instruction storeIterator() {
	return _astoreIterator;
    }
    
    public Instruction loadIterator() {
	return _aloadIterator;
    }

    public int getIteratorIndex() {
	return ITERATOR_INDEX;
    }

    public Instruction storeHandler() {
	return _astoreHandler;
    }

    public Instruction loadHandler() {
	return _aloadHandler;
    }

    public int getLocalIndex(String name) {
	return INVALID_INDEX;	// not available
    }
}
