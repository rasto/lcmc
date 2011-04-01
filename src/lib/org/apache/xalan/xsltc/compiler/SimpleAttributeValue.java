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
 * $Id: SimpleAttributeValue.java,v 1.6 2004/02/16 22:24:29 minchau Exp $
 */

package org.apache.xalan.xsltc.compiler;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.PUSH;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class SimpleAttributeValue extends AttributeValue {

    private String _value; // The attributes value (literate string).

    /**
     * Creates a new simple attribute value.
     * @param value the attribute value.
     */
    public SimpleAttributeValue(String value) {
	_value = value;
    }

    /**
     * Returns this attribute value's type (String).
     * @param stable The compiler/parser's symbol table
     */
    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	return _type = Type.String;
    }
    
    public String toString() {
	return _value;
    }
	
    protected boolean contextDependent() {
	return false;
    }

    /**
     * Translate this attribute value into JVM bytecodes that pushes the
     * attribute value onto the JVM's stack.
     * @param classGen BCEL Java class generator
     * @param methodGen BCEL Java method generator
     */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	il.append(new PUSH(cpg, _value));
    }
}
