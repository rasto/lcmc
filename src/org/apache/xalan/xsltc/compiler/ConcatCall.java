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
 * $Id: ConcatCall.java,v 1.7 2004/02/16 22:24:28 minchau Exp $
 */

package org.apache.xalan.xsltc.compiler;

import java.util.Vector;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.PUSH;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class ConcatCall extends FunctionCall {
    public ConcatCall(QName fname, Vector arguments) {
	super(fname, arguments);
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	for (int i = 0; i < argumentCount(); i++) {
	    final Expression exp = argument(i);
	    if (!exp.typeCheck(stable).identicalTo(Type.String)) {
		setArgument(i, new CastExpr(exp, Type.String));
	    }
	}
	return _type = Type.String;
    }
	
    /** translate leaves a String on the stack */
    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final ConstantPoolGen cpg = classGen.getConstantPool();
	final InstructionList il = methodGen.getInstructionList();
	final int nArgs = argumentCount();
	
	switch (nArgs) {
	case 0:
	    il.append(new PUSH(cpg, EMPTYSTRING));
	    break;
	    
	case 1:
	    argument().translate(classGen, methodGen);
	    break;

	default:
	    final int initBuffer = cpg.addMethodref(STRING_BUFFER_CLASS,
						    "<init>", "()V");
	    final Instruction append =
		new INVOKEVIRTUAL(cpg.addMethodref(STRING_BUFFER_CLASS,
						   "append",
						   "("+STRING_SIG+")"
						   +STRING_BUFFER_SIG));
	    
	    final int toString = cpg.addMethodref(STRING_BUFFER_CLASS,
						  "toString",
						  "()"+STRING_SIG);
	    
	    il.append(new NEW(cpg.addClass(STRING_BUFFER_CLASS)));
	    il.append(DUP);
	    il.append(new INVOKESPECIAL(initBuffer));
	    for (int i = 0; i < nArgs; i++) {
		argument(i).translate(classGen, methodGen);
		il.append(append);
	    }
	    il.append(new INVOKEVIRTUAL(toString));
	}
    }
}
