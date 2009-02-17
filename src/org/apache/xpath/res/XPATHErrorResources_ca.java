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
 * $Id: XPATHErrorResources_ca.java,v 1.5 2005/01/23 01:39:54 mcnamara Exp $
 */
package org.apache.xpath.res;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Set up error messages.
 * We build a two dimensional array of message keys and
 * message strings. In order to add a new message here,
 * you need to first add a Static string constant for the
 * Key and update the contents array with Key, Value pair
  * Also you need to  update the count of messages(MAX_CODE)or
 * the count of warnings(MAX_WARNING) [ Information purpose only]
 * @xsl.usage advanced
 */
public class XPATHErrorResources_ca extends ListResourceBundle
{

/*
 * General notes to translators:
 *
 * This file contains error and warning messages related to XPath Error
 * Handling.
 *
 *  1) Xalan (or more properly, Xalan-interpretive) and XSLTC are names of
 *     components.
 *     XSLT is an acronym for "XML Stylesheet Language: Transformations".
 *     XSLTC is an acronym for XSLT Compiler.
 *
 *  2) A stylesheet is a description of how to transform an input XML document
 *     into a resultant XML document (or HTML document or text).  The
 *     stylesheet itself is described in the form of an XML document.
 *
 *  3) A template is a component of a stylesheet that is used to match a
 *     particular portion of an input document and specifies the form of the
 *     corresponding portion of the output document.
 *
 *  4) An element is a mark-up tag in an XML document; an attribute is a
 *     modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *     "elem" is an element name, "attr" and "attr2" are attribute names with
 *     the values "val" and "val2", respectively.
 *
 *  5) A namespace declaration is a special attribute that is used to associate
 *     a prefix with a URI (the namespace).  The meanings of element names and
 *     attribute names that use that prefix are defined with respect to that
 *     namespace.
 *
 *  6) "Translet" is an invented term that describes the class file that
 *     results from compiling an XML stylesheet into a Java class.
 *
 *  7) XPath is a specification that describes a notation for identifying
 *     nodes in a tree-structured representation of an XML document.  An
 *     instance of that notation is referred to as an XPath expression.
 *
 *  8) The context node is the node in the document with respect to which an
 *     XPath expression is being evaluated.
 *
 *  9) An iterator is an object that traverses nodes in the tree, one at a time.
 *
 *  10) NCName is an XML term used to describe a name that does not contain a
 *     colon (a "no-colon name").
 *
 *  11) QName is an XML term meaning "qualified name".
 */

  /** Field MAX_CODE          */
  public static final int MAX_CODE = 108;  // this is needed to keep track of the number of messages

  /** Field MAX_WARNING          */
  public static final int MAX_WARNING = 11;  // this is needed to keep track of the number of warnings

  /** Field MAX_OTHERS          */
  public static final int MAX_OTHERS = 20;

  /** Field MAX_MESSAGES          */
  public static final int MAX_MESSAGES = MAX_CODE + MAX_WARNING + 1;


