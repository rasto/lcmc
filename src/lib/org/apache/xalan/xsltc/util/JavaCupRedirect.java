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
 * $Id: JavaCupRedirect.java,v 1.4 2004/12/15 17:35:47 jycli Exp $
 */

package org.apache.xalan.xsltc.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Utility class to redirect input to JavaCup program.
 *
 * Usage-command line: 
 * <code>java org.apache.xalan.xsltc.utils.JavaCupRedirect [args] -stdin filename.ext</code>
 *
 * @author Morten Jorgensen
 * @version $Id: JavaCupRedirect.java,v 1.4 2004/12/15 17:35:47 jycli Exp $
 */
public class JavaCupRedirect {

    private final static String ERRMSG = 
		 "You must supply a filename with the -stdin option.";

    public static void main (String args[]) {

		 // If we should call System.exit or not
         //@todo make this settable for use inside other java progs
		 boolean systemExitOK = true;

		 // This is the stream we'll set as our System.in
		 InputStream input = null;

		 // The number of arguments
		 final int argc = args.length;

		 // The arguments we'll pass to the real 'main()'
		 String[] new_args = new String[argc - 2];
		 int new_argc = 0;

		 // Parse all parameters passed to this class
		 for (int i = 0; i < argc; i++) {
		     // Parse option '-stdin <filename>'
		     if (args[i].equals("-stdin")) {
		 		 // This option must have an argument
		 		 if ((++i >= argc) || (args[i].startsWith("-"))) {
		 		     System.err.println(ERRMSG);
                     throw new RuntimeException(ERRMSG);
		 		 }
		 		 try {
		 		     input = new FileInputStream(args[i]);
		 		 }
		 		 catch (FileNotFoundException e) {
		 		     System.err.println("Could not open file "+args[i]);
                     throw new RuntimeException(e.getMessage());
		 		 }
		 		 catch (SecurityException e) {
		 		     System.err.println("No permission to file "+args[i]);
                     throw new RuntimeException(e.getMessage());
		 		 }
		     }
		     else {
		 		 if (new_argc == new_args.length) {
		 		     System.err.println("Missing -stdin option!");
                     throw new RuntimeException();
		 		 }
		 		 new_args[new_argc++] = args[i];
		     }
		 }

		 System.setIn(input);
		 try {
		     java_cup.Main.main(new_args);
		 }
		 catch (Exception e) {
		     System.err.println("Error running JavaCUP:");
		     e.printStackTrace();
		 }
    }
}
