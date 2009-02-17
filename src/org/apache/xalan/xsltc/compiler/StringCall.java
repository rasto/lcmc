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
 * $Id: StringCall.java,v 1.7 2004/02/16 22:24:29 minchau Exp $
 */

package org.apache.xalan.xsltc.compiler;

import java.util.Vector;

import org.apache.bcel.generic.InstructionList;
import org.apache.xalan.xsltc.compiler.util.ClassGenerator;
import org.apache.xalan.xsltc.compiler.util.ErrorMsg;
import org.apache.xalan.xsltc.compiler.util.MethodGenerator;
import org.apache.xalan.xsltc.compiler.util.Type;
import org.apache.xalan.xsltc.compiler.util.TypeCheckError;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 */
final class StringCall extends FunctionCall {
    public StringCall(QName fname, Vector arguments) {
	super(fname, arguments);
    }

    public Type typeCheck(SymbolTable stable) throws TypeCheckError {
	final int argc = argumentCount();
	if (argc > 1) {
	    ErrorMsg err = new ErrorMsg(ErrorMsg.ILLEGAL_ARG_ERR, this);
	    throw new TypeCheckError(err);
	}

	if (argc > 0) {
	    argument().typeCheck(stable);
	}
	return _type = Type.String;
    }

    public void translate(ClassGenerator classGen, MethodGenerator methodGen) {
	final InstructionList il = methodGen.getInstructionList();
	Type targ;

	if (argumentCount() == 0) {
	    il.append(methodGen.loadContextNode());
	    targ = Type.Node;
	}
	else {
	    final Expression arg = argument();
	    arg.translate(classGen, methodGen);
	    arg.startIterator(classGen, methodGen);
	    targ = arg.getType();
	}

	if (!targ.identicalTo(Type.String)) {
	    targ.translateTo(classGen, methodGen, Type.String);
	}
    }
}
