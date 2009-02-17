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
 * $Id: ErrorMessages_tr.java,v 1.3 2004/12/15 17:35:42 jycli Exp $
 */

package org.apache.xalan.xsltc.compiler.util;

import java.util.ListResourceBundle;

/**
 * @author Morten Jorgensen
 */
public class ErrorMessages_tr extends ListResourceBundle {

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
        "Ayn\u0131 dosyada birden \u00e7ok bi\u00e7em yapra\u011f\u0131 tan\u0131mland\u0131."},

        /*
         * Note to translators:  The substitution text is the name of a
         * template.  The same name was used on two different templates in the
         * same stylesheet.
         */
        {ErrorMsg.TEMPLATE_REDEF_ERR,
        "Bi\u00e7em yapra\u011f\u0131nda ''{0}'' \u015fablonu zaten tan\u0131ml\u0131."},


        /*
         * Note to translators:  The substitution text is the name of a
         * template.  A reference to the template name was encountered, but the
         * template is undefined.
         */
        {ErrorMsg.TEMPLATE_UNDEF_ERR,
        "Bu bi\u00e7em yapra\u011f\u0131nda ''{0}'' \u015fablonu tan\u0131ml\u0131 de\u011fil."},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * that was defined more than once.
         */
        {ErrorMsg.VARIABLE_REDEF_ERR,
        "''{0}'' de\u011fi\u015fkeni ayn\u0131 kapsamda bir kereden \u00e7ok tan\u0131mland\u0131."},

        /*
         * Note to translators:  The substitution text is the name of a variable
         * or parameter.  A reference to the variable or parameter was found,
         * but it was never defined.
         */
        {ErrorMsg.VARIABLE_UNDEF_ERR,
        "''{0}'' de\u011fi\u015fkeni ya da de\u011fi\u015ftirgesi tan\u0131ml\u0131 de\u011fil."},

        /*
         * Note to translators:  The word "class" here refers to a Java class.
         * Processing the stylesheet required a class to be loaded, but it could
         * not be found.  The substitution text is the name of the class.
         */
        {ErrorMsg.CLASS_NOT_FOUND_ERR,
        "''{0}'' s\u0131n\u0131f\u0131 bulunam\u0131yor."},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but it could not be found.  "public" is the
         * Java keyword.
         */
        {ErrorMsg.METHOD_NOT_FOUND_ERR,
        "''{0}'' d\u0131\u015f y\u00f6ntemi bulunam\u0131yor (public olmal\u0131)."},

        /*
         * Note to translators:  The word "method" here refers to a Java method.
         * Processing the stylesheet required a reference to the method named by
         * the substitution text, but no method with the required types of
         * arguments or return type could be found.
         */
        {ErrorMsg.ARGUMENT_CONVERSION_ERR,
        "''{0}'' y\u00f6ntemi \u00e7a\u011fr\u0131s\u0131nda ba\u011f\u0131ms\u0131z de\u011fi\u015fken/d\u00f6n\u00fc\u015f tipi d\u00f6n\u00fc\u015ft\u00fcr\u00fclemiyor."},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * is missing.
         */
        {ErrorMsg.FILE_NOT_FOUND_ERR,
        "Dosya ya da URI ''{0}'' bulunamad\u0131."},

        /*
         * Note to translators:  This message is displayed when the URI
         * mentioned in the substitution text is not well-formed syntactically.
         */
        {ErrorMsg.INVALID_URI_ERR,
        "Ge\u00e7ersiz URI ''{0}''."},

        /*
         * Note to translators:  The file or URI named in the substitution text
         * exists but could not be opened.
         */
        {ErrorMsg.FILE_ACCESS_ERR,
        "Dosya ya da URI ''{0}'' a\u00e7\u0131lam\u0131yor."},

        /*
         * Note to translators: <xsl:stylesheet> and <xsl:transform> are
         * keywords that should not be translated.
         */
        {ErrorMsg.MISSING_ROOT_ERR,
        "<xsl:stylesheet> ya da <xsl:transform> \u00f6\u011fesi bekleniyor."},

        /*
         * Note to translators:  The stylesheet contained a reference to a
         * namespace prefix that was undefined.  The value of the substitution
         * text is the name of the prefix.
         */
        {ErrorMsg.NAMESPACE_UNDEF_ERR,
        "Ad alan\u0131 \u00f6neki ''{0}'' bildirilmemi\u015f."},

        /*
         * Note to translators:  The Java function named in the stylesheet could
         * not be found.
         */
        {ErrorMsg.FUNCTION_RESOLVE_ERR,
        "''{0}'' i\u015flevi \u00e7a\u011fr\u0131s\u0131 \u00e7\u00f6z\u00fclemiyor."},

        /*
         * Note to translators:  The substitution text is the name of a
         * function.  A literal string here means a constant string value.
         */
        {ErrorMsg.NEED_LITERAL_ERR,
        "''{0}'' i\u015flevine ili\u015fkin ba\u011f\u0131ms\u0131z de\u011fi\u015fken bir haz\u0131r bilgi dizgisi olmal\u0131d\u0131r."},

        /*
         * Note to translators:  This message indicates there was a syntactic
         * error in the form of an XPath expression.  The substitution text is
         * the expression.
         */
        {ErrorMsg.XPATH_PARSER_ERR,
        "XPath ifadesi ''{0}'' ayr\u0131\u015ft\u0131r\u0131l\u0131rken hata olu\u015ftu."},

        /*
         * Note to translators:  An element in the stylesheet requires a
         * particular attribute named by the substitution text, but that
         * attribute was not specified in the stylesheet.
         */
        {ErrorMsg.REQUIRED_ATTR_ERR,
        "Gerekli ''{0}'' \u00f6zniteli\u011fi eksik."},

        /*
         * Note to translators:  This message indicates that a character not
         * permitted in an XPath expression was encountered.  The substitution
         * text is the offending character.
         */
        {ErrorMsg.ILLEGAL_CHAR_ERR,
        "XPath ifadesinde ge\u00e7ersiz ''{0}'' karakteri var."},

        /*
         * Note to translators:  A processing instruction is a mark-up item in
         * an XML document that request some behaviour of an XML processor.  The
         * form of the name of was invalid in this case, and the substitution
         * text is the name.
         */
        {ErrorMsg.ILLEGAL_PI_ERR,
        "\u0130\u015fleme y\u00f6nergesi i\u00e7in ''{0}'' ad\u0131 ge\u00e7ersiz."},

        /*
         * Note to translators:  This message is reported if the stylesheet
         * being processed attempted to construct an XML document with an
         * attribute in a place other than on an element.  The substitution text
         * specifies the name of the attribute.
         */
        {ErrorMsg.STRAY_ATTRIBUTE_ERR,
        "''{0}'' \u00f6zniteli\u011fi \u00f6\u011fenin d\u0131\u015f\u0131nda."},

        /*
         * Note to translators:  An attribute that wasn't recognized was
         * specified on an element in the stylesheet.  The attribute is named
         * by the substitution
         * text.
         */
        {ErrorMsg.ILLEGAL_ATTRIBUTE_ERR,
        "''{0}'' \u00f6zniteli\u011fi ge\u00e7ersiz."},

        /*
         * Note to translators:  "import" and "include" are keywords that should
         * not be translated.  This messages indicates that the stylesheet
         * named in the substitution text imported or included itself either
         * directly or indirectly.
         */
        {ErrorMsg.CIRCULAR_INCLUDE_ERR,
        "\u00c7evrimsel import/include. ''{0}'' bi\u00e7em yapra\u011f\u0131 zaten y\u00fcklendi."},

        /*
         * Note to translators:  A result-tree fragment is a portion of a
         * resulting XML document represented as a tree.  "<xsl:sort>" is a
         * keyword and should not be translated.
         */
        {ErrorMsg.RESULT_TREE_SORT_ERR,
        "Sonu\u00e7 a\u011fac\u0131 par\u00e7alar\u0131 s\u0131ralanam\u0131yor (<xsl:sort> \u00f6\u011feleri yok say\u0131ld\u0131). D\u00fc\u011f\u00fcmleri sonu\u00e7 a\u011fac\u0131n\u0131 yarat\u0131rken s\u0131ralamal\u0131s\u0131n\u0131z."},

        /*
         * Note to translators:  A name can be given to a particular style to be
         * used to format decimal values.  The substitution text gives the name
         * of such a style for which more than one declaration was encountered.
         */
        {ErrorMsg.SYMBOLS_REDEF_ERR,
        "Onlu bi\u00e7imleme bi\u00e7emi ''{0}'' zaten tan\u0131ml\u0131."},

        /*
         * Note to translators:  The stylesheet version named in the
         * substitution text is not supported.
         */
        {ErrorMsg.XSL_VERSION_ERR,
        "XSL s\u00fcr\u00fcm\u00fc ''{0}'' XSLTC taraf\u0131ndan desteklenmiyor."},

        /*
         * Note to translators:  The definitions of one or more variables or
         * parameters depend on one another.
         */
        {ErrorMsg.CIRCULAR_VARIABLE_ERR,
        "''{0}'' i\u00e7inde \u00e7evrimsel de\u011fi\u015fken/de\u011fi\u015ftirge ba\u015fvurusu."},

        /*
         * Note to translators:  The operator in an expresion with two operands was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_BINARY_OP_ERR,
        "\u0130kili ifadede bilinmeyen i\u015fle\u00e7."},

        /*
         * Note to translators:  This message is produced if a reference to a
         * function has too many or too few arguments.
         */
        {ErrorMsg.ILLEGAL_ARG_ERR,
        "\u0130\u015flev \u00e7a\u011fr\u0131s\u0131 i\u00e7in ge\u00e7ersiz say\u0131da ba\u011f\u0131ms\u0131z de\u011fi\u015fken."},

        /*
         * Note to translators:  "document()" is the name of function and must
         * not be translated.  A node-set is a set of the nodes in the tree
         * representation of an XML document.
         */
        {ErrorMsg.DOCUMENT_ARG_ERR,
        "document() i\u015flevinin ikinci ba\u011f\u0131ms\u0131z de\u011fi\u015fkeni d\u00fc\u011f\u00fcm k\u00fcmesi olmal\u0131d\u0131r."},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.MISSING_WHEN_ERR,
        "<xsl:choose> i\u00e7inde en az bir <xsl:when> \u00f6\u011fesi gereklidir."},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.MULTIPLE_OTHERWISE_ERR,
        "<xsl:choose> i\u00e7inde tek bir <xsl:otherwise> \u00f6\u011fesine izin verilir."},

        /*
         * Note to translators:  "<xsl:otherwise>" and "<xsl:choose>" are
         * keywords and should not be translated.  This message describes a
         * syntax error in the stylesheet.
         */
        {ErrorMsg.STRAY_OTHERWISE_ERR,
        "<xsl:otherwise> yaln\u0131zca <xsl:choose> i\u00e7inde kullan\u0131labilir."},

        /*
         * Note to translators:  "<xsl:when>" and "<xsl:choose>" are keywords
         * and should not be translated.  This message describes a syntax error
         * in the stylesheet.
         */
        {ErrorMsg.STRAY_WHEN_ERR,
        "<xsl:when> yaln\u0131zca <xsl:choose> i\u00e7inde kullan\u0131labilir."},

        /*
         * Note to translators:  "<xsl:when>", "<xsl:otherwise>" and
         * "<xsl:choose>" are keywords and should not be translated.  This
         * message describes a syntax error in the stylesheet.
         */
        {ErrorMsg.WHEN_ELEMENT_ERR,
        "<xsl:choose> i\u00e7inde yaln\u0131zca <xsl:when> ve <xsl:otherwise> \u00f6\u011feleri kullan\u0131labilir."},

        /*
         * Note to translators:  "<xsl:attribute-set>" and "name" are keywords
         * that should not be translated.
         */
        {ErrorMsg.UNNAMED_ATTRIBSET_ERR,
        "<xsl:attribute-set> \u00f6\u011fesinde 'name' \u00f6zniteli\u011fi eksik."},

        /*
         * Note to translators:  An element in the stylesheet contained an
         * element of a type that it was not permitted to contain.
         */
        {ErrorMsg.ILLEGAL_CHILD_ERR,
        "Ge\u00e7ersiz alt \u00f6\u011fe."},

        /*
         * Note to translators:  The stylesheet tried to create an element with
         * a name that was not a valid XML name.  The substitution text contains
         * the name.
         */
        {ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
        "Bir \u00f6\u011feye ''{0}'' ad\u0131 verilemez."},

        /*
         * Note to translators:  The stylesheet tried to create an attribute
         * with a name that was not a valid XML name.  The substitution text
         * contains the name.
         */
        {ErrorMsg.ILLEGAL_ATTR_NAME_ERR,
        "Bir \u00f6zniteli\u011fe ''{0}'' ad\u0131 verilemez."},

        /*
         * Note to translators:  The children of the outermost element of a
         * stylesheet are referred to as top-level elements.  No text should
         * occur within that outermost element unless it is within a top-level
         * element.  This message indicates that that constraint was violated.
         * "<xsl:stylesheet>" is a keyword that should not be translated.
         */
        {ErrorMsg.ILLEGAL_TEXT_NODE_ERR,
        "\u00dcst d\u00fczey <xsl:stylesheet> \u00f6\u011fesi d\u0131\u015f\u0131nda metin verisi."},

        /*
         * Note to translators:  JAXP is an acronym for the Java API for XML
         * Processing.  This message indicates that the XML parser provided to
         * XSLTC to process the XML input document had a configuration problem.
         */
        {ErrorMsg.SAX_PARSER_CONFIG_ERR,
        "JAXP ayr\u0131\u015ft\u0131r\u0131c\u0131s\u0131 do\u011fru yap\u0131land\u0131r\u0131lmam\u0131\u015f"},

        /*
         * Note to translators:  The substitution text names the internal error
         * encountered.
         */
        {ErrorMsg.INTERNAL_ERR,
        "Kurtar\u0131lamaz XSLTC i\u00e7 hatas\u0131: ''{0}''"},

        /*
         * Note to translators:  The stylesheet contained an element that was
         * not recognized as part of the XSL syntax.  The substitution text
         * gives the element name.
         */
        {ErrorMsg.UNSUPPORTED_XSL_ERR,
        "XSL \u00f6\u011fesi ''{0}'' desteklenmiyor."},

        /*
         * Note to translators:  The stylesheet referred to an extension to the
         * XSL syntax and indicated that it was defined by XSLTC, but XSTLC does
         * not recognized the particular extension named.  The substitution text
         * gives the extension name.
         */
        {ErrorMsg.UNSUPPORTED_EXT_ERR,
        "XSLTC uzant\u0131s\u0131 ''{0}'' tan\u0131nm\u0131yor."},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.  XSLTC is able to detect that in this
         * case because the outermost element in the stylesheet has to be
         * declared with respect to the XSL namespace URI, but no declaration
         * for that namespace was seen.
         */
        {ErrorMsg.MISSING_XSLT_URI_ERR,
        "Giri\u015f belgesi bir bi\u00e7em yapra\u011f\u0131 de\u011fil (XSL ad alan\u0131 k\u00f6k \u00f6\u011fede bildirilmedi)."},

        /*
         * Note to translators:  XSLTC could not find the stylesheet document
         * with the name specified by the substitution text.
         */
        {ErrorMsg.MISSING_XSLT_TARGET_ERR,
        "Bi\u00e7em yapra\u011f\u0131 hedefi ''{0}'' bulunamad\u0131."},

        /*
         * Note to translators:  This message represents an internal error in
         * condition in XSLTC.  The substitution text is the class name in XSLTC
         * that is missing some functionality.
         */
        {ErrorMsg.NOT_IMPLEMENTED_ERR,
        "Uygulanmad\u0131: ''{0}''."},

        /*
         * Note to translators:  The XML document given to XSLTC as a stylesheet
         * was not, in fact, a stylesheet.
         */
        {ErrorMsg.NOT_STYLESHEET_ERR,
        "Giri\u015f belgesi bir XSL bi\u00e7em yapra\u011f\u0131 i\u00e7ermiyor."},

        /*
         * Note to translators:  The element named in the substitution text was
         * encountered in the stylesheet but is not recognized.
         */
        {ErrorMsg.ELEMENT_PARSE_ERR,
        "''{0}'' \u00f6\u011fesi ayr\u0131\u015ft\u0131r\u0131lamad\u0131."},

        /*
         * Note to translators:  "use", "<key>", "node", "node-set", "string"
         * and "number" are keywords in this context and should not be
         * translated.  This message indicates that the value of the "use"
         * attribute was not one of the permitted values.
         */
        {ErrorMsg.KEY_USE_ATTR_ERR,
        "<key> ile ilgili use \u00f6zniteli\u011fi node, node-set, string ya da number olmal\u0131d\u0131r."},

        /*
         * Note to translators:  An XML document can specify the version of the
         * XML specification to which it adheres.  This message indicates that
         * the version specified for the output document was not valid.
         */
        {ErrorMsg.OUTPUT_VERSION_ERR,
        "\u00c7\u0131k\u0131\u015f XML belgesi s\u00fcr\u00fcm\u00fc 1.0 olmal\u0131d\u0131r."},

        /*
         * Note to translators:  The operator in a comparison operation was
         * not recognized.
         */
        {ErrorMsg.ILLEGAL_RELAT_OP_ERR,
        "\u0130li\u015fkisel ifade i\u00e7in bilinmeyen i\u015fle\u00e7"},

        /*
         * Note to translators:  An attribute set defines as a set of XML
         * attributes that can be added to an element in the output XML document
         * as a group.  This message is reported if the name specified was not
         * used to declare an attribute set.  The substitution text is the name
         * that is in error.
         */
        {ErrorMsg.ATTRIBSET_UNDEF_ERR,
        "Varolmayan ''{0}'' \u00f6znitelik k\u00fcmesini kullanma giri\u015fimi."},

        /*
         * Note to translators:  The term "attribute value template" is a term
         * defined by XSLT which describes the value of an attribute that is
         * determined by an XPath expression.  The message indicates that the
         * expression was syntactically incorrect; the substitution text
         * contains the expression that was in error.
         */
        {ErrorMsg.ATTR_VAL_TEMPLATE_ERR,
        "\u00d6znitelik de\u011feri \u015fablonu ''{0}'' ayr\u0131\u015ft\u0131r\u0131lam\u0131yor."},

        /*
         * Note to translators:  ???
         */
        {ErrorMsg.UNKNOWN_SIG_TYPE_ERR,
        "''{0}'' s\u0131n\u0131f\u0131na ili\u015fkin imzada bilinmeyen veri tipi."},

        /*
         * Note to translators:  The substitution text refers to data types.
         * The message is displayed if a value in a particular context needs to
         * be converted to type {1}, but that's not possible for a value of
         * type {0}.
         */
        {ErrorMsg.DATA_CONVERSION_ERR,
        "''{0}'' veri tipi ''{1}'' tipine d\u00f6n\u00fc\u015ft\u00fcr\u00fclemez."},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_TRANSLET_CLASS_ERR,
        "Bu Templates ge\u00e7erli bir derleme sonucu s\u0131n\u0131f tan\u0131m\u0131 i\u00e7ermiyor."},

        /*
         * Note to translators:  "Templates" is a Java class name that should
         * not be translated.
         */
        {ErrorMsg.NO_MAIN_TRANSLET_ERR,
        "Bu Templates ''{0}'' ad\u0131nda bir s\u0131n\u0131f i\u00e7ermiyor."},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSLET_CLASS_ERR,
        "Derleme sonucu s\u0131n\u0131f\u0131 ''{0}'' y\u00fcklenemedi."},

        {ErrorMsg.TRANSLET_OBJECT_ERR,
        "Derleme sonucu s\u0131n\u0131f\u0131 y\u00fcklendi, ancak derleme sonucu s\u0131n\u0131f\u0131n\u0131n somut kopyas\u0131 yarat\u0131lam\u0131yor."},

        /*
         * Note to translators:  "ErrorListener" is a Java interface name that
         * should not be translated.  The message indicates that the user tried
         * to set an ErrorListener object on object of the class named in the
         * substitution text with "null" Java value.
         */
        {ErrorMsg.ERROR_LISTENER_NULL_ERR,
        "''{0}'' ile ilgili ErrorListener nesnesini bo\u015f de\u011fer (null) olarak ayarlama giri\u015fimi."},

        /*
         * Note to translators:  StreamSource, SAXSource and DOMSource are Java
         * interface names that should not be translated.
         */
        {ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR,
        "XSLTC yaln\u0131zca StreamSource, SAXSource ve DOMSource arabirimlerini destekler."},

        /*
         * Note to translators:  "Source" is a Java class name that should not
         * be translated.  The substitution text is the name of Java method.
         */
        {ErrorMsg.JAXP_NO_SOURCE_ERR,
        "''{0}'' y\u00f6ntemine aktar\u0131lan Source nesnesinin i\u00e7eri\u011fi yok."},

        /*
         * Note to translators:  The message indicates that XSLTC failed to
         * compile the stylesheet into a translet (class file).
         */
        {ErrorMsg.JAXP_COMPILE_ERR,
        "Bi\u00e7em yapra\u011f\u0131 derlenemedi."},

        /*
         * Note to translators:  "TransformerFactory" is a class name.  In this
         * context, an attribute is a property or setting of the
         * TransformerFactory object.  The substitution text is the name of the
         * unrecognised attribute.  The method used to retrieve the attribute is
         * "getAttribute", so it's not clear whether it would be best to
         * translate the term "attribute".
         */
        {ErrorMsg.JAXP_INVALID_ATTR_ERR,
        "TransformerFactory ''{0}'' \u00f6zniteli\u011fini tan\u0131m\u0131yor."},

        /*
         * Note to translators:  "setResult()" and "startDocument()" are Java
         * method names that should not be translated.
         */
        {ErrorMsg.JAXP_SET_RESULT_ERR,
        "startDocument() y\u00f6nteminden \u00f6nce setResult() \u00e7a\u011fr\u0131lmal\u0131d\u0131r."},

        /*
         * Note to translators:  "Transformer" is a Java interface name that
         * should not be translated.  A Transformer object should contained a
         * reference to a translet object in order to be used for
         * transformations; this message is produced if that requirement is not
         * met.
         */
        {ErrorMsg.JAXP_NO_TRANSLET_ERR,
        "Transformer, derleme sonucu s\u0131n\u0131f dosyas\u0131 nesnesine ba\u015fvuru i\u00e7ermiyor."},

        /*
         * Note to translators:  The XML document that results from a
         * transformation needs to be sent to an output handler object; this
         * message is produced if that requirement is not met.
         */
        {ErrorMsg.JAXP_NO_HANDLER_ERR,
        "D\u00f6n\u00fc\u015ft\u00fcrme sonucu i\u00e7in tan\u0131ml\u0131 \u00e7\u0131k\u0131\u015f i\u015fleyicisi yok."},

        /*
         * Note to translators:  "Result" is a Java interface name in this
         * context.  The substitution text is a method name.
         */
        {ErrorMsg.JAXP_NO_RESULT_ERR,
        "''{0}'' y\u00f6ntemine aktar\u0131lan Result nesnesi ge\u00e7ersiz."},

        /*
         * Note to translators:  "Transformer" is a Java interface name.  The
         * user's program attempted to access an unrecognized property with the
         * name specified in the substitution text.  The method used to retrieve
         * the property is "getOutputProperty", so it's not clear whether it
         * would be best to translate the term "property".
         */
        {ErrorMsg.JAXP_UNKNOWN_PROP_ERR,
        "Ge\u00e7ersiz ''{0}'' Transformer \u00f6zelli\u011fine eri\u015fme giri\u015fimi."},

        /*
         * Note to translators:  SAX2DOM is the name of a Java class that should
         * not be translated.  This is an adapter in the sense that it takes a
         * DOM object and converts it to something that uses the SAX API.
         */
        {ErrorMsg.SAX2DOM_ADAPTER_ERR,
        "SAX2DOM ba\u011fda\u015ft\u0131r\u0131c\u0131s\u0131 yarat\u0131lamad\u0131: ''{0}''."},

        /*
         * Note to translators:  "XSLTCSource.build()" is a Java method name.
         * "systemId" is an XML term that is short for "system identification".
         */
        {ErrorMsg.XSLTC_SOURCE_ERR,
        "XSLTCSource.build() y\u00f6ntemi systemId tan\u0131mlanmadan \u00e7a\u011fr\u0131ld\u0131."},


        {ErrorMsg.COMPILE_STDIN_ERR,
        "-i se\u00e7ene\u011fi -o se\u00e7ene\u011fiyle birlikte kullan\u0131lmal\u0131d\u0131r."},


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
        "\u00d6ZET\n   java org.apache.xalan.xsltc.cmdline.Compile [-o <\u00e7\u0131k\u0131\u015f>]\n      [-d <dizin>] [-j <jardosyas\u0131>] [-p <paket>]\n      [-n] [-x] [-s] [-u] [-v] [-h] { <bi\u00e7emyapra\u011f\u0131> | -i }\n\nSE\u00c7ENEKLER\n   -o <\u00e7\u0131k\u0131\u015f>    derleme sonucu s\u0131n\u0131f dosyas\u0131na <\u00e7\u0131k\u0131\u015f>\n                  ad\u0131n\u0131 atar. Varsay\u0131lan olarak, derleme sonucu s\u0131n\u0131f dosyas\u0131\n                  ad\u0131 <bi\u00e7emyapra\u011f\u0131> ad\u0131ndan al\u0131n\u0131r. Birden \u00e7ok bi\u00e7em yapra\u011f\u0131 derleniyorsa\n                  bu se\u00e7enek dikkate al\u0131nmaz.\n   -d <dizin> derleme sonucu s\u0131n\u0131f dosyas\u0131 i\u00e7in hedef dizini belirtir.\n   -j <jardosyas\u0131>   derleme sonucu s\u0131n\u0131f dosyalar\u0131n\u0131\n                  <jardosyas\u0131> dosyas\u0131nda paketler.\n   -p <paket>   derleme sonucu \u00fcretilen t\u00fcm s\u0131n\u0131f dosyalar\u0131 i\u00e7in\n                  bir paket ad\u0131 \u00f6neki belirtir.\n   -n             \u015fablona do\u011frudan yerle\u015ftirmeyi etkinle\u015ftirir (ortalama olarak\n                  daha y\u00fcksek ba\u015far\u0131m sa\u011flar).\n   -x             ek hata ay\u0131klama iletisi \u00e7\u0131k\u0131\u015f\u0131n\u0131 etkinle\u015ftirir.\n   -s             System.exit \u00e7a\u011fr\u0131s\u0131n\u0131 ge\u00e7ersiz k\u0131lar.\n   -u             <bi\u00e7emyapra\u011f\u0131> ba\u011f\u0131ms\u0131z de\u011fi\u015fkenlerini URL olarak yorumlar.\n   -i             derleyiciyi stdin'den bi\u00e7em yapra\u011f\u0131n\u0131 okumaya zorlar.\n   -v             derleyici s\u00fcr\u00fcm\u00fcn\u00fc yazd\u0131r\u0131r.\n   -h             bu kullan\u0131m bilgilerini yazd\u0131r\u0131r.\n"},

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
        "S\u00d6ZD\u0130Z\u0130M\u0130 \n   java org.apache.xalan.xsltc.cmdline.Transform [-j <jardosyas\u0131>]\n      [-x] [-s] [-n <yineleme say\u0131s\u0131>] {-u <belge_url> | <belge>}\n      <s\u0131n\u0131f> [<de\u011fi\u015ftirge1>=<de\u011fer1> ...]\n\n   <belge> ile belirtilen XML belgesini d\u00f6n\u00fc\u015ft\u00fcrmek i\u00e7in <s\u0131n\u0131f> \n   s\u0131n\u0131f dosyas\u0131n\u0131 kullan\u0131r. <s\u0131n\u0131f> s\u0131n\u0131f dosyas\u0131\n   kullan\u0131c\u0131n\u0131n CLASSPATH de\u011fi\u015fkeninde ya da iste\u011fe ba\u011fl\u0131 olarak belirtilen <jardosyas\u0131> dosyas\u0131ndad\u0131r.\nSE\u00c7ENEKLER\n   -j <jardosyas\u0131>    derleme sonucu s\u0131n\u0131f dosyas\u0131n\u0131n hangi jar dosyas\u0131ndan y\u00fcklenece\u011fini belirtir.\n   -x              ek hata ay\u0131klama iletisi \u00e7\u0131k\u0131\u015f\u0131n\u0131 etkinle\u015ftirir.\n   -s              System.exit \u00e7a\u011fr\u0131s\u0131n\u0131 ge\u00e7ersiz k\u0131lar.\n   -n <yineleme say\u0131s\u0131> d\u00f6n\u00fc\u015ft\u00fcrmeyi <yineleme say\u0131s\u0131> ile belirtilen say\u0131 kadar \u00e7al\u0131\u015ft\u0131r\u0131r ve\n                   yakalama bilgilerini g\u00f6r\u00fcnt\u00fcler.\n   -u <belge_url> XML giri\u015f belgesini URL olarak belirtir.\n"},



        /*
         * Note to translators:  "<xsl:sort>", "<xsl:for-each>" and
         * "<xsl:apply-templates>" are keywords that should not be translated.
         * The message indicates that an xsl:sort element must be a child of
         * one of the other kinds of elements mentioned.
         */
        {ErrorMsg.STRAY_SORT_ERR,
        "<xsl:sort> yaln\u0131zca <xsl:for-each> ya da <xsl:apply-templates> i\u00e7inde kullan\u0131labilir."},

        /*
         * Note to translators:  The message indicates that the encoding
         * requested for the output document was on that requires support that
         * is not available from the Java Virtual Machine being used to execute
         * the program.
         */
        {ErrorMsg.UNSUPPORTED_ENCODING,
        "''{0}'' \u00e7\u0131k\u0131\u015f kodlamas\u0131 bu JVM \u00fczerinde desteklenmiyor."},

        /*
         * Note to translators:  The message indicates that the XPath expression
         * named in the substitution text was not well formed syntactically.
         */
        {ErrorMsg.SYNTAX_ERR,
        "''{0}'' ifadesinde s\u00f6zdizimi hatas\u0131."},

        /*
         * Note to translators:  The substitution text is the name of a Java
         * class.  The term "constructor" here is the Java term.  The message is
         * displayed if XSLTC could not find a constructor for the specified
         * class.
         */
        {ErrorMsg.CONSTRUCTOR_NOT_FOUND,
        "D\u0131\u015f olu\u015fturucu ''{0}'' bulunam\u0131yor."},

        /*
         * Note to translators:  "static" is the Java keyword.  The substitution
         * text is the name of a function.  The first argument of that function
         * is not of the required type.
         */
        {ErrorMsg.NO_JAVA_FUNCT_THIS_REF,
        "Dura\u011fan (static) olmayan ''{0}'' Java i\u015flevine ili\u015fkin ilk ba\u011f\u0131ms\u0131z de\u011fi\u015fken ge\u00e7erli bir nesne ba\u015fvurusu de\u011fil. "},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  The substitution text is the
         * expression that was in error.
         */
        {ErrorMsg.TYPE_CHECK_ERR,
        "''{0}'' ifadesinin tipi denetlenirken hata saptand\u0131."},

        /*
         * Note to translators:  An XPath expression was not of the type
         * required in a particular context.  However, the location of the
         * problematic expression is unknown.
         */
        {ErrorMsg.TYPE_CHECK_UNK_LOC_ERR,
        "Bilinmeyen bir yerdeki bir ifadenin tipi denetlenirken hata saptand\u0131."},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option that was not recognized.
         */
        {ErrorMsg.ILLEGAL_CMDLINE_OPTION_ERR,
        "Komut sat\u0131r\u0131 se\u00e7ene\u011fi ''{0}'' ge\u00e7erli de\u011fil."},

        /*
         * Note to translators:  The substitution text is the name of a command-
         * line option.
         */
        {ErrorMsg.CMDLINE_OPT_MISSING_ARG_ERR,
        "''{0}'' komut sat\u0131r\u0131 se\u00e7ene\u011finde gerekli bir ba\u011f\u0131ms\u0131z de\u011fi\u015fken eksik."},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.WARNING_PLUS_WRAPPED_MSG,
        "UYARI:  ''{0}''\n       :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.WARNING_MSG,
        "UYARI:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.FATAL_ERR_PLUS_WRAPPED_MSG,
        "ONULMAZ HATA:  ''{0}''\n           :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.FATAL_ERR_MSG,
        "ONULMAZ HATA:  ''{0}''"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text contains two error
         * messages.  The spacing before the second substitution text indents
         * it the same amount as the first in English.
         */
        {ErrorMsg.ERROR_PLUS_WRAPPED_MSG,
        "HATA:  ''{0}''\n     :{1}"},

        /*
         * Note to translators:  This message is used to indicate the severity
         * of another message.  The substitution text is an error message.
         */
        {ErrorMsg.ERROR_MSG,
        "HATA:  ''{0}''"},

        /*
         * Note to translators:  The substitution text is the name of a class.
         */
        {ErrorMsg.TRANSFORM_WITH_TRANSLET_STR,
        "''{0}'' s\u0131n\u0131f\u0131n\u0131 kullanarak d\u00f6n\u00fc\u015ft\u00fcr"},

        /*
         * Note to translators:  The first substitution is the name of a class,
         * while the second substitution is the name of a jar file.
         */
        {ErrorMsg.TRANSFORM_WITH_JAR_STR,
        "''{1}'' jar dosyas\u0131ndan ''{0}'' s\u0131n\u0131f\u0131n\u0131 kullanarak d\u00f6n\u00fc\u015ft\u00fcr"},

        /*
         * Note to translators:  "TransformerFactory" is the name of a Java
         * interface and must not be translated.  The substitution text is
         * the name of the class that could not be instantiated.
         */
        {ErrorMsg.COULD_NOT_CREATE_TRANS_FACT,
        "''{0}'' TransformerFactory s\u0131n\u0131f\u0131n\u0131n somut kopyas\u0131 yarat\u0131lamad\u0131."},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages are collected together and displayed beneath
         * this message.
         */
        {ErrorMsg.COMPILER_ERROR_KEY,
        "Derleyici hatalar\u0131:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the warning messages are collected together and displayed
         * beneath this message.
         */
        {ErrorMsg.COMPILER_WARNING_KEY,
        "Derleyici uyar\u0131lar\u0131:"},

        /*
         * Note to translators:  The following message is used as a header.
         * All the error messages that are produced when the stylesheet is
         * applied to an input document are collected together and displayed
         * beneath this message.  A 'translet' is the compiled form of a
         * stylesheet (see above).
         */
        {ErrorMsg.RUNTIME_ERROR_KEY,
        "Derleme sonusu s\u0131n\u0131f dosyas\u0131 hatalar\u0131:"}
    };
    }
}
