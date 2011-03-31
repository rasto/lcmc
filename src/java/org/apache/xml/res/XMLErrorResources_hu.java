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
 * $Id: XMLErrorResources_hu.java,v 1.3 2004/12/15 17:35:50 jycli Exp $
 */
package org.apache.xml.res;


import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Set up error messages.
 * We build a two dimensional array of message keys and
 * message strings. In order to add a new message here,
 * you need to first add a String constant. And you need
 * to enter key, value pair as part of the contents
 * array. You also need to update MAX_CODE for error strings
 * and MAX_WARNING for warnings ( Needed for only information
 * purpose )
 */
public class XMLErrorResources_hu extends ListResourceBundle
{

/*
 * This file contains error and warning messages related to Xalan Error
 * Handling.
 *
 *  General notes to translators:
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
 */

  /** Maximum error messages, this is needed to keep track of the number of messages.    */
  public static final int MAX_CODE = 61;

  /** Maximum warnings, this is needed to keep track of the number of warnings.          */
  public static final int MAX_WARNING = 0;

  /** Maximum misc strings.   */
  public static final int MAX_OTHERS = 4;

  /** Maximum total warnings and error messages.          */
  public static final int MAX_MESSAGES = MAX_CODE + MAX_WARNING + 1;


  /*
   * Message keys
   */
  public static final String ER_FUNCTION_NOT_SUPPORTED = "ER_FUNCTION_NOT_SUPPORTED";
  public static final String ER_CANNOT_OVERWRITE_CAUSE = "ER_CANNOT_OVERWRITE_CAUSE";
  public static final String ER_NO_DEFAULT_IMPL = "ER_NO_DEFAULT_IMPL";
  public static final String ER_CHUNKEDINTARRAY_NOT_SUPPORTED = "ER_CHUNKEDINTARRAY_NOT_SUPPORTED";
  public static final String ER_OFFSET_BIGGER_THAN_SLOT = "ER_OFFSET_BIGGER_THAN_SLOT";
  public static final String ER_COROUTINE_NOT_AVAIL = "ER_COROUTINE_NOT_AVAIL";
  public static final String ER_COROUTINE_CO_EXIT = "ER_COROUTINE_CO_EXIT";
  public static final String ER_COJOINROUTINESET_FAILED = "ER_COJOINROUTINESET_FAILED";
  public static final String ER_COROUTINE_PARAM = "ER_COROUTINE_PARAM";
  public static final String ER_PARSER_DOTERMINATE_ANSWERS = "ER_PARSER_DOTERMINATE_ANSWERS";
  public static final String ER_NO_PARSE_CALL_WHILE_PARSING = "ER_NO_PARSE_CALL_WHILE_PARSING";
  public static final String ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED = "ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED";
  public static final String ER_ITERATOR_AXIS_NOT_IMPLEMENTED = "ER_ITERATOR_AXIS_NOT_IMPLEMENTED";
  public static final String ER_ITERATOR_CLONE_NOT_SUPPORTED = "ER_ITERATOR_CLONE_NOT_SUPPORTED";
  public static final String ER_UNKNOWN_AXIS_TYPE = "ER_UNKNOWN_AXIS_TYPE";
  public static final String ER_AXIS_NOT_SUPPORTED = "ER_AXIS_NOT_SUPPORTED";
  public static final String ER_NO_DTMIDS_AVAIL = "ER_NO_DTMIDS_AVAIL";
  public static final String ER_NOT_SUPPORTED = "ER_NOT_SUPPORTED";
  public static final String ER_NODE_NON_NULL = "ER_NODE_NON_NULL";
  public static final String ER_COULD_NOT_RESOLVE_NODE = "ER_COULD_NOT_RESOLVE_NODE";
  public static final String ER_STARTPARSE_WHILE_PARSING = "ER_STARTPARSE_WHILE_PARSING";
  public static final String ER_STARTPARSE_NEEDS_SAXPARSER = "ER_STARTPARSE_NEEDS_SAXPARSER";
  public static final String ER_COULD_NOT_INIT_PARSER = "ER_COULD_NOT_INIT_PARSER";
  public static final String ER_EXCEPTION_CREATING_POOL = "ER_EXCEPTION_CREATING_POOL";
  public static final String ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE = "ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE";
  public static final String ER_SCHEME_REQUIRED = "ER_SCHEME_REQUIRED";
  public static final String ER_NO_SCHEME_IN_URI = "ER_NO_SCHEME_IN_URI";
  public static final String ER_NO_SCHEME_INURI = "ER_NO_SCHEME_INURI";
  public static final String ER_PATH_INVALID_CHAR = "ER_PATH_INVALID_CHAR";
  public static final String ER_SCHEME_FROM_NULL_STRING = "ER_SCHEME_FROM_NULL_STRING";
  public static final String ER_SCHEME_NOT_CONFORMANT = "ER_SCHEME_NOT_CONFORMANT";
  public static final String ER_HOST_ADDRESS_NOT_WELLFORMED = "ER_HOST_ADDRESS_NOT_WELLFORMED";
  public static final String ER_PORT_WHEN_HOST_NULL = "ER_PORT_WHEN_HOST_NULL";
  public static final String ER_INVALID_PORT = "ER_INVALID_PORT";
  public static final String ER_FRAG_FOR_GENERIC_URI ="ER_FRAG_FOR_GENERIC_URI";
  public static final String ER_FRAG_WHEN_PATH_NULL = "ER_FRAG_WHEN_PATH_NULL";
  public static final String ER_FRAG_INVALID_CHAR = "ER_FRAG_INVALID_CHAR";
  public static final String ER_PARSER_IN_USE = "ER_PARSER_IN_USE";
  public static final String ER_CANNOT_CHANGE_WHILE_PARSING = "ER_CANNOT_CHANGE_WHILE_PARSING";
  public static final String ER_SELF_CAUSATION_NOT_PERMITTED = "ER_SELF_CAUSATION_NOT_PERMITTED";
  public static final String ER_NO_USERINFO_IF_NO_HOST = "ER_NO_USERINFO_IF_NO_HOST";
  public static final String ER_NO_PORT_IF_NO_HOST = "ER_NO_PORT_IF_NO_HOST";
  public static final String ER_NO_QUERY_STRING_IN_PATH = "ER_NO_QUERY_STRING_IN_PATH";
  public static final String ER_NO_FRAGMENT_STRING_IN_PATH = "ER_NO_FRAGMENT_STRING_IN_PATH";
  public static final String ER_CANNOT_INIT_URI_EMPTY_PARMS = "ER_CANNOT_INIT_URI_EMPTY_PARMS";
  public static final String ER_METHOD_NOT_SUPPORTED ="ER_METHOD_NOT_SUPPORTED";
  public static final String ER_INCRSAXSRCFILTER_NOT_RESTARTABLE = "ER_INCRSAXSRCFILTER_NOT_RESTARTABLE";
  public static final String ER_XMLRDR_NOT_BEFORE_STARTPARSE = "ER_XMLRDR_NOT_BEFORE_STARTPARSE";
  public static final String ER_AXIS_TRAVERSER_NOT_SUPPORTED = "ER_AXIS_TRAVERSER_NOT_SUPPORTED";
  public static final String ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER = "ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER";
  public static final String ER_SYSTEMID_UNKNOWN = "ER_SYSTEMID_UNKNOWN";
  public static final String ER_LOCATION_UNKNOWN = "ER_LOCATION_UNKNOWN";
  public static final String ER_PREFIX_MUST_RESOLVE = "ER_PREFIX_MUST_RESOLVE";
  public static final String ER_CREATEDOCUMENT_NOT_SUPPORTED = "ER_CREATEDOCUMENT_NOT_SUPPORTED";
  public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT = "ER_CHILD_HAS_NO_OWNER_DOCUMENT";
  public static final String ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT = "ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT";
  public static final String ER_CANT_OUTPUT_TEXT_BEFORE_DOC = "ER_CANT_OUTPUT_TEXT_BEFORE_DOC";
  public static final String ER_CANT_HAVE_MORE_THAN_ONE_ROOT = "ER_CANT_HAVE_MORE_THAN_ONE_ROOT";
  public static final String ER_ARG_LOCALNAME_NULL = "ER_ARG_LOCALNAME_NULL";
  public static final String ER_ARG_LOCALNAME_INVALID = "ER_ARG_LOCALNAME_INVALID";
  public static final String ER_ARG_PREFIX_INVALID = "ER_ARG_PREFIX_INVALID";