  /*
   * static variables
   */
  public static final String ERROR0000 = "ERROR0000";
  public static final String ER_CURRENT_NOT_ALLOWED_IN_MATCH =
         "ER_CURRENT_NOT_ALLOWED_IN_MATCH";
  public static final String ER_CURRENT_TAKES_NO_ARGS =
         "ER_CURRENT_TAKES_NO_ARGS";
  public static final String ER_DOCUMENT_REPLACED = "ER_DOCUMENT_REPLACED";
  public static final String ER_CONTEXT_HAS_NO_OWNERDOC =
         "ER_CONTEXT_HAS_NO_OWNERDOC";
  public static final String ER_LOCALNAME_HAS_TOO_MANY_ARGS =
         "ER_LOCALNAME_HAS_TOO_MANY_ARGS";
  public static final String ER_NAMESPACEURI_HAS_TOO_MANY_ARGS =
         "ER_NAMESPACEURI_HAS_TOO_MANY_ARGS";
  public static final String ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS =
         "ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS";
  public static final String ER_NUMBER_HAS_TOO_MANY_ARGS =
         "ER_NUMBER_HAS_TOO_MANY_ARGS";
  public static final String ER_NAME_HAS_TOO_MANY_ARGS =
         "ER_NAME_HAS_TOO_MANY_ARGS";
  public static final String ER_STRING_HAS_TOO_MANY_ARGS =
         "ER_STRING_HAS_TOO_MANY_ARGS";
  public static final String ER_STRINGLENGTH_HAS_TOO_MANY_ARGS =
         "ER_STRINGLENGTH_HAS_TOO_MANY_ARGS";
  public static final String ER_TRANSLATE_TAKES_3_ARGS =
         "ER_TRANSLATE_TAKES_3_ARGS";
  public static final String ER_UNPARSEDENTITYURI_TAKES_1_ARG =
         "ER_UNPARSEDENTITYURI_TAKES_1_ARG";
  public static final String ER_NAMESPACEAXIS_NOT_IMPLEMENTED =
         "ER_NAMESPACEAXIS_NOT_IMPLEMENTED";
  public static final String ER_UNKNOWN_AXIS = "ER_UNKNOWN_AXIS";
  public static final String ER_UNKNOWN_MATCH_OPERATION =
         "ER_UNKNOWN_MATCH_OPERATION";
  public static final String ER_INCORRECT_ARG_LENGTH ="ER_INCORRECT_ARG_LENGTH";
  public static final String ER_CANT_CONVERT_TO_NUMBER =
         "ER_CANT_CONVERT_TO_NUMBER";
  public static final String ER_CANT_CONVERT_TO_NODELIST =
         "ER_CANT_CONVERT_TO_NODELIST";
  public static final String ER_CANT_CONVERT_TO_MUTABLENODELIST =
         "ER_CANT_CONVERT_TO_MUTABLENODELIST";
  public static final String ER_CANT_CONVERT_TO_TYPE ="ER_CANT_CONVERT_TO_TYPE";
  public static final String ER_EXPECTED_MATCH_PATTERN =
         "ER_EXPECTED_MATCH_PATTERN";
  public static final String ER_COULDNOT_GET_VAR_NAMED =
         "ER_COULDNOT_GET_VAR_NAMED";
  public static final String ER_UNKNOWN_OPCODE = "ER_UNKNOWN_OPCODE";
  public static final String ER_EXTRA_ILLEGAL_TOKENS ="ER_EXTRA_ILLEGAL_TOKENS";
  public static final String ER_EXPECTED_DOUBLE_QUOTE =
         "ER_EXPECTED_DOUBLE_QUOTE";
  public static final String ER_EXPECTED_SINGLE_QUOTE =
         "ER_EXPECTED_SINGLE_QUOTE";
  public static final String ER_EMPTY_EXPRESSION = "ER_EMPTY_EXPRESSION";
  public static final String ER_EXPECTED_BUT_FOUND = "ER_EXPECTED_BUT_FOUND";
  public static final String ER_INCORRECT_PROGRAMMER_ASSERTION =
         "ER_INCORRECT_PROGRAMMER_ASSERTION";
  public static final String ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL =
         "ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL";
  public static final String ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG =
         "ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG";
  public static final String ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG =
         "ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG";
  public static final String ER_PREDICATE_ILLEGAL_SYNTAX =
         "ER_PREDICATE_ILLEGAL_SYNTAX";
  public static final String ER_ILLEGAL_AXIS_NAME = "ER_ILLEGAL_AXIS_NAME";
  public static final String ER_UNKNOWN_NODETYPE = "ER_UNKNOWN_NODETYPE";
  public static final String ER_PATTERN_LITERAL_NEEDS_BE_QUOTED =
         "ER_PATTERN_LITERAL_NEEDS_BE_QUOTED";
  public static final String ER_COULDNOT_BE_FORMATTED_TO_NUMBER =
         "ER_COULDNOT_BE_FORMATTED_TO_NUMBER";
  public static final String ER_COULDNOT_CREATE_XMLPROCESSORLIAISON =
         "ER_COULDNOT_CREATE_XMLPROCESSORLIAISON";
  public static final String ER_DIDNOT_FIND_XPATH_SELECT_EXP =
         "ER_DIDNOT_FIND_XPATH_SELECT_EXP";
  public static final String ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH =
         "ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH";
  public static final String ER_ERROR_OCCURED = "ER_ERROR_OCCURED";
  public static final String ER_ILLEGAL_VARIABLE_REFERENCE =
         "ER_ILLEGAL_VARIABLE_REFERENCE";
  public static final String ER_AXES_NOT_ALLOWED = "ER_AXES_NOT_ALLOWED";
  public static final String ER_KEY_HAS_TOO_MANY_ARGS =
         "ER_KEY_HAS_TOO_MANY_ARGS";
  public static final String ER_COUNT_TAKES_1_ARG = "ER_COUNT_TAKES_1_ARG";
  public static final String ER_COULDNOT_FIND_FUNCTION =
         "ER_COULDNOT_FIND_FUNCTION";
  public static final String ER_UNSUPPORTED_ENCODING ="ER_UNSUPPORTED_ENCODING";
  public static final String ER_PROBLEM_IN_DTM_NEXTSIBLING =
         "ER_PROBLEM_IN_DTM_NEXTSIBLING";
  public static final String ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL =
         "ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL";
  public static final String ER_SETDOMFACTORY_NOT_SUPPORTED =
         "ER_SETDOMFACTORY_NOT_SUPPORTED";
  public static final String ER_PREFIX_MUST_RESOLVE = "ER_PREFIX_MUST_RESOLVE";
  public static final String ER_PARSE_NOT_SUPPORTED = "ER_PARSE_NOT_SUPPORTED";
  //public static final String ER_CREATEDOCUMENT_NOT_SUPPORTED =
//       "ER_CREATEDOCUMENT_NOT_SUPPORTED";
  //public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT =
//       "ER_CHILD_HAS_NO_OWNER_DOCUMENT";
  //public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT =
//       "ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT";
  public static final String ER_SAX_API_NOT_HANDLED = "ER_SAX_API_NOT_HANDLED";
public static final String ER_IGNORABLE_WHITESPACE_NOT_HANDLED =
         "ER_IGNORABLE_WHITESPACE_NOT_HANDLED";
  public static final String ER_DTM_CANNOT_HANDLE_NODES =
         "ER_DTM_CANNOT_HANDLE_NODES";
  public static final String ER_XERCES_CANNOT_HANDLE_NODES =
         "ER_XERCES_CANNOT_HANDLE_NODES";
  public static final String ER_XERCES_PARSE_ERROR_DETAILS =
         "ER_XERCES_PARSE_ERROR_DETAILS";
  public static final String ER_XERCES_PARSE_ERROR = "ER_XERCES_PARSE_ERROR";
  //public static final String ER_CANT_OUTPUT_TEXT_BEFORE_DOC =
//       "ER_CANT_OUTPUT_TEXT_BEFORE_DOC";
  //public static final String ER_CANT_HAVE_MORE_THAN_ONE_ROOT =
//       "ER_CANT_HAVE_MORE_THAN_ONE_ROOT";
  public static final String ER_INVALID_UTF16_SURROGATE =
         "ER_INVALID_UTF16_SURROGATE";
  public static final String ER_OIERROR = "ER_OIERROR";
  public static final String ER_CANNOT_CREATE_URL = "ER_CANNOT_CREATE_URL";
  public static final String ER_XPATH_READOBJECT = "ER_XPATH_READOBJECT";
 public static final String ER_FUNCTION_TOKEN_NOT_FOUND =
         "ER_FUNCTION_TOKEN_NOT_FOUND";
 //public static final String ER_ARG_LOCALNAME_NULL = "ER_ARG_LOCALNAME_NULL";
  public static final String ER_CANNOT_DEAL_XPATH_TYPE =
         "ER_CANNOT_DEAL_XPATH_TYPE";
  public static final String ER_NODESET_NOT_MUTABLE = "ER_NODESET_NOT_MUTABLE";
  public static final String ER_NODESETDTM_NOT_MUTABLE =
         "ER_NODESETDTM_NOT_MUTABLE";
   /**  Variable not resolvable:   */
  public static final String ER_VAR_NOT_RESOLVABLE = "ER_VAR_NOT_RESOLVABLE";
   /** Null error handler  */
 public static final String ER_NULL_ERROR_HANDLER = "ER_NULL_ERROR_HANDLER";
   /**  Programmer's assertion: unknown opcode  */
  public static final String ER_PROG_ASSERT_UNKNOWN_OPCODE =
         "ER_PROG_ASSERT_UNKNOWN_OPCODE";
   /**  0 or 1   */
  public static final String ER_ZERO_OR_ONE = "ER_ZERO_OR_ONE";
   /**  rtf() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER =
         "ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
   /**  asNodeIterator() not supported by XRTreeFragSelectWrapper   */
  public static final String ER_ASNODEITERATOR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER = "ER_ASNODEITERATOR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER";
   /**  fsb() not supported for XStringForChars   */
  public static final String ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS =
         "ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS";
   /**  Could not find variable with the name of   */
 public static final String ER_COULD_NOT_FIND_VAR = "ER_COULD_NOT_FIND_VAR";
   /**  XStringForChars can not take a string for an argument   */
 public static final String ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING =
         "ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING";
   /**  The FastStringBuffer argument can not be null   */
 public static final String ER_FASTSTRINGBUFFER_CANNOT_BE_NULL =
         "ER_FASTSTRINGBUFFER_CANNOT_BE_NULL";
   /**  2 or 3   */
  public static final String ER_TWO_OR_THREE = "ER_TWO_OR_THREE";
   /** Variable accessed before it is bound! */
  public static final String ER_VARIABLE_ACCESSED_BEFORE_BIND =
         "ER_VARIABLE_ACCESSED_BEFORE_BIND";
   /** XStringForFSB can not take a string for an argument! */
 public static final String ER_FSB_CANNOT_TAKE_STRING =
         "ER_FSB_CANNOT_TAKE_STRING";
   /** Error! Setting the root of a walker to null! */
  public static final String ER_SETTING_WALKER_ROOT_TO_NULL =
         "ER_SETTING_WALKER_ROOT_TO_NULL";
   /** This NodeSetDTM can not iterate to a previous node! */
  public static final String ER_NODESETDTM_CANNOT_ITERATE =
         "ER_NODESETDTM_CANNOT_ITERATE";
  /** This NodeSet can not iterate to a previous node! */
 public static final String ER_NODESET_CANNOT_ITERATE =
         "ER_NODESET_CANNOT_ITERATE";
  /** This NodeSetDTM can not do indexing or counting functions! */
  public static final String ER_NODESETDTM_CANNOT_INDEX =
         "ER_NODESETDTM_CANNOT_INDEX";
  /** This NodeSet can not do indexing or counting functions! */
  public static final String ER_NODESET_CANNOT_INDEX =
         "ER_NODESET_CANNOT_INDEX";
  /** Can not call setShouldCacheNodes after nextNode has been called! */
  public static final String ER_CANNOT_CALL_SETSHOULDCACHENODE =
         "ER_CANNOT_CALL_SETSHOULDCACHENODE";
  /** {0} only allows {1} arguments */
 public static final String ER_ONLY_ALLOWS = "ER_ONLY_ALLOWS";
  /** Programmer's assertion in getNextStepPos: unknown stepType: {0} */
  public static final String ER_UNKNOWN_STEP = "ER_UNKNOWN_STEP";
  /** Problem with RelativeLocationPath */
  public static final String ER_EXPECTED_REL_LOC_PATH =
         "ER_EXPECTED_REL_LOC_PATH";
  /** Problem with LocationPath */
  public static final String ER_EXPECTED_LOC_PATH = "ER_EXPECTED_LOC_PATH";
  /** Problem with Step */
  public static final String ER_EXPECTED_LOC_STEP = "ER_EXPECTED_LOC_STEP";
  /** Problem with NodeTest */
  public static final String ER_EXPECTED_NODE_TEST = "ER_EXPECTED_NODE_TEST";
  /** Expected step pattern */
  public static final String ER_EXPECTED_STEP_PATTERN =
        "ER_EXPECTED_STEP_PATTERN";
  /** Expected relative path pattern */
  public static final String ER_EXPECTED_REL_PATH_PATTERN =
         "ER_EXPECTED_REL_PATH_PATTERN";
  /** localname in QNAME should be a valid NCName */
  //public static final String ER_ARG_LOCALNAME_INVALID =
//       "ER_ARG_LOCALNAME_INVALID";
  /** prefix in QNAME should be a valid NCName */
  //public static final String ER_ARG_PREFIX_INVALID = "ER_ARG_PREFIX_INVALID";
  /** Field ER_CANT_CONVERT_TO_BOOLEAN          */
  public static final String ER_CANT_CONVERT_TO_BOOLEAN =
         "ER_CANT_CONVERT_TO_BOOLEAN";
  /** Field ER_CANT_CONVERT_TO_SINGLENODE       */
  public static final String ER_CANT_CONVERT_TO_SINGLENODE =
         "ER_CANT_CONVERT_TO_SINGLENODE";
  /** Field ER_CANT_GET_SNAPSHOT_LENGTH         */
  public static final String ER_CANT_GET_SNAPSHOT_LENGTH =
         "ER_CANT_GET_SNAPSHOT_LENGTH";
  /** Field ER_NON_ITERATOR_TYPE                */
  public static final String ER_NON_ITERATOR_TYPE = "ER_NON_ITERATOR_TYPE";
  /** Field ER_DOC_MUTATED                      */
  public static final String ER_DOC_MUTATED = "ER_DOC_MUTATED";
  public static final String ER_INVALID_XPATH_TYPE = "ER_INVALID_XPATH_TYPE";
  public static final String ER_EMPTY_XPATH_RESULT = "ER_EMPTY_XPATH_RESULT";
  public static final String ER_INCOMPATIBLE_TYPES = "ER_INCOMPATIBLE_TYPES";
  public static final String ER_NULL_RESOLVER = "ER_NULL_RESOLVER";
  public static final String ER_CANT_CONVERT_TO_STRING =
         "ER_CANT_CONVERT_TO_STRING";
  public static final String ER_NON_SNAPSHOT_TYPE = "ER_NON_SNAPSHOT_TYPE";
  public static final String ER_WRONG_DOCUMENT = "ER_WRONG_DOCUMENT";
  /* Note to translators:  The XPath expression cannot be evaluated with respect
   * to this type of node.
   */
  /** Field ER_WRONG_NODETYPE                    */
  public static final String ER_WRONG_NODETYPE = "ER_WRONG_NODETYPE";
  public static final String ER_XPATH_ERROR = "ER_XPATH_ERROR";

