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
 * $Id: ErrorMessages_no.java,v 1.11 2004/12/15 17:35:42 jycli Exp $
 */

package org.apache.xalan.xsltc.compiler.util;


/**
 * @author Morten Jorgensen
 */
public final class ErrorMessages_no extends ErrorMessages {
    
    // Disse feilmeldingene maa korrespondere med konstantene som er definert
    // i kildekoden til {ErrorMsg.
    /** Get the lookup table for error messages.   
     *
     * @return The message lookup table.
     */
    public Object[][] getContents()
    {
      return new Object[][] { 
	{ErrorMsg.MULTIPLE_STYLESHEET_ERR,
	"En fil kan bare innehold ett stilark."},
	{ErrorMsg.TEMPLATE_REDEF_ERR,	
	"<xsl:template> ''{0}'' er allerede definert i dette stilarket."},
	{ErrorMsg.TEMPLATE_UNDEF_ERR,
	"<xsl:template> ''{0}'' er ikke definert i dette stilarket."},
	{ErrorMsg.VARIABLE_REDEF_ERR,	
	"Variabel ''{0}'' er allerede definert."},
	{ErrorMsg.VARIABLE_UNDEF_ERR,
	"Variabel eller parameter ''{0}'' er ikke definert."},
	{ErrorMsg.CLASS_NOT_FOUND_ERR,
	"Finner ikke klassen ''{0}''."},
	{ErrorMsg.METHOD_NOT_FOUND_ERR,
	"Finner ikke ekstern funksjon ''{0}'' (m\u00e5 v\00e6re deklarert b\u00e5de 'static' og 'public')."},
	{ErrorMsg.ARGUMENT_CONVERSION_ERR,
	"Kan ikke konvertere argument/retur type i kall til funksjon ''{0}''"},
	{ErrorMsg.FILE_NOT_FOUND_ERR,
	"Finner ikke fil eller URI ''{0}''."},
	{ErrorMsg.INVALID_URI_ERR,
	"Ugyldig URI ''{0}''."},
	{ErrorMsg.FILE_ACCESS_ERR,
	"Kan ikke \u00e5pne fil eller URI ''{0}''."},
	{ErrorMsg.MISSING_ROOT_ERR,
	"Forvented <xsl:stylesheet> eller <xsl:transform> element."},
	{ErrorMsg.NAMESPACE_UNDEF_ERR,
	"Prefiks ''{0}'' er ikke deklarert."},
	{ErrorMsg.FUNCTION_RESOLVE_ERR,
	"Kunne ikke resolvere kall til funksjon ''{0}''."},
	{ErrorMsg.NEED_LITERAL_ERR,
	"Argument til ''{0}'' m\u00e5 v\00e6re ordrett tekst."},
	{ErrorMsg.XPATH_PARSER_ERR,
	"Kunne ikke tolke XPath uttrykk ''{0}''."},
	{ErrorMsg.REQUIRED_ATTR_ERR,
	"N\u00f8dvendig attributt ''{0}'' er ikke deklarert."},
	{ErrorMsg.ILLEGAL_CHAR_ERR,
	"Ugyldig bokstav/tegn ''{0}'' i XPath uttrykk."},
	{ErrorMsg.ILLEGAL_PI_ERR,
	"Ugyldig navn ''{0}'' for prosesserings-instruksjon."},
	{ErrorMsg.STRAY_ATTRIBUTE_ERR,
	"Attributt ''{0}'' utenfor element."},
	{ErrorMsg.ILLEGAL_ATTRIBUTE_ERR,
	"Ugyldig attributt ''{0}''."},
	{ErrorMsg.CIRCULAR_INCLUDE_ERR,
	"Sirkul \00e6 import/include; stilark ''{0}'' er alt lest."},
	{ErrorMsg.RESULT_TREE_SORT_ERR,
	"Result-tre fragmenter kan ikke sorteres (<xsl:sort> elementer vil "+
	"bli ignorert). Du m\u00e5 sortere nodene mens du bygger treet."},
	{ErrorMsg.SYMBOLS_REDEF_ERR,
	"Formatterings-symboler ''{0}'' er alt definert."},
	{ErrorMsg.XSL_VERSION_ERR,
	"XSL versjon ''{0}'' er ikke st\u00f8ttet av XSLTC."},
	{ErrorMsg.CIRCULAR_VARIABLE_ERR,
	"Sirkul\00e6r variabel/parameter referanse i ''{0}''."},
	{ErrorMsg.ILLEGAL_BINARY_OP_ERR,
	"Ugyldig operator for bin\00e6rt uttrykk."},
	{ErrorMsg.ILLEGAL_ARG_ERR,
	"Ugyldig parameter i funksjons-kall."},
	{ErrorMsg.DOCUMENT_ARG_ERR,
	"Andre argument til document() m\u00e5 v\00e6re et node-sett."},
	{ErrorMsg.MISSING_WHEN_ERR,
	"Du m\u00e5 deklarere minst ett <xsl:when> element innenfor <xsl:choose>."},
	{ErrorMsg.MULTIPLE_OTHERWISE_ERR,
	"Kun ett <xsl:otherwise> element kan deklareres innenfor <xsl:choose>."},
	{ErrorMsg.STRAY_OTHERWISE_ERR,
	"<xsl:otherwise> kan kun benyttes innenfor <xsl:choose>."},
	{ErrorMsg.STRAY_WHEN_ERR,
	"<xsl:when> kan kun benyttes innenfor <xsl:choose>."},
	{ErrorMsg.WHEN_ELEMENT_ERR,	
	"Kun <xsl:when> og <xsl:otherwise> kan benyttes innenfor <xsl:choose>."},
	{ErrorMsg.UNNAMED_ATTRIBSET_ERR,
	"<xsl:attribute-set> element manger 'name' attributt."},
	{ErrorMsg.ILLEGAL_CHILD_ERR,
	"Ugyldig element."},
	{ErrorMsg.ILLEGAL_ELEM_NAME_ERR,
	"''{0}'' er ikke et gyldig navn for et element."},
	{ErrorMsg.ILLEGAL_ATTR_NAME_ERR,
	"''{0}'' er ikke et gyldig navn for et attributt."},
	{ErrorMsg.ILLEGAL_TEXT_NODE_ERR,
	"Du kan ikke plassere tekst utenfor et <xsl:stylesheet> element."},
	{ErrorMsg.SAX_PARSER_CONFIG_ERR,
	"JAXP parser er ikke korrekt konfigurert."},
	{ErrorMsg.INTERNAL_ERR,
	"XSLTC-intern feil: ''{0}''"},
	{ErrorMsg.UNSUPPORTED_XSL_ERR,
	"St\u00f8tter ikke XSL element ''{0}''."},
	{ErrorMsg.UNSUPPORTED_EXT_ERR,
	"XSLTC st\u00f8tter ikke utvidet funksjon ''{0}''."},
	{ErrorMsg.MISSING_XSLT_URI_ERR,
	"Dette dokumentet er ikke et XSL stilark "+
	"(xmlns:xsl='http://www.w3.org/1999/XSL/Transform' er ikke deklarert)."},
	{ErrorMsg.MISSING_XSLT_TARGET_ERR,
	"Kan ikke finne stilark ved navn ''{0}'' i dette dokumentet."},
	{ErrorMsg.NOT_IMPLEMENTED_ERR,
	"Ikke implementert/gjenkjent: ''{0}''."},
	{ErrorMsg.NOT_STYLESHEET_ERR,
	"Dokumentet inneholder ikke et XSL stilark"},
	{ErrorMsg.ELEMENT_PARSE_ERR,
	"Kan ikke tolke element ''{0}''"},
	{ErrorMsg.KEY_USE_ATTR_ERR,
	"'use'-attributtet i <xsl:key> m\u00e5 v\00e6re node, node-sett, tekst eller nummer."},
	{ErrorMsg.OUTPUT_VERSION_ERR,
	"Det genererte XML dokumentet m\u00e5 gis versjon 1.0"},
	{ErrorMsg.ILLEGAL_RELAT_OP_ERR,
	"Ugyldig operator for relasjons-uttrykk."},
	{ErrorMsg.ATTRIBSET_UNDEF_ERR,
	"Finner ikke <xsl:attribute-set> element med navn ''{0}''."},
	{ErrorMsg.ATTR_VAL_TEMPLATE_ERR,
	"Kan ikke tolke attributt ''{0}''."},
	{ErrorMsg.UNKNOWN_SIG_TYPE_ERR,
	"Ukjent data-type i signatur for klassen ''{0}''."},
	{ErrorMsg.DATA_CONVERSION_ERR,
	"Kan ikke oversette mellom data-type ''{0}'' og ''{1}''."},

	{ErrorMsg.NO_TRANSLET_CLASS_ERR,
	"Dette Templates objected inneholder ingen translet klasse definisjon."},
	{ErrorMsg.NO_MAIN_TRANSLET_ERR,
	"Dette Templates objected inneholder ingen klasse ved navn ''{0}''."},
	{ErrorMsg.TRANSLET_CLASS_ERR,
	"Kan ikke laste translet-klasse ''{0}''."},
	{ErrorMsg.TRANSLET_OBJECT_ERR,
	"Translet klassen er lastet man kan instansieres."},
	{ErrorMsg.ERROR_LISTENER_NULL_ERR,
	"ErrorListener for ''{0}'' fors\u00f8kt satt til 'null'."},
	{ErrorMsg.JAXP_UNKNOWN_SOURCE_ERR,
	"Kun StreamSource, SAXSource og DOMSOurce er st\u00f8ttet av XSLTC"},
	{ErrorMsg.JAXP_NO_SOURCE_ERR,
	"Source objekt sendt til ''{0}'' har intet innhold."},
	{ErrorMsg.JAXP_COMPILE_ERR,
	"Kan ikke kompilere stilark."},
	{ErrorMsg.JAXP_INVALID_ATTR_ERR,
	"TransformerFactory gjenkjenner ikke attributtet ''{0}''."},
	{ErrorMsg.JAXP_SET_RESULT_ERR,
	"setResult() m\u00e5 kalles f\u00f8r startDocument()."},
	{ErrorMsg.JAXP_NO_TRANSLET_ERR,
	"Transformer objektet inneholder ikken noen translet instans."},
	{ErrorMsg.JAXP_NO_HANDLER_ERR,
	"Ingen 'handler' er satt for \u00e5 ta imot generert dokument."},
	{ErrorMsg.JAXP_NO_RESULT_ERR,
	"Result objektet sendt til ''{0}'' er ikke gyldig."},
	{ErrorMsg.JAXP_UNKNOWN_PROP_ERR,
	"Fors\u00f8ker \u00e5 lese ugyldig attributt ''{0}'' fra Transformer."},
	{ErrorMsg.SAX2DOM_ADAPTER_ERR,
	"Kan ikke instansiere SAX2DOM adapter: ''{0}''."},
	{ErrorMsg.XSLTC_SOURCE_ERR,
	"XSLTCSource.build() kalt uten at 'systemId' er definert."},

	{ErrorMsg.COMPILE_STDIN_ERR,
	"Du kan ikke bruke -i uten \u00e5 ogs\u00e5 angi klasse-navn med -o."},
	{ErrorMsg.COMPILE_USAGE_STR,
	"Bruk:\n" + 
	"   xsltc [-o <klasse>] [-d <katalog>] [-j <arkiv>]\n"+
	"         [-p <pakke>] [-x] [-s] [-u] <stilark>|-i\n\n"+
	"   Der:  <klasse> er navnet du vil gi den kompilerte java klassen.\n"+
	"         <stilark> er ett eller flere XSL stilark, eller dersom -u\n"+
	"         er benyttet, en eller flere URL'er til stilark.\n"+
	"         <katalog> katalog der klasse filer vil plasseres.\n"+
	"         <arkiv> er en JAR-fil der klassene vil plasseres\n"+
	"         <pakke> er an Java 'package' klassene vil legges i.\n\n"+
	"   Annet:\n"+
	"         -i tvinger kompilatoren til \u00e5 lese fra stdin.\n"+
	"         -o ignoreres dersom flere enn ett silark kompileres.\n"+
	"         -x sl\u00e5r p\u00e5 debug meldinger.\n"+
	"         -s blokkerer alle kall til System.exit()."},
	{ErrorMsg.TRANSFORM_USAGE_STR,
	"Bruk: \n" +
	"   xslt  [-j <arkiv>] {-u <url> | <dokument>} <klasse>\n"+
	"         [<param>=<verdi> ...]\n\n" +
	"   Der:  <dokument> er XML dokumentet som skal behandles.\n" +
	"         <url> er en URL til XML dokumentet som skal behandles.\n" +
	"         <klasse> er Java klassen som skal benyttes.\n" +
	"         <arkiv> er en JAR-fil som klassen leses fra.\n"+
	"   Annet:\n"+
	"         -x sl\u00e5r p\u00e5 debug meldinger.\n"+
	"         -s blokkerer alle kall til System.exit()."},

	{ErrorMsg.STRAY_SORT_ERR,
	"<xsl:sort> kan bare brukes under <xsl:for-each> eller <xsl:apply-templates>."},
	{ErrorMsg.UNSUPPORTED_ENCODING,
	"Karaktersett ''{0}'' er ikke st\u00f8ttet av denne JVM."},
	{ErrorMsg.SYNTAX_ERR,
	"Syntax error in ''{0}''."}  // TODO: How do you say "syntax error" in norwegian?
    };
    }
}