  // Message keys used by the serializer
  public static final String ER_RESOURCE_COULD_NOT_FIND = "ER_RESOURCE_COULD_NOT_FIND";
  public static final String ER_RESOURCE_COULD_NOT_LOAD = "ER_RESOURCE_COULD_NOT_LOAD";
  public static final String ER_BUFFER_SIZE_LESSTHAN_ZERO = "ER_BUFFER_SIZE_LESSTHAN_ZERO";
  public static final String ER_INVALID_UTF16_SURROGATE = "ER_INVALID_UTF16_SURROGATE";
  public static final String ER_OIERROR = "ER_OIERROR";
  public static final String ER_NAMESPACE_PREFIX = "ER_NAMESPACE_PREFIX";
  public static final String ER_STRAY_ATTRIBUTE = "ER_STRAY_ATTIRBUTE";
  public static final String ER_STRAY_NAMESPACE = "ER_STRAY_NAMESPACE";
  public static final String ER_COULD_NOT_LOAD_RESOURCE = "ER_COULD_NOT_LOAD_RESOURCE";
  public static final String ER_COULD_NOT_LOAD_METHOD_PROPERTY = "ER_COULD_NOT_LOAD_METHOD_PROPERTY";
  public static final String ER_SERIALIZER_NOT_CONTENTHANDLER = "ER_SERIALIZER_NOT_CONTENTHANDLER";
  public static final String ER_ILLEGAL_ATTRIBUTE_POSITION = "ER_ILLEGAL_ATTRIBUTE_POSITION";

