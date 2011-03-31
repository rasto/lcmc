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
 * $Id: ErrorMessages_hu.java,v 1.3 2004/12/15 17:35:41 jycli Exp $
 */

package org.apache.xalan.xsltc.compiler.util;

import java.util.ListResourceBundle;

/**
 * @author Morten Jorgensen
 */
public class ErrorMessages_hu extends ListResourceBundle {

/*
 * XSLTC compile-time error messages.
 *
 * General notes to translators and definitions:
 *
 *   1) XSLTC is the name of the product.  It is an acronym for "XSLT Compiler".
 *      XSLT is an acronym for "XML Stylesheet Language: Transformations".
 *
 *   2) A stylesheet is a description of how to transform an input XML document
 *      into a resultant XML document (or HTML document or text).  The
 *      stylesheet itself is described in the form of an XML document.
 *
 *   3) A template is a component of a stylesheet that is used to match a
 *      particular portion of an input document and specifies the form of the
 *      corresponding portion of the output document.
 *
 *   4) An axis is a particular "dimension" in a tree representation of an XML
 *      document; the nodes in the tree are divided along different axes.
 *      Traversing the "child" axis, for instance, means that the program
 *      would visit each child of a particular node; traversing the "descendant"
 *      axis means that the program would visit the child nodes of a particular
 *      node, their children, and so on until the leaf nodes of the tree are
 *      reached.
 *
 *   5) An iterator is an object that traverses nodes in a tree along a
 *      particular axis, one at a time.
 *
 *   6) An element is a mark-up tag in an XML document; an attribute is a
 *      modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
 *      "elem" is an element name, "attr" and "attr2" are attribute names with
 *      the values "val" and "val2", respectively.
 *
 *   7) A namespace declaration is a special attribute that is used to associate
 *      a prefix with a URI (the namespace).  The meanings of element names and
 *      attribute names that use that prefix are defined with respect to that
 *      namespace.
 *
 *   8) DOM is an acronym for Document Object Model.  It is a tree
 *      representation of an XML document.
 *
 *      SAX is an acronym for the Simple API for XML processing.  It is an API
 *      used inform an XML processor (in this case XSLTC) of the structure and
 *      content of an XML document.
 *
 *      Input to the stylesheet processor can come from an XML parser in the
 *      form of a DOM tree or through the SAX API.
 *
 *   9) DTD is a document type declaration.  It is a way of specifying the
 *      grammar for an XML file, the names and types of elements, attributes,
 *      etc.
 *
 *  10) XPath is a specification that describes a notation for identifying
 *      nodes in a tree-structured representation of an XML document.  An
 *      instance of that notation is referred to as an XPath expression.
 *
 *  11) Translet is an invented term that refers to the class file that contains
 *      the compiled form of a stylesheet.
 */