  public static final String WG_LOCALE_NAME_NOT_HANDLED =
         "WG_LOCALE_NAME_NOT_HANDLED";
  public static final String WG_PROPERTY_NOT_SUPPORTED =
         "WG_PROPERTY_NOT_SUPPORTED";
  public static final String WG_DONT_DO_ANYTHING_WITH_NS =
         "WG_DONT_DO_ANYTHING_WITH_NS";
  public static final String WG_SECURITY_EXCEPTION = "WG_SECURITY_EXCEPTION";
  public static final String WG_QUO_NO_LONGER_DEFINED =
         "WG_QUO_NO_LONGER_DEFINED";
  public static final String WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST =
         "WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST";
  public static final String WG_FUNCTION_TOKEN_NOT_FOUND =
         "WG_FUNCTION_TOKEN_NOT_FOUND";
  public static final String WG_COULDNOT_FIND_FUNCTION =
         "WG_COULDNOT_FIND_FUNCTION";
  public static final String WG_CANNOT_MAKE_URL_FROM ="WG_CANNOT_MAKE_URL_FROM";
  public static final String WG_EXPAND_ENTITIES_NOT_SUPPORTED =
         "WG_EXPAND_ENTITIES_NOT_SUPPORTED";
  public static final String WG_ILLEGAL_VARIABLE_REFERENCE =
         "WG_ILLEGAL_VARIABLE_REFERENCE";
  public static final String WG_UNSUPPORTED_ENCODING ="WG_UNSUPPORTED_ENCODING";


  // Error messages...

