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
 * $Id: SerializerMessages_hu.java,v 1.2 2004/12/16 19:19:57 minchau Exp $
 */

package org.apache.xml.serializer.utils;

import java.util.ListResourceBundle;

public class SerializerMessages_hu extends ListResourceBundle {
  public Object[][] getContents() {
    Object[][] contents =  new Object[][] {
        // BAD_MSGKEY needs translation
        // BAD_MSGFORMAT needs translation
      { MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
        "A(z) ''{0}'' serializer oszt\u00e1ly nem val\u00f3s\u00edtja meg az org.xml.sax.ContentHandler funkci\u00f3t."},

      { MsgKey.ER_RESOURCE_COULD_NOT_FIND,
        "A(z) [ {0} ] er\u0151forr\u00e1s nem tal\u00e1lhat\u00f3.\n {1}"},

      { MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
        "Az er\u0151forr\u00e1st [ {0} ] nem lehet bet\u00f6lteni: {1} \n {2} \n {3}"},

      { MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Pufferm\u00e9ret <= 0"},

      { MsgKey.ER_INVALID_UTF16_SURROGATE,
        "\u00c9rv\u00e9nytelen UTF-16 helyettes\u00edt\u00e9s: {0} ?"},

      { MsgKey.ER_OIERROR,
        "IO hiba"},

      { MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
        "Nem lehet {0} attrib\u00fatumat felvenni a gyermek node-ok ut\u00e1n vagy miel\u0151tt egy elem l\u00e9trej\u00f6nne.  Az attrib\u00fatum figyelmen k\u00edv\u00fcl marad."},

      { MsgKey.ER_NAMESPACE_PREFIX,
        "A(z) ''{0}'' el\u0151tag n\u00e9vtere nem defini\u00e1lt."},

        // ER_STRAY_ATTRIBUTE needs translation
      { MsgKey.ER_STRAY_NAMESPACE,
        "A(z) ''{0}''=''{1}'' n\u00e9vt\u00e9r-deklar\u00e1ci\u00f3 k\u00edv\u00fcl esik az elemen."},

      { MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
        "Nem lehet bet\u00f6lteni ''{0}''-t (ellen\u0151rizze a CLASSPATH be\u00e1ll\u00edt\u00e1st), az alap\u00e9rtelmez\u00e9seket haszn\u00e1lom"},

        // ER_ILLEGAL_CHARACTER needs translation
      { MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
        "Nem lehet bet\u00f6lteni a(z) ''{0}'' tulajdons\u00e1g-f\u00e1jlt a(z) ''{1}''  (ellen\u0151rizze a CLASSPATH be\u00e1ll\u00edt\u00e1st)"},

      { MsgKey.ER_INVALID_PORT,
        "\u00c9rv\u00e9nytelen portsz\u00e1m"},

      { MsgKey.ER_PORT_WHEN_HOST_NULL,
        "A port-t nem \u00e1ll\u00edthatja be, ha a host null"},

      { MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
        "A host nem j\u00f3l form\u00e1zott c\u00edm"},

      { MsgKey.ER_SCHEME_NOT_CONFORMANT,
        "A s\u00e9ma nem megfelel\u0151."},

      { MsgKey.ER_SCHEME_FROM_NULL_STRING,
        "Nem lehet be\u00e1ll\u00edtani a s\u00e9m\u00e1t null karakterl\u00e1ncb\u00f3l"},

      { MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
        "Az el\u00e9r\u00e9i \u00fat \u00e9rv\u00e9nytelen eszk\u00e9pszekvenci\u00e1t tartalmaz"},

      { MsgKey.ER_PATH_INVALID_CHAR,
        "Az el\u00e9r\u00e9si \u00fat \u00e9rv\u00e9nytelen karaktert tartalmaz: {0}"},

      { MsgKey.ER_FRAG_INVALID_CHAR,
        "A darab \u00e9rv\u00e9nytelen karaktert tartalmaz"},

      { MsgKey.ER_FRAG_WHEN_PATH_NULL,
        "A darabot csak nem \u00e1ll\u00edthatja be, h\u00ed az el\u00e9r\u00e9si \u00fat null"},

      { MsgKey.ER_FRAG_FOR_GENERIC_URI,
        "Darabot csak egy \u00e1ltal\u00e1nos URI-hoz \u00e1ll\u00edthat be"},

      { MsgKey.ER_NO_SCHEME_IN_URI,
        "Nem tal\u00e1lhat\u00f3 s\u00e9ma az URI-ban: {0}"},

      { MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
        "Nem inicializ\u00e1lhatja az URI-t \u00fcres param\u00e9terekkel"},

      { MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
        "Darabot nem adhat meg sem az el\u00e9r\u00e9si \u00fatban sem a darabban"},

      { MsgKey.ER_NO_QUERY_STRING_IN_PATH,
        "Lek\u00e9rdez\u00e9si karakterl\u00e1ncot nem adhat meg el\u00e9r\u00e9si \u00fatban \u00e9s lek\u00e9rdez\u00e9si karakterl\u00e1ncban"},

      { MsgKey.ER_NO_PORT_IF_NO_HOST,
        "Nem adhat meg port-ot, ha nem adott meg host-ot"},

      { MsgKey.ER_NO_USERINFO_IF_NO_HOST,
        "Nem adhat meg userinfo-t, ha nem adott meg host-ot"},

      { MsgKey.ER_SCHEME_REQUIRED,
        "S\u00e9m\u00e1ra sz\u00fcks\u00e9g van!"}

    };
    return contents;
  }
}