    // These message should be read from a locale-specific resource bundle
    /** Get the lookup table for error messages.   
     *
     * @return The message lookup table.
     */
    public Object[][] getContents()
    {
      return new Object[][] {
        {ErrorMsg.MULTIPLE_STYLESHEET_ERR,
        "Egyn\u00e9l t\u00f6bb st\u00edluslap van defini\u00e1lva ugyanabban a f\u00e1jlban."},

        /*
         * Note to translators:  The substitution text is the name of a
         * template.  The same name was used on two different templates in the
         * same stylesheet.
         */
        {ErrorMsg.TEMPLATE_REDEF_ERR,
        "A(z) ''{0}'' sablon m\u00e1r defini\u00e1lt ebben a st\u00edluslapban."},


        /*
         * Note to translators:  The substitution text is the name of a
         * template.  A reference to the template name was encountered, but the
         * template is undefined.
         */
        {ErrorMsg.TEMPLATE_UNDEF_ERR,
        "A(z) ''{0}'' sablon nem defini\u00e1lt ebben a st\u00edluslapban."},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * that was defined more than once.
         */
        {ErrorMsg.VARIABLE_REDEF_ERR,
        "A(z) ''{0}'' v\u00e1ltoz\u00f3 t\u00f6bbsz\u00f6r defini\u00e1lt ugyanabban a hat\u00f3k\u00f6rben."},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * or parameter.  A reference to the variable or parameter was found,
         * but it was never defined.
         */
        {ErrorMsg.VARIABLE_UNDEF_ERR,
        "A(z) ''{0}'' v\u00e1ltoz\u00f3 vagy param\u00e9ter nem defini\u00e1lt."},

        /*
         * Note to translators:  The word "class" here refers to a Java class.
         * Processing the stylesheet required a class to be loaded, but it could
         * not be found.  The substitution text is the name of the class.
         */
        {ErrorMsg.CLASS_NOT_FOUND_ERR,
        "Nem tal\u00e1lhat\u00f3 a(z) ''{0}'' oszt\u00e1ly."},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but it could not be found.  "public" is the
         * Java keyword.
         */
        {ErrorMsg.METHOD_NOT_FOUND_ERR,
        "Nem tal\u00e1lhat\u00f3 a(z) ''{0}'' k\u00fcls\u0151 met\u00f3dus (public-nak kellene lenni)."},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but no method with the required types of
         * arguments or return type could be found.
         */
        {ErrorMsg.ARGUMENT_CONVERSION_ERR,
        "Nem lehet konvert\u00e1lni az argumentum/visszat\u00e9r\u00e9si k\u00f3d t\u00edpus\u00e1t a(z) ''{0}'' met\u00f3dus h\u00edv\u00e1s\u00e1ban."},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * is missing.
         */
        {ErrorMsg.FILE_NOT_FOUND_ERR,
        "A(z) ''{0}'' f\u00e1jl vagy URI nem tal\u00e1lhat\u00f3."},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.INVALID_URI_ERR,
        "\u00c9rv\u00e9nytelen URI: ''{0}''."},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * exists but could not be opened.
         */
        {ErrorMsg.FILE_ACCESS_ERR,
        "Nem lehet megnyitni a(z) ''{0}'' f\u00e1jlt vagy URI-t."},

        /*
         * Note to translators: <xsl:stylesheet> and <xsl:transform> are
         * keywords that should not be translated.
         */
        {ErrorMsg.MISSING_ROOT_ERR,
        "Hi\u00e1nyzik az <xsl:stylesheet> vagy <xsl:transform> elem."},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {ErrorMsg.NAMESPACE_UNDEF_ERR,
        "A(z) ''{0}'' n\u00e9vt\u00e9r-prefix nincs deklar\u00e1lva."},

        /*
         * Note to translators:  The Java function named in the stylesheet could
         * not be found.
         */
        {ErrorMsg.FUNCTION_RESOLVE_ERR,
        "Nem lehet feloldani a(z) ''{0}'' f\u00fcggv\u00e9ny h\u00edv\u00e1s\u00e1t."},

        /*
         * Note to translators:  The substitution text is the name of a
         * function.  A literal string here means a constant string value.
         */
        {ErrorMsg.NEED_LITERAL_ERR,
        "A(z) ''{0}'' argumentuma egy liter\u00e1l kell legyen."},

        /*
         * Note to translators:  This message indicates there was a syntactic
         * error in the form of an XPath expression.  The substitution text is
         * the expression.
         */
        {ErrorMsg.XPATH_PARSER_ERR,
        "Hiba t\u00f6rt\u00e9nt a(z) ''{0}'' XPath kifejez\u00e9s elemz\u00e9sekor."},

        /*
         * Note to translators:  An element in the stylesheet requires a
         * particular attribute named by the substitution text, but that
         * attribute was not specified in the stylesheet.
         */
        {ErrorMsg.REQUIRED_ATTR_ERR,
        "Hi\u00e1nyzik a(z) ''{0}'' k\u00f6telez\u0151 attrib\u00fatum."},

        /*
         * Note to translators:  This message indicates that a character not
         * permitted in an XPath expression was encountered.  The substitution
         * text is the offending character.
         */
        {ErrorMsg.ILLEGAL_CHAR_ERR,
        "Nem megengedett karakter (''{0}'') szerepel az XPath kifejez\u00e9sben."},

        /*
         * Note to translators:  A processing instruction is a mark-up item in
         * an XML document that request some behaviour of an XML processor.  The
         * form of the name of was invalid in this case, and the substitution
         * text is the name.
         */
        {ErrorMsg.ILLEGAL_PI_ERR,
        "Nem megengedett n\u00e9v (''{0}'') szerepelt a feldolgoz\u00e1si utas\u00edt\u00e1sokban."},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {ErrorMsg.STRAY_ATTRIBUTE_ERR,
        "A(z) ''{0}'' attrib\u00fatum k\u00edv\u00fcl esik az elemen."},

        /*
         * Note to translators:  An attribute that wasn't recognized was
         * specified on an element in the stylesheet.  The attribute is named
         * by the substitution
         * text.
         */
        {ErrorMsg.ILLEGAL_ATTRIBUTE_ERR,
        "Nem megengedett attrib\u00fatum: ''{0}''."},

        /*
         * Note to translators:  "import" and "include" are keywords that should
         * not be translated.  This messages indicates that the stylesheet
         * named in the substitution text imported or included itself either
         * directly or indirectly.
         */
        {ErrorMsg.CIRCULAR_INCLUDE_ERR,
        "K\u00f6rk\u00f6r\u00f6s import/include. A(z) ''{0}'' st\u00edluslap m\u00e1r be van t\u00f6ltve."},

        /*
         * Note to translators:  A result-tree fragment is a portion of a
         * resulting XML document represented as a tree.  "<xsl:sort>" is a
         * keyword and should not be translated.
         */
        {ErrorMsg.RESULT_TREE_SORT_ERR,
        "Az eredm\u00e9nyfa-r\u00e9szleteket nem lehet rendezni (az <xsl:sort> elemek figyelmen k\u00edv\u00fcl maradnak). Rendeznie kell a node-okat, amikor eredm\u00e9nyf\u00e1t hoz l\u00e9tre."},

        /*
         * Note to translators:  A name can be given to a particular style to be
         * used to format decimal values.  The substitution text gives the name
         * of such a style for which more than one declaration was encountered.
         */
        {ErrorMsg.SYMBOLS_REDEF_ERR,
        "M\u00e1r defini\u00e1lva van a(z) ''{0}'' decim\u00e1lis form\u00e1z\u00e1s."},

        /*
         * Note to translators:  The stylesheet version named in the
         * substitution text is not supported.
         */
        {ErrorMsg.XSL_VERSION_ERR,
        "Az XSLTC nem t\u00e1mogatja a(z) ''{0}'' XSL verzi\u00f3t."},

        /*
         * Note to translators:  The definitions of one or more variables or
         * parameters depend on one another.
         */
        {ErrorMsg.CIRCULAR_VARIABLE_ERR,
        "K\u00f6rk\u00f6r\u00f6s v\u00e1ltoz\u00f3/param\u00e9ter-hivatkoz\u00e1s; helye: ''{0}''."},

        /*
         * Note to translators:  The operator in an expresion with two operands was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_BINARY_OP_ERR,
        "Ismeretlen oper\u00e1tort haszn\u00e1lt a bin\u00e1ris kifejez\u00e9sben."},

        /*
         * Note to translators:  This message is produced if a reference to a
         * function has too many or too few arguments.
         */
        {ErrorMsg.ILLEGAL_ARG_ERR,
        "Nem megengedett argumentumo(ka)t haszn\u00e1lt a f\u00fcggv\u00e9nyh\u00edv\u00e1sban."},

        /*
         * Note to translators:  "document()" is the name of function and must
         * not be translated.  A node-set is a set of the nodes in the tree
         * representation of an XML document.
         */
        {ErrorMsg.DOCUMENT_ARG_ERR,
        "A document() f\u00fcggv\u00e9ny m\u00e1sodik argumentuma egy node-k\u00e9szlet kell legyen."},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.MISSING_WHEN_ERR,
        "Legal\u00e1bb egy <xsl:when> elem sz\u00fcks\u00e9ges az <xsl:choose>-ban."},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.MULTIPLE_OTHERWISE_ERR,
        "Csak egy <xsl:otherwise> elem megengedett <xsl:choose>-ban."},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.STRAY_OTHERWISE_ERR,
        "Az <xsl:otherwise> csak <xsl:choose>-on bel\u00fcl haszn\u00e1lhat\u00f3."},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.STRAY_WHEN_ERR,
        "Az <xsl:when> csak <xsl:choose>-on bel\u00fcl haszn\u00e1lhat\u00f3."},

        /*
         * Note to translators:  "<xsl:when>", "<xsl:otherwise>" and
         * "<xsl:choose>" are keywords and should not be translated.  This
         * message describes a syntax error in the stylesheet.
         */
        {ErrorMsg.WHEN_ELEMENT_ERR,
        "Csak <xsl:when> \u00e9s <xsl:otherwise> elemek megengedettek az <xsl:choose>-ban."},

        /*
         * Note to translators:  "<xsl:attribute-set>" and "name" are keywords
         * that should not be translated.
         */
        {ErrorMsg.UNNAMED_ATTRIBSET_ERR,
        "Hi\u00e1nyzik az <xsl:attribute-set>-b\u0151l a 'name' attrib\u00fatum."},

        /*
         * Note to translators:  An element in the stylesheet contained an
         * element of a type that it was not permitted to contain.
         */
        {ErrorMsg.ILLEGAL_CHILD_ERR,
        "Nem megengedett gyermek elem."},

        /*
         * Note to translators:  The stylesheet tried to create an element with
         * a name that was not a valid XML name.  The substitution text contains
         * the name.
         */
        {ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
        "Nem h\u00edvhat ''{0}''-nek elemet."},

        /*
         * Note to translators:  The stylesheet tried to create an attribute
         * with a name that was not a valid XML name.  The substitution text
         * contains the name.
         */
        {ErrorMsg.ILLEGAL_ATTR_NAME_ERR,
        "Nem h\u00edvhat ''{0}''-nek attrib\u00fatumot."},

        /*
         * Note to translators:  The children of the outermost element of a
         * stylesheet are referred to as top-level elements.  No text should
         * occur within that outermost element unless it is within a top-level
         * element.  This message indicates that that constraint was violated.
         * "<xsl:stylesheet>" is a keyword that should not be translated.
         */
        {ErrorMsg.ILLEGAL_TEXT_NODE_ERR,
        "Sz\u00f6vegadat szerepel a fels\u0151 szint\u0171 <xsl:stylesheet> elemen k\u00edv\u00fcl."},

        /*
         * Note to translators:  JAXP is an acronym for the Java API for XML
         * Processing.  This message indicates that the XML parser provided to
         * XSLTC to process the XML input document had a configuration problem.
         */
        {ErrorMsg.SAX_PARSER_CONFIG_ERR,
        "Nincs megfelel\u0151en konfigur\u00e1lva a JAXP elemz\u0151."},

        /*
         * Note to translators:  The substitution text names the internal error
         * encountered.
         */
        {ErrorMsg.INTERNAL_ERR,
        "Helyrehozhatatlan XSLTC-bels\u0151 hiba t\u00f6rt\u00e9nt: ''{0}''"},

        /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {ErrorMsg.UNSUPPORTED_XSL_ERR,
        "Nem t\u00e1mogatott XSL elem: ''{0}''."},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSTLC does
         * not recognized the particular extension named.  The substitution text
         * gives the extension name.
         */
        {ErrorMsg.UNSUPPORTED_EXT_ERR,
        "Ismeretlen XSLTC kiterjeszt\u00e9s: ''{0}''."},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.  XSLTC is able to detect that in this
         * case because the outermost element in the stylesheet has to be
         * declared with respect to the XSL namespace URI, but no declaration
         * for that namespace was seen.
         */
        {ErrorMsg.MISSING_XSLT_URI_ERR,
        "A bemen\u0151 dokumentum nem st\u00edluslap (az XSL n\u00e9vt\u00e9r nincs deklar\u00e1lva a root elemben)."},

        /*
         * Note to translators:  XSLTC could not find the stylesheet document
         * with the name specified by the substitution text.
         */
        {ErrorMsg.MISSING_XSLT_TARGET_ERR,
        "Nem tal\u00e1lhat\u00f3 a(z) ''{0}'' st\u00edluslap-c\u00e9lban."},

        /*
         * Note to translators:  This message represents an internal error in
         * condition in XSLTC.  The substitution text is the class name in XSLTC
         * that is missing some functionality.
         */
        {ErrorMsg.NOT_IMPLEMENTED_ERR,
        "Nincs megval\u00f3s\u00edtva: ''{0}''."},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.
         */
        {ErrorMsg.NOT_STYLESHEET_ERR,
        "A bemen\u0151 dokumentum nem tartalmaz XSL st\u00edluslapot."},

        /*
         * Note to translators:  The element named in the substitution text was
         * encountered in the stylesheet but is not recognized.
         */
        {ErrorMsg.ELEMENT_PARSE_ERR,
        "Nem lehet elemezni a(z) ''{0}'' elemet."},

        /*
         * Note to translators:  "use", "<key>", "node", "node-set", "string"
         * and "number" are keywords in this context and should not be
         * translated.  This message indicates that the value of the "use"
         * attribute was not one of the permitted values.
         */
        {ErrorMsg.KEY_USE_ATTR_ERR,
        "A(z) <key> attrib\u00fatuma node, node-k\u00e9szlet, sz\u00f6veg vagy sz\u00e1m lehet."},

        /*
         * Note to translators:  An XML document can specify the version of the
         * XML specification to which it adheres.  This message indicates that
         * the version specified for the output document was not valid.
         */
        {ErrorMsg.OUTPUT_VERSION_ERR,
        "A kimen\u0151 XML dokumentum-verzi\u00f3 1.0 kell legyen."},

        /*
         * Note to translators:  The operator in a comparison operation was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_RELAT_OP_ERR,
        "Ismeretlen oper\u00e1tort haszn\u00e1lt a rel\u00e1ci\u00f3s kifejez\u00e9sben."},

        /*
         * Note to translators:  An attribute set defines as a set of XML
         * attributes that can be added to an element in the output XML document
         * as a group.  This message is reported if the name specified was not
         * used to declare an attribute set.  The substitution text is the name
         * that is in error.
         */
        {ErrorMsg.ATTRIBSET_UNDEF_ERR,
        "Neml\u00e9tez\u0151 attrib\u00fatumk\u00e9szletet (''{0}'') pr\u00f3b\u00e1lt haszn\u00e1lni."},

        /*
         * Note to translators:  The term "attribute value template" is a term
         * defined by XSLT which describes the value of an attribute that is
         * determined by an XPath expression.  The message indicates that the
         * expression was syntactically incorrect; the substitution text
         * contains the expression that was in error.
         */
        {ErrorMsg.ATTR_VAL_TEMPLATE_ERR,
        "Nem lehet elemezni a(z) ''{0}'' attrib\u00fatum\u00e9rt\u00e9k-sablont."},

        /*
         * Note to translators:  ???
         */
        {ErrorMsg.UNKNOWN_SIG_TYPE_ERR,
        "Ismeretlen adatt\u00edpus szerepel a(z) ''{0}'' oszt\u00e1ly al\u00e1\u00edr\u00e1s\u00e1ban."},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of
         * type {0}.
         */
        {ErrorMsg.DATA_CONVERSION_ERR,
        "Nem lehet a(z) ''{0}'' adatt\u00edpust ''{1}'' t\u00edpusra konvert\u00e1lni."},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_TRANSLET_CLASS_ERR,
        "Ez a sablon nem tartalmaz \u00e9rv\u00e9nyes translet oszt\u00e1lydefin\u00edci\u00f3t."},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_MAIN_TRANSLET_ERR,
        "Ez a sablon nem tartalmaz ''{0}'' nev\u0171 oszt\u00e1lyt."},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSLET_CLASS_ERR,
        "Nem lehet bet\u00f6lteni a(z) ''{0}'' translet oszt\u00e1lyt."},

        {ErrorMsg.TRANSLET_OBJECT_ERR,
        "A translet oszt\u00e1ly bet\u00f6lt\u0151d\u00f6tt, de nem siker\u00fclt l\u00e9trehozni a translet p\u00e9ld\u00e1nyt."},

        /*
         * Note to translators:  "ErrorListener" is a Java interface name that
         * should not be translated.  The message indicates that the user tried
         * to set an ErrorListener object on object of the class named in the
         * substitution text with "null" Java value.
         */
        {ErrorMsg.ERROR_LISTENER_NULL_ERR,
        "Megpr\u00f3b\u00e1lta null-ra \u00e1ll\u00edtani ''{0}'' ErrorListener objektum\u00e1t."},

        /*
         * Note to translators:  StreamSource, SAXSource and DOMSource are Java
         * interface names that should not be translated.
         */
        {ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR,
        "Az XSLTC csak a StreamSource, SAXSource \u00e9s DOMSource interf\u00e9szeket t\u00e1mogatja."},

        /*
         * Note to translators:  "Source" is a Java class name that should not
         * be translated.  The substitution text is the name of Java method.
         */
        {ErrorMsg.JAXP_NO_SOURCE_ERR,
        "A(z) ''{0}'' met\u00f3dusnak \u00e1tadott source objektum nem tartalmaz semmit."},

        /*
         * Note to translators:  The message indicates that XSLTC failed to
         * compile the stylesheet into a translet (class file).
         */
        {ErrorMsg.JAXP_COMPILE_ERR,
        "Nem siker\u00fclt leford\u00edtani a st\u00edluslapot."},

        /*
         * Note to translators:  "TransformerFactory" is a class name.  In this
         * context, an attribute is a property or setting of the
         * TransformerFactory object.  The substitution text is the name of the
         * unrecognised attribute.  The method used to retrieve the attribute is
         * "getAttribute", so it's not clear whether it would be best to
         * translate the term "attribute".
         */
        {ErrorMsg.JAXP_INVALID_ATTR_ERR,
        "A TransformerFactory objektum nem ismer ''{0}'' attrib\u00fatumot."},

        /*
         * Note to translators:  "setResult()" and "startDocument()" are Java
         * method names that should not be translated.
         */
        {ErrorMsg.JAXP_SET_RESULT_ERR,
        "A setResult() met\u00f3dust a startDocument() h\u00edv\u00e1sa el\u0151tt kell megh\u00edvni."},

        /*
         * Note to translators:  "Transformer" is a Java interface name that
         * should not be translated.  A Transformer object should contained a
         * reference to a translet object in order to be used for
         * transformations; this message is produced if that requirement is not
         * met.
         */
        {ErrorMsg.JAXP_NO_TRANSLET_ERR,
        "A transformer interf\u00e9sz nem tartalmaz be\u00e1gyazott translet objektumot."},

        /*
         * Note to translators:  The XML document that results from a
         * transformation needs to be sent to an output handler object; this
         * message is produced if that requirement is not met.
         */
        {ErrorMsg.JAXP_NO_HANDLER_ERR,
        "Nincs defini\u00e1lva kimenetkezel\u0151 az \u00e1talak\u00edt\u00e1s eredm\u00e9ny\u00e9hez."},

        /*
         * Note to translators:  "Result" is a Java interface name in this
         * context.  The substitution text is a method name.
         */
        {ErrorMsg.JAXP_NO_RESULT_ERR,
        "A(z) ''{0}'' met\u00f3dusnak \u00e1tadott result objektum \u00e9rv\u00e9nytelen."},

        /*
         * Note to translators:  "Transformer" is a Java interface name.  The
         * user's program attempted to access an unrecognized property with the
         * name specified in the substitution text.  The method used to retrieve
         * the property is "getOutputProperty", so it's not clear whether it
         * would be best to translate the term "property".
         */
        {ErrorMsg.JAXP_UNKNOWN_PROP_ERR,
        "\u00c9rv\u00e9nytelen Transformer tulajdons\u00e1got (''{0}'') pr\u00f3b\u00e1lt meg el\u00e9rni."},

        /*
         * Note to translators:  SAX2DOM is the name of a Java class that should
         * not be translated.  This is an adapter in the sense that it takes a
         * DOM object and converts it to something that uses the SAX API.
         */
        {ErrorMsg.SAX2DOM_ADAPTER_ERR,
        "Nem lehet l\u00e9trehozni a SAX2DOM adaptert: ''{0}''."},

        /*
         * Note to translators:  "XSLTCSource.build()" is a Java method name.
         * "systemId" is an XML term that is short for "system identification".
         */
        {ErrorMsg.XSLTC_SOURCE_ERR,
        "XSLTCSource.build() h\u00edv\u00e1sa systemId be\u00e1ll\u00edt\u00e1sa n\u00e9lk\u00fcl t\u00f6rt\u00e9nt."},


        {ErrorMsg.COMPILE_STDIN_ERR,
        "A -i kapcsol\u00f3t a -o kapcsol\u00f3val egy\u00fctt kell haszn\u00e1lni."},


        /*
         * Note to translators:  This message contains usage information for a
         * means of invoking XSLTC from the command-line.  The message is
         * formatted for presentation in English.  The strings <output>,
         * <directory>, etc. indicate user-specified argument values, and can
         * be translated - the argument <package> refers to a Java package, so
         * it should be handled in the same way the term is handled for JDK
         * documentation.
         */
        {ErrorMsg.COMPILE_USAGE_STR,
        "Haszn\u00e1lat:\n   java org.apache.xalan.xsltc.cmdline.Compile [-o <kimenet>]\n      [-d <alk\u00f6nyvt\u00e1r>] [-j <jarf\u00e1jl>] [-p <csomag>]\n      [-n] [-x] [-s] [-u] [-v] [-h] { <st\u00edluslap> | -i }\n\nOPCI\u00d3K\n   -o <kimenet>    \u00f6sszerendeli a <kimenetet> a l\u00e9trehozott\n                  translet-tel. Alap\u00e9rtelmez\u00e9s szerint a translet neve\n                  a <st\u00edluslap> nev\u00e9b\u0151l j\u00f6n. Ez az opci\u00f3\n                  figyelmen k\u00edv\u00f3l marad, ha t\u00f6bb st\u00edluslapot ford\u00edt.\n   -d <alk\u00f6nyvt\u00e1r> meghat\u00e1rozza a translet c\u00e9l-alk\u00f6nyvt\u00e1r\u00e1t\n   -j <jarf\u00e1jl>   a translet oszt\u00e1lyokat egy jar f\u00e1jlba csomagolja,\n                  aminek a nev\u00e9t a <jarf\u00e1jl> attrib\u00fatum adja meg\n   -p <csomag>    meghat\u00e1rozza az \u00f6sszes gener\u00e1lt translet oszt\u00e1ly\n                  prefixnev\u00e9t.\n   -n             enged\u00e9lyezi a template inlining optimaliz\u00e1l\u00e1st\n                  (az alap\u00e9rtelmezett viselked\u00e9s \u00e1ltal\u00e1ban jobb).\n   -x             bekapcsolja a tov\u00e1bbi hibakeres\u00e9si \u00fczenet-kimenetet\n   -s             letiltja a System.exit h\u00edv\u00e1s\u00e1t\n   -u             a <st\u00edluslap> argumentumokat URL-k\u00e9nt \u00e9rtelmezi\n   -i             k\u00e9nyszer\u00edti a ford\u00edt\u00f3t, hogy a st\u00edluslapokat az stdin-r\u0151l olvassa\n   -v             ki\u00edrja a ford\u00edt\u00f3  verzi\u00f3j\u00e1t\n   -h             ki\u00edrja ezt a haszn\u00e1lati \u00fczenetet\n"},

        /*
         * Note to translators:  This message contains usage information for a
         * means of invoking XSLTC from the command-line.  The message is
         * formatted for presentation in English.  The strings <jarfile>,
         * <document>, etc. indicate user-specified argument values, and can
         * be translated - the argument <class> refers to a Java class, so it
         * should be handled in the same way the term is handled for JDK
         * documentation.
         */
        {ErrorMsg.TRANSFORM_USAGE_STR,
        "Haszn\u00e1lat \n   java org.apache.xalan.xsltc.cmdline.Transform [-j <jarf\u00e1jl>]\n      [-x] [-s] [-n <iter\u00e1ci\u00f3k>] {-u <dokumentum_url> | <dokumentum>}\n      <oszt\u00e1ly> [<param1>=<\u00e9rt\u00e9k1> ...]\n\n   a translet <oszt\u00e1lyt> haszn\u00e1lja a <dokumentum> \n   attrib\u00fatumban megadott XML dokumentum ford\u00edt\u00e1s\u00e1ra. A translet <osz\u00e1ly> vagy a\n   felhaszn\u00e1l\u00f3 CLASSPATH v\u00e1ltoz\u00f3ja alapj\u00e1n vagy a megadott <jarf\u00e1jl>-ban tal\u00e1lhat\u00f3 meg.\nOpci\u00f3k:\n   -j <jarf\u00e1jl>      megadja azt a jarf\u00e1jlt, amib\u0151l a translet-et be kell t\u00f6lteni\n   -x                bekapcsolja a tov\u00e1bbi hibakeres\u00e9si \u00fczeneteket\n   -s                letiltja a System.exit h\u00edv\u00e1s\u00e1t\n   -n <iter\u00e1ci\u00f3k>    az \u00e1talak\u00edt\u00e1st <iter\u00e1ci\u00f3k> alkalommal v\u00e9gzi el\n                     \u00e9s megjelen\u00edti a  teljes\u00edtm\u00e9ny-inform\u00e1ci\u00f3kat\n   -u <dokumentum_url> a bemeneti XML dokumentumot URL-k\u00e9nt adja meg\n"},



        /*
         * Note to translators:  "<xsl:sort>", "<xsl:for-each>" and
         * "<xsl:apply-templates>" are keywords that should not be translated.
         * The message indicates that an xsl:sort element must be a child of
         * one of the other kinds of elements mentioned.
         */
        {ErrorMsg.STRAY_SORT_ERR,
        "Az <xsl:sort> csak <xsl:for-each>-en vagy <xsl:apply-templates>-en bel\u00fcl haszn\u00e1lhat\u00f3."},

        /*
         * Note to translators:  The message indicates that the encoding
         * requested for the output document was on that requires support that
         * is not available from the Java Virtual Machine being used to execute
         * the program.
         */
        {ErrorMsg.UNSUPPORTED_ENCODING,
        "A(z) ''{0}'' kimeneti k\u00f3dol\u00e1st nem t\u00e1mogatja ez a JVM."},

        /*
         * Note to translators:  The message indicates that the XPath expression
         * named in the substitution text was not well formed syntactically.
         */
        {ErrorMsg.SYNTAX_ERR,
        "Szintaktikai hiba t\u00f6rt\u00e9nt ''{0}''-ben."},

        /*
         * Note to translators:  The substitution text is the name of a Java
         * class.  The term "constructor" here is the Java term.  The message is
         * displayed if XSLTC could not find a constructor for the specified
         * class.
         */
        {ErrorMsg.CONSTRUCTOR_NOT_FOUND,
        "Nem tal\u00e1lhat\u00f3 a(z) ''{0}'' k\u00fcls\u0151 konstruktor."},

        /*
         * Note to translators:  "static" is the Java keyword.  The substitution
         * text is the name of a function.  The first argument of that function
         * is not of the required type.
         */
        {ErrorMsg.NO_JAVA_FUNCT_THIS_REF,
        "A(z ''{0}'' nem statikus J\u00e1va f\u00fcggv\u00e9ny els\u0151 argumentuma nem egy \u00e9rv\u00e9nyes objektum-hivatkoz\u00e1s."},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  The substitution text is the
         * expression that was in error.
         */
        {ErrorMsg.TYPE_CHECK_ERR,
        "Hiba t\u00f6rt\u00e9nt a(z) ''{0}'' kifejez\u00e9s t\u00edpus\u00e1nak ellen\u0151rz\u00e9sekor."},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  However, the location of the
         * problematic expression is unknown.
         */
        {ErrorMsg.TYPE_CHECK_UNK_LOC_ERR,
        "Hiba t\u00f6rt\u00e9nt egy ismeretlen helyen l\u00e9v\u0151 kifejez\u00e9s t\u00edpus\u00e1nak ellen\u0151rz\u00e9sekor."},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option that was not recognized.
         */
        {ErrorMsg.ILLEGAL_CMDLINE_OPTION_ERR,
        "A(z) ''{0}'' parancssori opci\u00f3 \u00e9rv\u00e9nytelen."},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option.
         */
        {ErrorMsg.CMDLINE_OPT_MISSING_ARG_ERR,
        "A(z) ''{0}'' parancssori opci\u00f3hoz hi\u00e1nyzik egy k\u00f6telez\u0151 argumentum."},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.WARNING_PLUS_WRAPPED_MSG,
        "FIGYELEM:  ''{0}''\n       :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.WARNING_MSG,
        "FIGYELEM:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.FATAL_ERR_PLUS_WRAPPED_MSG,
        "V\u00c9GZETES HIBA:  ''{0}''\n           :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.FATAL_ERR_MSG,
        "V\u00c9GZETES HIBA:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.ERROR_PLUS_WRAPPED_MSG,
        "HIBA:  ''{0}''\n     :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.ERROR_MSG,
        "HIBA:  ''{0}''"},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSFORM_WITH_TRANSLET_STR,
        "\u00c1talak\u00edt\u00e1s a(z) ''{0}'' translet seg\u00edts\u00e9g\u00e9vel. "},

        /*
         * Note to translators:  The first substitution is the name of a class,
         * while the second substitution is the name of a jar file.
         */
        {ErrorMsg.TRANSFORM_WITH_JAR_STR,
        "\u00c1talak\u00edt\u00e1s a(z) ''{1}'' jar f\u00e1jlb\u00f3l a(z) ''{0}'' translet seg\u00edts\u00e9g\u00e9vel."},

        /*
         * Note to translators:  "TransformerFactory" is the name of a Java
         * interface and must not be translated.  The substitution text is
         * the name of the class that could not be instantiated.
         */
        {ErrorMsg.COULD_NOT_CREATE_TRANS_FACT,
        "Nem lehet l\u00e9trehozni a(z) ''{0}'' TransformerFactory oszt\u00e1ly p\u00e9ld\u00e1ny\u00e1t."},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages are collected together and displayed beneath
         * this message.
         */
        {ErrorMsg.COMPILER_ERROR_KEY,
        "Ford\u00edt\u00e1s hib\u00e1k:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the warning messages are collected together and displayed
         * beneath this message.
         */
        {ErrorMsg.COMPILER_WARNING_KEY,
        "Ford\u00edt\u00e1si figyelmeztet\u00e9sek:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages that are produced when the stylesheet is
         * applied to an input document are collected together and displayed
         * beneath this message.  A 'translet' is the compiled form of a
         * stylesheet (see above).
         */
        {ErrorMsg.RUNTIME_ERROR_KEY,
        "Translet hib\u00e1k:"}
    };
    }
}