  /*
   * Now fill in the message text.
   * Then fill in the message text for that message code in the
   * array. Use the new error code as the index into the array.
   */

  // Error messages...

  /**
   * Get the lookup table for error messages
   *
   * @return The association list.
   */
  public Object[][] getContents()
  {
    return new Object[][] {

  /** Error message ID that has a null message, but takes in a single object.    */
    {"ER0000" , "{0}" },

    { ER_FUNCTION_NOT_SUPPORTED,
      "A f\u00fcggv\u00e9ny nem t\u00e1mogatott!"},

    { ER_CANNOT_OVERWRITE_CAUSE,
      "Nem lehet fel\u00fcl\u00edrni az okot"},

    { ER_NO_DEFAULT_IMPL,
      "Nem tal\u00e1ltunk alap\u00e9rtelmezett megval\u00f3s\u00edt\u00e1st "},

    { ER_CHUNKEDINTARRAY_NOT_SUPPORTED,
      "A ChunkedIntArray({0}) jelenleg nem t\u00e1mogatott"},

    { ER_OFFSET_BIGGER_THAN_SLOT,
      "Az eltol\u00e1s nagyobb mint a ny\u00edl\u00e1s"},

    { ER_COROUTINE_NOT_AVAIL,
      "T\u00e1rs-szubrutin nem \u00e9rhet\u0151 el, id={0}"},

    { ER_COROUTINE_CO_EXIT,
      "CoroutineManager \u00e9rkezett a co_exit() k\u00e9r\u00e9sre"},

    { ER_COJOINROUTINESET_FAILED,
      "A co_joinCoroutineSet() nem siker\u00fclt"},

    { ER_COROUTINE_PARAM,
      "T\u00e1rs-szubrutin param\u00e9ter hiba ({0})"},

    { ER_PARSER_DOTERMINATE_ANSWERS,
      "\nV\u00c1RATLAN: elemz\u0151 doTerminate v\u00e1laszok {0}"},

    { ER_NO_PARSE_CALL_WHILE_PARSING,
      "A parse-t nem h\u00edvhatja meg a elemz\u00e9s k\u00f6zben"},

    { ER_TYPED_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Hiba: A t\u00edpusos iter\u00e1tor a(z) {0} tengelyhez nincs megval\u00f3s\u00edtva"},

    { ER_ITERATOR_AXIS_NOT_IMPLEMENTED,
      "Hiba: Az iter\u00e1tor a(z) {0} tengelyhez nincs megval\u00f3s\u00edtva "},

    { ER_ITERATOR_CLONE_NOT_SUPPORTED,
      "Az iter\u00e1tor kl\u00f3nok nem t\u00e1mogatottak"},

    { ER_UNKNOWN_AXIS_TYPE,
      "Ismeretlen tengelytraverz\u00e1l-t\u00edpus: {0}"},

    { ER_AXIS_NOT_SUPPORTED,
      "Tengelytraverz\u00e1l nem t\u00e1mogatott: {0}"},

    { ER_NO_DTMIDS_AVAIL,
      "Nincs t\u00f6bb DTM ID"},

    { ER_NOT_SUPPORTED,
      "Nem t\u00e1mogatott: {0}"},

    { ER_NODE_NON_NULL,
      "A csom\u00f3pint nem-null kell legyen a getDTMHandleFromNode-hoz"},

    { ER_COULD_NOT_RESOLVE_NODE,
      "Nem lehet a csom\u00f3pontot hivatkoz\u00e1sra feloldani"},

    { ER_STARTPARSE_WHILE_PARSING,
       "A startParse-t nem h\u00edvhatja elemz\u00e9s k\u00f6zben"},

    { ER_STARTPARSE_NEEDS_SAXPARSER,
       "A startParse-nak nem-null SAXParser kell"},

    { ER_COULD_NOT_INIT_PARSER,
       "Nem lehet inicializ\u00e1lni az elemz\u0151t ezzel"},

    { ER_EXCEPTION_CREATING_POOL,
       "kiv\u00e9tel egy \u00faj pool p\u00e9ld\u00e1ny l\u00e9trehoz\u00e1s\u00e1n\u00e1l"},

    { ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
       "Az el\u00e9r\u00e9i \u00fat \u00e9rv\u00e9nytelen eszk\u00e9pszekvenci\u00e1t tartalmaz"},

    { ER_SCHEME_REQUIRED,
       "S\u00e9m\u00e1ra sz\u00fcks\u00e9g van!"},

    { ER_NO_SCHEME_IN_URI,
       "Nem tal\u00e1lhat\u00f3 s\u00e9ma az URI-ban: {0}"},

    { ER_NO_SCHEME_INURI,
       "Nem tal\u00e1lhat\u00f3 s\u00e9ma az URI-ban"},

    { ER_PATH_INVALID_CHAR,
       "Az el\u00e9r\u00e9si \u00fat \u00e9rv\u00e9nytelen karaktert tartalmaz: {0}"},

    { ER_SCHEME_FROM_NULL_STRING,
       "Nem lehet be\u00e1ll\u00edtani a s\u00e9m\u00e1t null karakterl\u00e1ncb\u00f3l"},

    { ER_SCHEME_NOT_CONFORMANT,
       "A s\u00e9ma nem megfelel\u0151."},

    { ER_HOST_ADDRESS_NOT_WELLFORMED,
       "A host nem j\u00f3l form\u00e1zott c\u00edm"},

    { ER_PORT_WHEN_HOST_NULL,
       "A port-t nem \u00e1ll\u00edthatja be, ha a host null"},

    { ER_INVALID_PORT,
       "\u00c9rv\u00e9nytelen portsz\u00e1m"},

    { ER_FRAG_FOR_GENERIC_URI,
       "Darabot csak egy \u00e1ltal\u00e1nos URI-hoz \u00e1ll\u00edthat be"},

    { ER_FRAG_WHEN_PATH_NULL,
       "A darabot csak nem \u00e1ll\u00edthatja be, h\u00ed az el\u00e9r\u00e9si \u00fat null"},

    { ER_FRAG_INVALID_CHAR,
       "A darab \u00e9rv\u00e9nytelen karaktert tartalmaz"},

    { ER_PARSER_IN_USE,
      "Az elemz\u0151 m\u00e1r haszn\u00e1latban van"},

    { ER_CANNOT_CHANGE_WHILE_PARSING,
      "Nem v\u00e1ltoztathat\u00f3 meg a(z) {0} {1} elemz\u00e9s k\u00f6zben"},

    { ER_SELF_CAUSATION_NOT_PERMITTED,
      "Az \u00f6n-megokol\u00e1s nem megengedett"},

    { ER_NO_USERINFO_IF_NO_HOST,
      "Nem adhat meg userinfo-t, ha nem adott meg host-ot"},

    { ER_NO_PORT_IF_NO_HOST,
      "Nem adhat meg port-ot, ha nem adott meg host-ot"},

    { ER_NO_QUERY_STRING_IN_PATH,
      "Lek\u00e9rdez\u00e9si karakterl\u00e1ncot nem adhat meg el\u00e9r\u00e9si \u00fatban \u00e9s lek\u00e9rdez\u00e9si karakterl\u00e1ncban"},

    { ER_NO_FRAGMENT_STRING_IN_PATH,
      "Darabot nem adhat meg sem az el\u00e9r\u00e9si \u00fatban sem a darabban"},

    { ER_CANNOT_INIT_URI_EMPTY_PARMS,
      "Nem inicializ\u00e1lhatja az URI-t \u00fcres param\u00e9terekkel"},

    { ER_METHOD_NOT_SUPPORTED,
      "A met\u00f3dus m\u00e9g nem t\u00e1mogatott "},

    { ER_INCRSAXSRCFILTER_NOT_RESTARTABLE,
      "Az IncrementalSAXSource_Filter jelenleg nem \u00ednd\u00edthat\u00f3 \u00fajra"},

    { ER_XMLRDR_NOT_BEFORE_STARTPARSE,
      "XMLReader nem a startParse k\u00e9r\u00e9s el\u0151tt"},

    { ER_AXIS_TRAVERSER_NOT_SUPPORTED,
      "Tengelytraverz\u00e1l nem t\u00e1mogatott: {0}"},

    { ER_ERRORHANDLER_CREATED_WITH_NULL_PRINTWRITER,
      "A ListingErrorHandler l\u00e9trej\u00f6tt null PrintWriter-rel!"},

    { ER_SYSTEMID_UNKNOWN,
      "Ismeretlen SystemId"},

    { ER_LOCATION_UNKNOWN,
      "A hiba helye ismeretlen"},

    { ER_PREFIX_MUST_RESOLVE,
      "Az el\u0151tagnak egy n\u00e9vt\u00e9rre kell felold\u00f3dnia: {0}"},

    { ER_CREATEDOCUMENT_NOT_SUPPORTED,
      "A createDocument() nem t\u00e1mogatott az XPathContext-ben!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT,
      "Az attrib\u00fatum lesz\u00e1rmazottnak nincs tulajdonos dokumentuma!"},

    { ER_CHILD_HAS_NO_OWNER_DOCUMENT_ELEMENT,
      "Az attrib\u00fatum lesz\u00e1rmazottnak nincs tulajdonos dokumentum eleme!"},

    { ER_CANT_OUTPUT_TEXT_BEFORE_DOC,
      "Figyelmeztet\u00e9s: nem lehet sz\u00f6veget ki\u00edrni dokumentum elem el\u0151tt!  Figyelmen k\u00edv\u00fcl marad..."},

    { ER_CANT_HAVE_MORE_THAN_ONE_ROOT,
      "Nem lehet egyn\u00e9l t\u00f6bb gy\u00f6k\u00e9r a DOM-on!"},

    { ER_ARG_LOCALNAME_NULL,
       "A 'localName' argumentum null"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The localname is the portion after the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_LOCALNAME_INVALID,
       "A QNAME-beli helyi n\u00e9vnek egy \u00e9rv\u00e9nyes NCName-nek kell lenni"},

    // Note to translators:  A QNAME has the syntactic form [NCName:]NCName
    // The prefix is the portion before the optional colon; the message indicates
    // that there is a problem with that part of the QNAME.
    { ER_ARG_PREFIX_INVALID,
       "A QNAME-beli prefixnek egy \u00e9rv\u00e9nyes NCName-nek kell lenni"},

    { "BAD_CODE", "A createMessage param\u00e9tere nincs a megfelel\u0151 tartom\u00e1nyban"},
    { "FORMAT_FAILED", "Kiv\u00e9tel t\u00f6rt\u00e9nt a messageFormat h\u00edv\u00e1s alatt"},
    { "line", "Sor #"},
    { "column","Oszlop #"},

    {ER_SERIALIZER_NOT_CONTENTHANDLER,
      "A(z) ''{0}'' serializer oszt\u00e1ly nem val\u00f3s\u00edtja meg az org.xml.sax.ContentHandler funkci\u00f3t."},

    {ER_RESOURCE_COULD_NOT_FIND,
      "A(z) [ {0} ] er\u0151forr\u00e1s nem tal\u00e1lhat\u00f3.\n {1}" },

    {ER_RESOURCE_COULD_NOT_LOAD,
      "Az er\u0151forr\u00e1st [ {0} ] nem lehet bet\u00f6lteni: {1} \n {2} \t {3}" },

    {ER_BUFFER_SIZE_LESSTHAN_ZERO,
      "Pufferm\u00e9ret <= 0" },

    {ER_INVALID_UTF16_SURROGATE,
      "\u00c9rv\u00e9nytelen UTF-16 helyettes\u00edt\u00e9s: {0} ?" },

    {ER_OIERROR,
      "IO hiba" },

    {ER_ILLEGAL_ATTRIBUTE_POSITION,
      "Nem lehet {0} attrib\u00fatumat felvenni a gyermek node-ok ut\u00e1n vagy miel\u0151tt egy elem l\u00e9trej\u00f6nne.  Az attrib\u00fatum figyelmen k\u00edv\u00fcl marad."},

      /*
       * Note to translators:  The stylesheet contained a reference to a
       * namespace prefix that was undefined.  The value of the substitution
       * text is the name of the prefix.
       */
    {ER_NAMESPACE_PREFIX,
      "A(z) ''{0}'' el\u0151tag n\u00e9vtere nem defini\u00e1lt." },
      /*
       * Note to translators:  This message is reported if the stylesheet
       * being processed attempted to construct an XML document with an
       * attribute in a place other than on an element.  The substitution text
       * specifies the name of the attribute.
       */
    {ER_STRAY_ATTRIBUTE,
      "A(z) ''{0}'' attrib\u00fatum k\u00edv\u00fcl esik az elemen." },

      /*
       * Note to translators:  As with the preceding message, a namespace
       * declaration has the form of an attribute and is only permitted to
       * appear on an element.  The substitution text {0} is the namespace
       * prefix and {1} is the URI that was being used in the erroneous
       * namespace declaration.
       */
    {ER_STRAY_NAMESPACE,
      "A(z) ''{0}''=''{1}'' n\u00e9vt\u00e9r-deklar\u00e1ci\u00f3 k\u00edv\u00fcl esik az elemen." },

    {ER_COULD_NOT_LOAD_RESOURCE,
      "Nem lehet bet\u00f6lteni ''{0}''-t (ellen\u0151rizze a CLASSPATH be\u00e1ll\u00edt\u00e1st), az alap\u00e9rtelmez\u00e9seket haszn\u00e1lom"},

    {ER_COULD_NOT_LOAD_METHOD_PROPERTY,
      "Nem lehet bet\u00f6lteni a(z) ''{0}'' tulajdons\u00e1g-f\u00e1jlt a(z) ''{1}''  (ellen\u0151rizze a CLASSPATH be\u00e1ll\u00edt\u00e1st)" }


  };
  }

  /**
   *   Return a named ResourceBundle for a particular locale.  This method mimics the behavior
   *   of ResourceBundle.getBundle().
   *
   *   @param className the name of the class that implements the resource bundle.
   *   @return the ResourceBundle
   *   @throws MissingResourceException
   */
  public static final XMLErrorResources loadResourceBundle(String className)
          throws MissingResourceException
  {

    Locale locale = Locale.getDefault();
    String suffix = getResourceSuffix(locale);

    try
    {

      // first try with the given locale
      return (XMLErrorResources) ResourceBundle.getBundle(className
              + suffix, locale);
    }
    catch (MissingResourceException e)
    {
      try  // try to fall back to en_US if we can't load
      {

        // Since we can't find the localized property file,
        // fall back to en_US.
        return (XMLErrorResources) ResourceBundle.getBundle(className,
                new Locale("hu", "HU"));
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