  /**
   * Get the association list.
   *
   * @return The association list.
   */
  public Object[][] getContents()
  {
    return new Object[][]{

  /** Field ERROR0000          */

//  public static final int ERROR0000 = 0;

  { "ERROR0000" , "{0}" },


  /** Field ER_CURRENT_NOT_ALLOWED_IN_MATCH          */
//  public static final int ER_CURRENT_NOT_ALLOWED_IN_MATCH = 1;

  { ER_CURRENT_NOT_ALLOWED_IN_MATCH, "La funci\u00f3 current() no \u00e9s permesa en un patr\u00f3 de coincid\u00e8ncia. " },

  /** Field ER_CURRENT_TAKES_NO_ARGS          */
  //public static final int ER_CURRENT_TAKES_NO_ARGS = 2;

  { ER_CURRENT_TAKES_NO_ARGS, "La funci\u00f3 current() no accepta arguments. " },

  /** Field ER_DOCUMENT_REPLACED          */
//  public static final int ER_DOCUMENT_REPLACED = 3;
  { ER_DOCUMENT_REPLACED,
      "La implementaci\u00f3 de la funci\u00f3 document() s'ha substitu\u00eft per org.apache.xalan.xslt.FuncDocument. "},


  /** Field ER_CONTEXT_HAS_NO_OWNERDOC          */
 // public static final int ER_CONTEXT_HAS_NO_OWNERDOC = 4;

  { ER_CONTEXT_HAS_NO_OWNERDOC,
      "El context no t\u00e9 un document de propietari. "},

  /** Field ER_LOCALNAME_HAS_TOO_MANY_ARGS          */
 // public static final int ER_LOCALNAME_HAS_TOO_MANY_ARGS = 5;

  { ER_LOCALNAME_HAS_TOO_MANY_ARGS,
      "local-name() t\u00e9 massa arguments. "},

  /** Field ER_NAMESPACEURI_HAS_TOO_MANY_ARGS          */
 //public static final int ER_NAMESPACEURI_HAS_TOO_MANY_ARGS = 6;

  { ER_NAMESPACEURI_HAS_TOO_MANY_ARGS,
      "namespace-uri() t\u00e9 massa arguments. "},

  /** Field ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS          */
//  public static final int ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS = 7;
  { ER_NORMALIZESPACE_HAS_TOO_MANY_ARGS,
      "normalize-space() t\u00e9 massa arguments. "},

  /** Field ER_NUMBER_HAS_TOO_MANY_ARGS          */
//  public static final int ER_NUMBER_HAS_TOO_MANY_ARGS = 8;

  { ER_NUMBER_HAS_TOO_MANY_ARGS,
      "number() t\u00e9 massa arguments. "},

  /** Field ER_NAME_HAS_TOO_MANY_ARGS          */
//  public static final int ER_NAME_HAS_TOO_MANY_ARGS = 9;

  { ER_NAME_HAS_TOO_MANY_ARGS,
     "name() t\u00e9 massa arguments. "},

  /** Field ER_STRING_HAS_TOO_MANY_ARGS          */
//  public static final int ER_STRING_HAS_TOO_MANY_ARGS = 10;

  { ER_STRING_HAS_TOO_MANY_ARGS,
      "string() t\u00e9 massa arguments. "},

  /** Field ER_STRINGLENGTH_HAS_TOO_MANY_ARGS          */
//  public static final int ER_STRINGLENGTH_HAS_TOO_MANY_ARGS = 11;

  { ER_STRINGLENGTH_HAS_TOO_MANY_ARGS,
      "string-length() t\u00e9 massa arguments. "},

  /** Field ER_TRANSLATE_TAKES_3_ARGS          */
//  public static final int ER_TRANSLATE_TAKES_3_ARGS = 12;

  { ER_TRANSLATE_TAKES_3_ARGS,
      "La funci\u00f3 translate() t\u00e9 tres arguments. "},

  /** Field ER_UNPARSEDENTITYURI_TAKES_1_ARG          */
//  public static final int ER_UNPARSEDENTITYURI_TAKES_1_ARG = 13;

  { ER_UNPARSEDENTITYURI_TAKES_1_ARG,
      "La funci\u00f3 unparsed-entity-uri ha de tenir un argument. "},

  /** Field ER_NAMESPACEAXIS_NOT_IMPLEMENTED          */
//  public static final int ER_NAMESPACEAXIS_NOT_IMPLEMENTED = 14;

  { ER_NAMESPACEAXIS_NOT_IMPLEMENTED,
      "L'eix de l'espai de noms encara no s'ha implementat. "},

  /** Field ER_UNKNOWN_AXIS          */
//  public static final int ER_UNKNOWN_AXIS = 15;

  { ER_UNKNOWN_AXIS,
     "Eix desconegut: {0} "},

  /** Field ER_UNKNOWN_MATCH_OPERATION          */
//  public static final int ER_UNKNOWN_MATCH_OPERATION = 16;

  { ER_UNKNOWN_MATCH_OPERATION,
     "Operaci\u00f3 de coincid\u00e8ncia desconeguda. "},

  /** Field ER_INCORRECT_ARG_LENGTH          */
//  public static final int ER_INCORRECT_ARG_LENGTH = 17;

  { ER_INCORRECT_ARG_LENGTH,
      "La longitud de l'argument de la prova de node processing-instruction() no \u00e9s correcta. "},

  /** Field ER_CANT_CONVERT_TO_NUMBER          */
//  public static final int ER_CANT_CONVERT_TO_NUMBER = 18;

  { ER_CANT_CONVERT_TO_NUMBER,
      "No es pot convertir {0} en un n\u00famero. "},

  /** Field ER_CANT_CONVERT_TO_NODELIST          */
  //public static final int ER_CANT_CONVERT_TO_NODELIST = 19;

  { ER_CANT_CONVERT_TO_NODELIST,
      "No es pot convertir {0} en una NodeList. "},

  /** Field ER_CANT_CONVERT_TO_MUTABLENODELIST          */
//  public static final int ER_CANT_CONVERT_TO_MUTABLENODELIST = 20;

  { ER_CANT_CONVERT_TO_MUTABLENODELIST,
      "No es pot convertir {0} en un NodeSetDTM. "},

  /** Field ER_CANT_CONVERT_TO_TYPE          */
//  public static final int ER_CANT_CONVERT_TO_TYPE = 21;

  { ER_CANT_CONVERT_TO_TYPE,
      "No es pot convertir {0} en un tipus #{1} "},

  /** Field ER_EXPECTED_MATCH_PATTERN          */
//  public static final int ER_EXPECTED_MATCH_PATTERN = 22;

  { ER_EXPECTED_MATCH_PATTERN,
      "El patr\u00f3 de coincid\u00e8ncia de getMatchScore \u00e9s l'esperat. "},

  /** Field ER_COULDNOT_GET_VAR_NAMED          */
//  public static final int ER_COULDNOT_GET_VAR_NAMED = 23;

  { ER_COULDNOT_GET_VAR_NAMED,
      "No s''ha pogut obtenir la variable {0}. "},

  /** Field ER_UNKNOWN_OPCODE          */
//  public static final int ER_UNKNOWN_OPCODE = 24;

  { ER_UNKNOWN_OPCODE,
     "ERROR. Codi op desconegut: {0} "},

  /** Field ER_EXTRA_ILLEGAL_TOKENS          */
//  public static final int ER_EXTRA_ILLEGAL_TOKENS = 25;

  { ER_EXTRA_ILLEGAL_TOKENS,
     "Senyals addicionals no permesos: {0} "},

  /** Field ER_EXPECTED_DOUBLE_QUOTE          */
//  public static final int ER_EXPECTED_DOUBLE_QUOTE = 26;

  { ER_EXPECTED_DOUBLE_QUOTE,
      "Les cometes del literal s\u00f3n incorrectes. Hi ha d'haver cometes dobles. "},

  /** Field ER_EXPECTED_SINGLE_QUOTE          */
//  public static final int ER_EXPECTED_SINGLE_QUOTE = 27;

  { ER_EXPECTED_SINGLE_QUOTE,
      "Les cometes del literal s\u00f3n incorrectes. Hi ha d'haver una cometa simple. "},

  /** Field ER_EMPTY_EXPRESSION          */
//  public static final int ER_EMPTY_EXPRESSION = 28;

  { ER_EMPTY_EXPRESSION,
     "Expressi\u00f3 buida. "},

  /** Field ER_EXPECTED_BUT_FOUND          */
//  public static final int ER_EXPECTED_BUT_FOUND = 29;

  { ER_EXPECTED_BUT_FOUND,
     "S''esperava {0}, per\u00f2 s''ha detectat {1} "},

  /** Field ER_INCORRECT_PROGRAMMER_ASSERTION          */
//  public static final int ER_INCORRECT_PROGRAMMER_ASSERTION = 30;

  { ER_INCORRECT_PROGRAMMER_ASSERTION,
      "L''afirmaci\u00f3 del programador \u00e9s incorrecta. - {0} "},

  /** Field ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL          */
//  public static final int ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL = 31;

  { ER_BOOLEAN_ARG_NO_LONGER_OPTIONAL,
      "L'argument boolean(...) ja no \u00e9s opcional amb l'esborrany d'XPath 19990709. "},

  /** Field ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG          */
//  public static final int ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG = 32;

  { ER_FOUND_COMMA_BUT_NO_PRECEDING_ARG,
      "S'ha trobat ',' per\u00f2 no hi havia cap argument abans. "},

  /** Field ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG          */
//  public static final int ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG = 33;

  { ER_FOUND_COMMA_BUT_NO_FOLLOWING_ARG,
      "S'ha trobat ',' per\u00f2 no hi havia cap argument darrere. "},

  /** Field ER_PREDICATE_ILLEGAL_SYNTAX          */
//  public static final int ER_PREDICATE_ILLEGAL_SYNTAX = 34;

  { ER_PREDICATE_ILLEGAL_SYNTAX,
      "'..[predicate]' o '.[predicate]' no \u00e9s una sintaxi permesa. En comptes d'aix\u00f2, utilitzeu 'self::node()[predicate]'. "},

  /** Field ER_ILLEGAL_AXIS_NAME          */
//  public static final int ER_ILLEGAL_AXIS_NAME = 35;

  { ER_ILLEGAL_AXIS_NAME,
     "Nom d''eix no perm\u00e8s: {0} "},

  /** Field ER_UNKNOWN_NODETYPE          */
//  public static final int ER_UNKNOWN_NODETYPE = 36;

  { ER_UNKNOWN_NODETYPE,
     "Tipus de node desconegut: {0} "},

  /** Field ER_PATTERN_LITERAL_NEEDS_BE_QUOTED          */
//  public static final int ER_PATTERN_LITERAL_NEEDS_BE_QUOTED = 37;

  { ER_PATTERN_LITERAL_NEEDS_BE_QUOTED,
      "El literal de patr\u00f3 ({0}) ha d''anar entre cometes. "},

  /** Field ER_COULDNOT_BE_FORMATTED_TO_NUMBER          */
//  public static final int ER_COULDNOT_BE_FORMATTED_TO_NUMBER = 38;

  { ER_COULDNOT_BE_FORMATTED_TO_NUMBER,
      "{0} no s''ha pogut formatar com a n\u00famero. "},

  /** Field ER_COULDNOT_CREATE_XMLPROCESSORLIAISON          */
//  public static final int ER_COULDNOT_CREATE_XMLPROCESSORLIAISON = 39;

  { ER_COULDNOT_CREATE_XMLPROCESSORLIAISON,
      "No s''ha pogut crear la relaci\u00f3 XML TransformerFactory: {0} "},

  /** Field ER_DIDNOT_FIND_XPATH_SELECT_EXP          */
//  public static final int ER_DIDNOT_FIND_XPATH_SELECT_EXP = 40;

  { ER_DIDNOT_FIND_XPATH_SELECT_EXP,
      "Error. No s'ha trobat l'expressi\u00f3 select d'xpath (-select). "},

  /** Field ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH          */
//  public static final int ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH = 41;

  { ER_COULDNOT_FIND_ENDOP_AFTER_OPLOCATIONPATH,
      "ERROR. No s'ha trobat ENDOP despr\u00e9s d'OP_LOCATIONPATH. "},

  /** Field ER_ERROR_OCCURED          */
//  public static final int ER_ERROR_OCCURED = 42;

  { ER_ERROR_OCCURED,
     "S'ha produ\u00eft un error. "},

  /** Field ER_ILLEGAL_VARIABLE_REFERENCE          */
//  public static final int ER_ILLEGAL_VARIABLE_REFERENCE = 43;

  { ER_ILLEGAL_VARIABLE_REFERENCE,
      "S''ha donat VariableReference per a una variable fora de context o sense definici\u00f3. Nom = {0} "},

  /** Field ER_AXES_NOT_ALLOWED          */
//  public static final int ER_AXES_NOT_ALLOWED = 44;

  { ER_AXES_NOT_ALLOWED,
      "Nom\u00e9s es permeten els eixos subordinat:: i atribut:: en els patrons de coincid\u00e8ncia. Eixos incorrectes = {0} "},

  /** Field ER_KEY_HAS_TOO_MANY_ARGS          */
//  public static final int ER_KEY_HAS_TOO_MANY_ARGS = 45;

  { ER_KEY_HAS_TOO_MANY_ARGS,
      "key() t\u00e9 un nombre incorrecte d'arguments. "},

  /** Field ER_COUNT_TAKES_1_ARG          */
//  public static final int ER_COUNT_TAKES_1_ARG = 46;

  { ER_COUNT_TAKES_1_ARG,
      "La funci\u00f3 count ha de tenir un argument. "},

  /** Field ER_COULDNOT_FIND_FUNCTION          */
//  public static final int ER_COULDNOT_FIND_FUNCTION = 47;

  { ER_COULDNOT_FIND_FUNCTION,
     "No s''ha pogut trobar la funci\u00f3: {0} "},

  /** Field ER_UNSUPPORTED_ENCODING          */
//  public static final int ER_UNSUPPORTED_ENCODING = 48;

  { ER_UNSUPPORTED_ENCODING,
     "Codificaci\u00f3 sense suport: {0} "},

  /** Field ER_PROBLEM_IN_DTM_NEXTSIBLING          */
//  public static final int ER_PROBLEM_IN_DTM_NEXTSIBLING = 49;

  { ER_PROBLEM_IN_DTM_NEXTSIBLING,
      "S'ha produ\u00eft un error en el DTM de getNextSibling. S'intentar\u00e0 solucionar. "},

  /** Field ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL          */
//  public static final int ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL = 50;

  { ER_CANNOT_WRITE_TO_EMPTYNODELISTIMPL,
      "Error del programador: no es pot escriure a EmptyNodeList. "},

  /** Field ER_SETDOMFACTORY_NOT_SUPPORTED          */
//  public static final int ER_SETDOMFACTORY_NOT_SUPPORTED = 51;

  { ER_SETDOMFACTORY_NOT_SUPPORTED,
      "XPathContext no d\u00f3na suport a setDOMFactory. "},

  /** Field ER_PREFIX_MUST_RESOLVE          */
//  public static final int ER_PREFIX_MUST_RESOLVE = 52;

  { ER_PREFIX_MUST_RESOLVE,
      "El prefix s''ha de resoldre en un espai de noms: {0} "},

  /** Field ER_PARSE_NOT_SUPPORTED          */
//  public static final int ER_PARSE_NOT_SUPPORTED = 53;

  { ER_PARSE_NOT_SUPPORTED,
      "L''an\u00e0lisi (origen InputSource) no t\u00e9 suport a XPathContext. No es pot obrir {0}. "},

  /** Field ER_CREATEDOCUMENT_NOT_SUPPORTED          */
//  public static final int ER_CREATEDOCUMENT_NOT_SUPPORTED = 54;

  //{ ER_CREATEDOCUMENT_NOT_SUPPORTED,
  //    "createDocument() not supported in XPathContext!"},

  /** Field ER_CHILD_HAS_NO_OWNER_DOCUMENT          */
//  public static final int ER_CHILD_HAS_NO_OWNER_DOCUMENT = 55;

  //{ ER_CHILD_HAS_NO_OWNER_DOCUMENT,
  //    "Attribute child does not have an owner document!"},

  /** Field ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT          */
//  public static final int ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT = 56;

  //{ ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
  //    "Attribute child does not have an owner document element!"},

  /** Field ER_SAX_API_NOT_HANDLED          */
//  public static final int ER_SAX_API_NOT_HANDLED = 57;

  { ER_SAX_API_NOT_HANDLED,
      "Car\u00e0cters SAX API (char ch[]... no es poden gestionar pel DTM. "},

  /** Field ER_IGNORABLE_WHITESPACE_NOT_HANDLED          */
//public static final int ER_IGNORABLE_WHITESPACE_NOT_HANDLED = 58;

  { ER_IGNORABLE_WHITESPACE_NOT_HANDLED,
      "ignorableWhitespace(char ch[]... no es poden gestionar pel DTM. "},

  /** Field ER_DTM_CANNOT_HANDLE_NODES          */
//  public static final int ER_DTM_CANNOT_HANDLE_NODES = 59;

  { ER_DTM_CANNOT_HANDLE_NODES,
      "DTMLiaison no pot gestionar nodes del tipus {0}. "},

  /** Field ER_XERCES_CANNOT_HANDLE_NODES          */
//  public static final int ER_XERCES_CANNOT_HANDLE_NODES = 60;

  { ER_XERCES_CANNOT_HANDLE_NODES,
      "DOM2Helper no pot gestionar nodes del tipus {0}. "},

  /** Field ER_XERCES_PARSE_ERROR_DETAILS          */
//  public static final int ER_XERCES_PARSE_ERROR_DETAILS = 61;

  { ER_XERCES_PARSE_ERROR_DETAILS,
      "Error de DOM2Helper.parse: ID del sistema - {0} l\u00ednia - {1} "},

  /** Field ER_XERCES_PARSE_ERROR          */
//  public static final int ER_XERCES_PARSE_ERROR = 62;

  { ER_XERCES_PARSE_ERROR,
     "Error de DOM2Helper.parse "},

  /** Field ER_CANT_OUTPUT_TEXT_BEFORE_DOC          */
//  public static final int ER_CANT_OUTPUT_TEXT_BEFORE_DOC = 63;

  //{ ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
   //   "Warning: can't output text before document element!  Ignoring..."},

  /** Field ER_CANT_HAVE_MORE_THAN_ONE_ROOT          */
//  public static final int ER_CANT_HAVE_MORE_THAN_ONE_ROOT = 64;

  //{ ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
   //   "Can't have more than one root on a DOM!"},

  /** Field ER_INVALID_UTF16_SURROGATE          */
//  public static final int ER_INVALID_UTF16_SURROGATE = 65;

  { ER_INVALID_UTF16_SURROGATE,
      "S''ha detectat un suplent UTF-16 no v\u00e0lid: {0} ? "},

  /** Field ER_OIERROR          */
  //public static final int ER_OIERROR = 66;

  { ER_OIERROR,
     "Error d'E/S "},

  /** Field ER_CANNOT_CREATE_URL          */
  //public static final int ER_CANNOT_CREATE_URL = 67;

  { ER_CANNOT_CREATE_URL,
     "No es pot crear la url de: {0} "},

  /** Field ER_XPATH_READOBJECT          */
//  public static final int ER_XPATH_READOBJECT = 68;

  { ER_XPATH_READOBJECT,
     "En XPath.readObject: {0} "},

  /** Field ER_FUNCTION_TOKEN_NOT_FOUND         */
// public static final int ER_FUNCTION_TOKEN_NOT_FOUND = 69;

  { ER_FUNCTION_TOKEN_NOT_FOUND,
      "No s'ha trobat el senyal de funci\u00f3. "},

   /**  Argument 'localName' is null  */
// public static final int ER_ARG_LOCALNAME_NULL = 70;

  //{ ER_ARG_LOCALNAME_NULL,
  //     "Argument 'localName' is null"},

   /**  Can not deal with XPath type:   */
//  public static final int ER_CANNOT_DEAL_XPATH_TYPE = 71;

  { ER_CANNOT_DEAL_XPATH_TYPE,
       "No s''ha pogut tractar amb el tipus d''XPath: {0} "},

   /**  This NodeSet is not mutable  */
 // public static final int ER_NODESET_NOT_MUTABLE = 72;

  { ER_NODESET_NOT_MUTABLE,
       "Aquest NodeSet no \u00e9s mutable. "},

   /**  This NodeSetDTM is not mutable  */
//  public static final int ER_NODESETDTM_NOT_MUTABLE = 73;

  { ER_NODESETDTM_NOT_MUTABLE,
       "Aquest NodeSetDTM no \u00e9s mutable. "},

   /**  Variable not resolvable:   */
//  public static final int ER_VAR_NOT_RESOLVABLE = 74;

  { ER_VAR_NOT_RESOLVABLE,
        "No es pot resoldre la variable: {0} "},

   /** Null error handler  */
// public static final int ER_NULL_ERROR_HANDLER = 75;

  { ER_NULL_ERROR_HANDLER,
        "Manejador d'error nul "},

   /**  Programmer's assertion: unknown opcode  */
 // public static final int ER_PROG_ASSERT_UNKNOWN_OPCODE = 76;

  { ER_PROG_ASSERT_UNKNOWN_OPCODE,
       "Afirmaci\u00f3 del programador: opcode desconegut: {0}"},

   /**  0 or 1   */
//  public static final int ER_ZERO_OR_ONE = 77;

  { ER_ZERO_OR_ONE,
       "0 o 1 "},


   /**  rtf() not supported by XRTreeFragSelectWrapper   */
  //public static final int ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER = 78;

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "rtf() no t\u00e9 suport de XRTreeFragSelectWrapper "},

   /**  asNodeIterator() not supported by XRTreeFragSelectWrapper   */
  //public static final int ER_ASNODEITERATOR_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER = 79;

  { ER_RTF_NOT_SUPPORTED_XRTREEFRAGSELECTWRAPPER,
       "asNodeIterator() no t\u00e9 suport de XRTreeFragSelectWrapper "},

   /**  fsb() not supported for XStringForChars   */
 // public static final int ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS = 80;

  { ER_FSB_NOT_SUPPORTED_XSTRINGFORCHARS,
       "fsb() no t\u00e9 suport de XStringForChars "},

   /**  Could not find variable with the name of   */
// public static final int ER_COULD_NOT_FIND_VAR = 81;

  { ER_COULD_NOT_FIND_VAR,
      "No s''ha trobat la variable amb el nom de {0} "},

   /**  XStringForChars can not take a string for an argument   */
// public static final int ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING = 82;

  { ER_XSTRINGFORCHARS_CANNOT_TAKE_STRING,
      "XStringForChars no pot agafar una cadena com a argument. "},

   /**  The FastStringBuffer argument can not be null   */
// public static final int ER_FASTSTRINGBUFFER_CANNOT_BE_NULL = 83;

  { ER_FASTSTRINGBUFFER_CANNOT_BE_NULL,
      "L'argument FastStringBuffer no pot ser nul."},

  /* MANTIS_XALAN CHANGE: BEGIN */
   /**  2 or 3   */
//  public static final int ER_TWO_OR_THREE = 84;

  { ER_TWO_OR_THREE,
       "2 o 3 "},

   /** Variable accessed before it is bound! */
//  public static final int ER_VARIABLE_ACCESSED_BEFORE_BIND = 85;

  { ER_VARIABLE_ACCESSED_BEFORE_BIND,
       "S'ha accedit a la variable abans que estigu\u00e9s vinculada. "},

   /** XStringForFSB can not take a string for an argument! */
// public static final int ER_FSB_CANNOT_TAKE_STRING = 86;

  { ER_FSB_CANNOT_TAKE_STRING,
       "XStringForFSB no pot agafar una cadena com a argument. "},

   /** Error! Setting the root of a walker to null! */
//  public static final int ER_SETTING_WALKER_ROOT_TO_NULL = 87;

  { ER_SETTING_WALKER_ROOT_TO_NULL,
       "\n Error. S'est\u00e0 establint l'arrel d'un itinerant en nul. "},

   /** This NodeSetDTM can not iterate to a previous node! */
//  public static final int ER_NODESETDTM_CANNOT_ITERATE = 88;

  { ER_NODESETDTM_CANNOT_ITERATE,
       "Aquest NodeSetDTM no es pot iterar en un node previ "},

  /** This NodeSet can not iterate to a previous node! */
// public static final int ER_NODESET_CANNOT_ITERATE = 89;

  { ER_NODESET_CANNOT_ITERATE,
       "Aquest NodeSet no es pot iterar en un node previ "},

  /** This NodeSetDTM can not do indexing or counting functions! */
//  public static final int ER_NODESETDTM_CANNOT_INDEX = 90;

  { ER_NODESETDTM_CANNOT_INDEX,
       "Aquest NodeSetDTM no pot indexar ni efectuar funcions de recompte "},

  /** This NodeSet can not do indexing or counting functions! */
//  public static final int ER_NODESET_CANNOT_INDEX = 91;

  { ER_NODESET_CANNOT_INDEX,
       "Aquest NodeSet no pot indexar ni efectuar funcions de recompte "},

  /** Can not call setShouldCacheNodes after nextNode has been called! */
//  public static final int ER_CANNOT_CALL_SETSHOULDCACHENODE = 92;

  { ER_CANNOT_CALL_SETSHOULDCACHENODE,
       "No es pot cridar setShouldCacheNodes despr\u00e9s que s'hagi cridat nextNode "},

  /** {0} only allows {1} arguments */
// public static final int ER_ONLY_ALLOWS = 93;

  { ER_ONLY_ALLOWS,
       "{0} nom\u00e9s permet {1} arguments "},

  /** Programmer's assertion in getNextStepPos: unknown stepType: {0} */
//  public static final int ER_UNKNOWN_STEP = 94;

  { ER_UNKNOWN_STEP,
       "Afirmaci\u00f3 del programador a getNextStepPos: stepType desconegut: {0}"},

  //Note to translators:  A relative location path is a form of XPath expression.
  // The message indicates that such an expression was expected following the
  // characters '/' or '//', but was not found.

  /** Problem with RelativeLocationPath */
//  public static final int ER_EXPECTED_REL_LOC_PATH = 95;

  { ER_EXPECTED_REL_LOC_PATH,
      "S'esperava una via d'acc\u00e9s relativa darrere del senyal '/' o '//'. "},

  // Note to translators:  A location path is a form of XPath expression.
  // The message indicates that syntactically such an expression was expected,but
  // the characters specified by the substitution text were encountered instead.

  /** Problem with LocationPath */
//  public static final int ER_EXPECTED_LOC_PATH = 96;

  { ER_EXPECTED_LOC_PATH,
       "S''esperava una via d''acc\u00e9s d''ubicaci\u00f3, per\u00f2 s''ha trobat el senyal seg\u00fcent\u003a {0}"},

  // Note to translators:  A location step is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected
  // following the specified characters.

  /** Problem with Step */
//  public static final int ER_EXPECTED_LOC_STEP = 97;

  { ER_EXPECTED_LOC_STEP,
       "S'esperava un pas d'ubicaci\u00f3 despr\u00e9s del senyal '/' o '//'. "},

  // Note to translators:  A node test is part of an XPath expression that is
  // used to test for particular kinds of nodes.  In this case, a node test that
  // consists of an NCName followed by a colon and an asterisk or that consists
  // of a QName was expected, but was not found.

  /** Problem with NodeTest */
//  public static final int ER_EXPECTED_NODE_TEST = 98;

  { ER_EXPECTED_NODE_TEST,
       "S'esperava una prova de node que coincid\u00eds amb NCName:* o QName. "},

  // Note to translators:  A step pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but the specified character was found in the expression instead.

  /** Expected step pattern */
//  public static final int ER_EXPECTED_STEP_PATTERN = 99;

  { ER_EXPECTED_STEP_PATTERN,
       "S'esperava un patr\u00f3 de pas per\u00f2 s'ha trobat '/'. "},

  // Note to translators: A relative path pattern is part of an XPath expression.
  // The message indicates that syntactically such an expression was expected,
  // but was not found.

  /** Expected relative path pattern */
//  public static final int ER_EXPECTED_REL_PATH_PATTERN = 100;

  { ER_EXPECTED_REL_PATH_PATTERN,
       "S'esperava un patr\u00f3 de via d'acc\u00e9s relativa. "},

  // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
  // The localname is the portion after the optional colon; the message indicates
  // that there is a problem with that part of the QNAME.

  /** localname in QNAME should be a valid NCName */
//  public static final int ER_ARG_LOCALNAME_INVALID = 101;

  //{ ER_ARG_LOCALNAME_INVALID,
  //     "Localname in QNAME should be a valid NCName"},

  // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
  // The prefix is the portion before the optional colon; the message indicates
  // that there is a problem with that part of the QNAME.

  /** prefix in QNAME should be a valid NCName */
 // public static final int ER_ARG_PREFIX_INVALID = 102;

  //{ ER_ARG_PREFIX_INVALID,
   //    "Prefix in QNAME should be a valid NCName"},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.

  /** Field ER_CANT_CONVERT_TO_BOOLEAN          */
//  public static final int ER_CANT_CONVERT_TO_BOOLEAN = 103;

  { ER_CANT_CONVERT_TO_BOOLEAN,
       "No es pot convertir {0} en boole\u00e0. "},

  // Note to translators: Do not translate ANY_UNORDERED_NODE_TYPE and
  // FIRST_ORDERED_NODE_TYPE.

  /** Field ER_CANT_CONVERT_TO_SINGLENODE       */
  //public static final int ER_CANT_CONVERT_TO_SINGLENODE = 104;

  { ER_CANT_CONVERT_TO_SINGLENODE,
       "No es pot convertir {0} en un node \u00fanic. Aquest captador fa refer\u00e8ncia als tipus ANY_UNORDERED_NODE_TYPE i FIRST_ORDERED_NODE_TYPE. "},

  // Note to translators: Do not translate UNORDERED_NODE_SNAPSHOT_TYPE and
  // ORDERED_NODE_SNAPSHOT_TYPE.

  /** Field ER_CANT_GET_SNAPSHOT_LENGTH         */
//  public static final int ER_CANT_GET_SNAPSHOT_LENGTH = 105;

  { ER_CANT_GET_SNAPSHOT_LENGTH,
       "No es pot obtenir la longitud de la instant\u00e0nia del tipus: {0}. Aquest captador fa refer\u00e8ncia als tipus UNORDERED_NODE_SNAPSHOT_TYPE i ORDERED_NODE_SNAPSHOT_TYPE. "},

  /** Field ER_NON_ITERATOR_TYPE                */
  //public static final int ER_NON_ITERATOR_TYPE        = 106;

  { ER_NON_ITERATOR_TYPE,
       "No es pot iterar en un tipus no iterador: {0} "},

  // Note to translators: This message indicates that the document being operated
  // upon changed, so the iterator object that was being used to traverse the
  // document has now become invalid.

  /** Field ER_DOC_MUTATED                      */
//  public static final int ER_DOC_MUTATED              = 107;

  { ER_DOC_MUTATED,
       "El document s'ha modificat des que es van produir els resultats. L'iterador no \u00e9s v\u00e0lid. "},

  /** Field ER_INVALID_XPATH_TYPE               */
//  public static final int ER_INVALID_XPATH_TYPE       = 108;

  { ER_INVALID_XPATH_TYPE,
       "L''argument de tipus XPath no \u00e9s v\u00e0lid: {0} "},

  /** Field ER_EMPTY_XPATH_RESULT                */
//  public static final int ER_EMPTY_XPATH_RESULT       = 109;

  { ER_EMPTY_XPATH_RESULT,
       "L'objecte de resultats XPath est\u00e0 buit. "},

  /** Field ER_INCOMPATIBLE_TYPES                */
//  public static final int ER_INCOMPATIBLE_TYPES       = 110;

  { ER_INCOMPATIBLE_TYPES,
       "El tipus retornat {0} no es pot encaixar en el tipus especificat {1} "},

  /** Field ER_NULL_RESOLVER                     */
 // public static final int ER_NULL_RESOLVER            = 111;

  { ER_NULL_RESOLVER,
       "No es pot resoldre el prefix amb un solucionador de prefix nul. "},

  // Note to translators:  The substitution text is the name of a data type.  The
  // message indicates that a value of a particular type could not be converted
  // to a value of type string.

  /** Field ER_CANT_CONVERT_TO_STRING            */
//  public static final int ER_CANT_CONVERT_TO_STRING   = 112;

  { ER_CANT_CONVERT_TO_STRING,
       "No es pot convertir {0} en cadena. "},

  // Note to translators: Do not translate snapshotItem,
  // UNORDERED_NODE_SNAPSHOT_TYPE and ORDERED_NODE_SNAPSHOT_TYPE.

  /** Field ER_NON_SNAPSHOT_TYPE                 */
//  public static final int ER_NON_SNAPSHOT_TYPE       = 113;

  { ER_NON_SNAPSHOT_TYPE,
       "No es pot cridar snapshotItem en el tipus: {0}. Aquest m\u00e8tode fa refer\u00e8ncia als tipus UNORDERED_NODE_SNAPSHOT_TYPE i ORDERED_NODE_SNAPSHOT_TYPE. "},

  // Note to translators:  XPathEvaluator is a Java interface name.  An
  // XPathEvaluator is created with respect to a particular XML document, and in
  // this case the expression represented by this object was being evaluated with
  // respect to a context node from a different document.

  /** Field ER_WRONG_DOCUMENT                    */
//  public static final int ER_WRONG_DOCUMENT          = 114;

  { ER_WRONG_DOCUMENT,
       "El node de context no pertany al document vinculat a aquest XPathEvaluator. "},

  // Note to translators:  The XPath expression cannot be evaluated with respect
  // to this type of node.
  /** Field ER_WRONG_NODETYPE                    */
//  public static final int ER_WRONG_NODETYPE          = 115;

  { ER_WRONG_NODETYPE,
       "El tipus de node de context no t\u00e9 suport. "},

  /** Field ER_XPATH_ERROR                       */
//  public static final int ER_XPATH_ERROR             = 116;

  { ER_XPATH_ERROR,
       "S'ha produ\u00eft un error desconegut a XPath."},


  // Warnings...

  /** Field WG_LOCALE_NAME_NOT_HANDLED          */
//  public static final int WG_LOCALE_NAME_NOT_HANDLED = 1;

  { WG_LOCALE_NAME_NOT_HANDLED,
      "No s'ha gestionat encara el nom d'entorn nacional en la funci\u00f3 format-number. "},

  /** Field WG_PROPERTY_NOT_SUPPORTED          */
//  public static final int WG_PROPERTY_NOT_SUPPORTED = 2;

  { WG_PROPERTY_NOT_SUPPORTED,
      "La propietat XSL no t\u00e9 suport: {0} "},

  /** Field WG_DONT_DO_ANYTHING_WITH_NS          */
//  public static final int WG_DONT_DO_ANYTHING_WITH_NS = 3;

  { WG_DONT_DO_ANYTHING_WITH_NS,
      "No feu res ara mateix amb l''espai de noms {0} de la propietat: {1} "},

  /** Field WG_SECURITY_EXCEPTION          */
// public static final int WG_SECURITY_EXCEPTION = 4;

  { WG_SECURITY_EXCEPTION,
      "S''ha produ\u00eft SecurityException en intentar accedir a la propietat de sistema XSL: {0} "},

  /** Field WG_QUO_NO_LONGER_DEFINED          */
//  public static final int WG_QUO_NO_LONGER_DEFINED = 5;

  { WG_QUO_NO_LONGER_DEFINED,
      "Sintaxi antiga: quo(...) ja no est\u00e0 definit a XPath. "},

  /** Field WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST          */
// public static final int WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST = 6;

  { WG_NEED_DERIVED_OBJECT_TO_IMPLEMENT_NODETEST,
      "XPath necessita un objecte dedu\u00eft per implementar nodeTest. "},

  /** Field WG_FUNCTION_TOKEN_NOT_FOUND          */
//  public static final int WG_FUNCTION_TOKEN_NOT_FOUND = 7;

  { WG_FUNCTION_TOKEN_NOT_FOUND,
      "No s'ha trobat el senyal de funci\u00f3. "},

  /** Field WG_COULDNOT_FIND_FUNCTION          */
//  public static final int WG_COULDNOT_FIND_FUNCTION = 8;

  { WG_COULDNOT_FIND_FUNCTION,
      "No s''ha pogut trobar la funci\u00f3: {0} "},

  /** Field WG_CANNOT_MAKE_URL_FROM          */
//  public static final int WG_CANNOT_MAKE_URL_FROM = 9;

  { WG_CANNOT_MAKE_URL_FROM,
      "No es pot crear la URL de: {0} "},

  /** Field WG_EXPAND_ENTITIES_NOT_SUPPORTED          */
//  public static final int WG_EXPAND_ENTITIES_NOT_SUPPORTED = 10;

  { WG_EXPAND_ENTITIES_NOT_SUPPORTED,
      "L'opci\u00f3 -E no t\u00e9 suport a l'analitzador de DTM "},

  /** Field WG_ILLEGAL_VARIABLE_REFERENCE          */
//  public static final int WG_ILLEGAL_VARIABLE_REFERENCE = 11;

  { WG_ILLEGAL_VARIABLE_REFERENCE,
      "S''ha donat VariableReference per a una variable fora de context o sense definici\u00f3. Nom = {0} "},

  /** Field WG_UNSUPPORTED_ENCODING          */
//  public static final int WG_UNSUPPORTED_ENCODING = 12;

  { WG_UNSUPPORTED_ENCODING,
     "Codificaci\u00f3 sense suport: {0} "},



  // Other miscellaneous text used inside the code...
  { "ui_language", "ca"},
  { "help_language", "ca"},
  { "language", "ca"},
  { "BAD_CODE", "El par\u00e0metre de createMessage estava fora dels l\u00edmits. "},
  { "FORMAT_FAILED", "S'ha generat una excepci\u00f3 durant la crida messageFormat. "},
  { "version", ">>>>>>> Versi\u00f3 Xalan "},
  { "version2", "<<<<<<< "},
  { "yes", "s\u00ed"},
  { "line", "L\u00ednia n\u00fam. "},
  { "column", "Columna n\u00fam. "},
  { "xsldone", "XSLProcessor: fet "},
  { "xpath_option", "Opcions d'xpath: "},
  { "optionIN", "[-in inputXMLURL] "},
  { "optionSelect", "[-select expressi\u00f3 xpath] "},
  { "optionMatch", "[-match patr\u00f3 coincid\u00e8ncia (per a diagn\u00f2stics de coincid\u00e8ncia)] "},
  { "optionAnyExpr", "O nom\u00e9s una expressi\u00f3 xpath far\u00e0 un buidatge de diagn\u00f2stic. "},
  { "noParsermsg1", "El proc\u00e9s XSL no ha estat correcte. "},
  { "noParsermsg2", "** No s'ha trobat l'analitzador ** "},
  { "noParsermsg3", "Comproveu la vostra classpath. "},
  { "noParsermsg4", "Si no teniu XML Parser for Java d'IBM, el podeu baixar d' "},
  { "noParsermsg5", "AlphaWorks d'IBM: http://www.alphaworks.ibm.com/formula/xml "},
  { "gtone", ">1 " },
  { "zero", "0 " },
  { "one", "1 " },
  { "two" , "2 " },
  { "three", "3 " }

  };
  }


  // ================= INFRASTRUCTURE ======================

  /** Field BAD_CODE          */
  public static final String BAD_CODE = "BAD_CODE";

  /** Field FORMAT_FAILED          */
  public static final String FORMAT_FAILED = "FORMAT_FAILED";

  /** Field ERROR_RESOURCES          */
  public static final String ERROR_RESOURCES =
    "org.apache.xpath.res.XPATHErrorResources";

  /** Field ERROR_STRING          */
  public static final String ERROR_STRING = "#error";

  /** Field ERROR_HEADER          */
  public static final String ERROR_HEADER = "Error: ";

  /** Field WARNING_HEADER          */
  public static final String WARNING_HEADER = "Av\u00eds: ";

  /** Field XSL_HEADER          */
  public static final String XSL_HEADER = "XSL ";

  /** Field XML_HEADER          */
  public static final String XML_HEADER = "XML ";

  /** Field QUERY_HEADER          */
  public static final String QUERY_HEADER = "PATTERN ";


  /**
   * Return a named ResourceBundle for a particular locale.  This method mimics the behavior
   * of ResourceBundle.getBundle().
   *
   * @param className Name of local-specific subclass.
   * @return the ResourceBundle
   * @throws MissingResourceException
   */
  public static final XPATHErrorResources loadResourceBundle(String className)
          throws MissingResourceException
  {

    Locale locale = Locale.getDefault();
    String suffix = getResourceSuffix(locale);

    try
    {

      // first try with the given locale
      return (XPATHErrorResources) ResourceBundle.getBundle(className
              + suffix, locale);
    }
    catch (MissingResourceException e)
    {
      try  // try to fall back to en_US if we can't load
      {

        // Since we can't find the localized property file,
        // fall back to en_US.
        return (XPATHErrorResources) ResourceBundle.getBundle(className,
                new Locale("ca", "ES"));
      }
      catch (MissingResourceException e2)
      {

        // Now we are really in trouble.
        // very bad, definitely very bad...not going to get very far
        throw new MissingResourceException(
          "Could not load any resource bundles.", className, "");
      }
    }
  }

  /**
   * Return the resource file suffic for the indicated locale
   * For most locales, this will be based the language code.  However
   * for Chinese, we do distinguish between Taiwan and PRC
   *
   * @param locale the locale
   * @return an String suffix which canbe appended to a resource name
   */
  private static final String getResourceSuffix(Locale locale)
  {

    String suffix = "_" + locale.getLanguage();
    String country = locale.getCountry();

    if (country.equals("TW"))
      suffix += "_" + country;

    return suffix;
  }

}
