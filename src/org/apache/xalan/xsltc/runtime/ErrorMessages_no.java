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
 * $Id: ErrorMessages_no.java,v 1.6 2004/12/15 17:35:45 jycli Exp $
 */

package org.apache.xalan.xsltc.runtime;

import java.util.ListResourceBundle;

/**
 * @author Morten Jorgensen
 */
public class ErrorMessages_no extends ListResourceBundle {


    // Disse feilmeldingene maa korrespondere med konstantene some er definert
    // nederst i kildekoden til BasisLibrary.
    /** Get the lookup table for error messages.   
     *
     * @return The message lookup table.
     */
    public Object[][] getContents()
    {
      return new Object[][] {
    {BasisLibrary.RUN_TIME_INTERNAL_ERR,
    "Intern programfeil i ''{0}''"},

    {BasisLibrary.RUN_TIME_COPY_ERR,
    "Programfeil under utf\u00f8ing av <xsl:copy>."},

    {BasisLibrary.DATA_CONVERSION_ERR,
    "Ugyldig konvertering av ''{0}'' fra ''{1}''."},

    {BasisLibrary.EXTERNAL_FUNC_ERR,
    "Ekstern funksjon ''{0}'' er ikke st\u00f8ttet av XSLTC."},

    {BasisLibrary.EQUALITY_EXPR_ERR,
    "Ugyldig argument i EQUALITY uttrykk."},

    {BasisLibrary.INVALID_ARGUMENT_ERR,
    "Ugyldig argument ''{0}'' i kall til ''{1}''"},

    {BasisLibrary.FORMAT_NUMBER_ERR,
    "Fors\u00f8k p\u00e5 \u00e5 formattere nummer ''{0}'' med ''{1}''."},

    {BasisLibrary.ITERATOR_CLONE_ERR,
    "Kan ikke klone iterator ''{0}''."},

    {BasisLibrary.AXIS_SUPPORT_ERR,
    "Iterator for axis ''{0}'' er ikke st\u00e8ttet."},

    {BasisLibrary.TYPED_AXIS_SUPPORT_ERR,
    "Iterator for typet axis ''{0}'' er ikke st\u00e8ttet."},

    {BasisLibrary.STRAY_ATTRIBUTE_ERR,
    "Attributt ''{0}'' utenfor element."},

    {BasisLibrary.STRAY_NAMESPACE_ERR,
    "Navnedeklarasjon ''{0}''=''{1}'' utenfor element."},

    {BasisLibrary.NAMESPACE_PREFIX_ERR,
    "Prefix ''{0}'' er ikke deklartert."},

    {BasisLibrary.DOM_ADAPTER_INIT_ERR,
	"Fors\u00f8k p\u00e5 \u00e5 instansiere DOMAdapter med feil type DOM."}

    };
    }
}
