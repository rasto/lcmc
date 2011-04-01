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
 * $Id: SerializerMessages_pl.java,v 1.2 2004/12/16 19:19:57 minchau Exp $
 */

package org.apache.xml.serializer.utils;

import java.util.ListResourceBundle;

public class SerializerMessages_pl extends ListResourceBundle {
  public Object[][] getContents() {
    Object[][] contents =  new Object[][] {
        // BAD_MSGKEY needs translation
        // BAD_MSGFORMAT needs translation
      { MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
        "Klasa szereguj\u0105ca ''{0}'' nie implementuje procedury obs\u0142ugi org.xml.sax.ContentHandler."},

      { MsgKey.ER_RESOURCE_COULD_NOT_FIND,
        "Nie mo\u017cna znale\u017a\u0107 zasobu [ {0} ].\n {1}"},

      { MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
        "Zas\u00f3b [ {0} ] nie m\u00f3g\u0142 za\u0142adowa\u0107: {1} \n {2} \n {3}"},

      { MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Wielko\u015b\u0107 bufora <=0"},

      { MsgKey.ER_INVALID_UTF16_SURROGATE,
        "Wykryto niepoprawny surogat UTF-16: {0} ?"},

      { MsgKey.ER_OIERROR,
        "B\u0142\u0105d we/wy"},

      { MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
        "Nie mo\u017cna doda\u0107 atrybutu {0} po w\u0119z\u0142ach potomnych ani przed wyprodukowaniem elementu.  Atrybut zostanie zignorowany."},

      { MsgKey.ER_NAMESPACE_PREFIX,
        "Nie zadeklarowano przestrzeni nazw dla przedrostka ''{0}''."},

        // ER_STRAY_ATTRIBUTE needs translation
      { MsgKey.ER_STRAY_NAMESPACE,
        "Deklaracja przestrzeni nazw ''{0}''=''{1}'' poza elementem."},

      { MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
        "Nie mo\u017cna za\u0142adowa\u0107 ''{0}'' (sprawd\u017a CLASSPATH), u\u017cywane s\u0105 teraz warto\u015bci domy\u015blne"},

        // ER_ILLEGAL_CHARACTER needs translation
      { MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
        "Nie mo\u017cna za\u0142adowa\u0107 pliku w\u0142a\u015bciwo\u015bci ''{0}'' dla metody wyj\u015bciowej ''{1}'' (sprawd\u017a CLASSPATH)"},

      { MsgKey.ER_INVALID_PORT,
        "Niepoprawny numer portu"},

      { MsgKey.ER_PORT_WHEN_HOST_NULL,
        "Nie mo\u017cna ustawi\u0107 portu, kiedy host jest pusty"},

      { MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
        "Host nie jest poprawnie skonstruowanym adresem"},

      { MsgKey.ER_SCHEME_NOT_CONFORMANT,
        "Schemat nie jest zgodny."},

      { MsgKey.ER_SCHEME_FROM_NULL_STRING,
        "Nie mo\u017cna ustawi\u0107 schematu z pustego ci\u0105gu znak\u00f3w"},

      { MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
        "\u015acie\u017cka zawiera niepoprawn\u0105 sekwencj\u0119 o zmienionym znaczeniu"},

      { MsgKey.ER_PATH_INVALID_CHAR,
        "\u015acie\u017cka zawiera niepoprawny znak {0}"},

      { MsgKey.ER_FRAG_INVALID_CHAR,
        "Fragment zawiera niepoprawny znak"},

      { MsgKey.ER_FRAG_WHEN_PATH_NULL,
        "Nie mo\u017cna ustawi\u0107 fragmentu, kiedy \u015bcie\u017cka jest pusta"},

      { MsgKey.ER_FRAG_FOR_GENERIC_URI,
        "Fragment mo\u017cna ustawi\u0107 tylko dla og\u00f3lnego URI"},

      { MsgKey.ER_NO_SCHEME_IN_URI,
        "Nie znaleziono schematu w URI {0}"},

      { MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
        "Nie mo\u017cna zainicjowa\u0107 URI z pustymi parametrami"},

      { MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
        "Nie mo\u017cna poda\u0107 fragmentu jednocze\u015bnie w \u015bcie\u017cce i fragmencie"},

      { MsgKey.ER_NO_QUERY_STRING_IN_PATH,
        "Tekstu zapytania nie mo\u017cna poda\u0107 w tek\u015bcie \u015bcie\u017cki i zapytania"},

      { MsgKey.ER_NO_PORT_IF_NO_HOST,
        "Nie mo\u017cna poda\u0107 portu, je\u015bli nie podano hosta"},

      { MsgKey.ER_NO_USERINFO_IF_NO_HOST,
        "Nie mo\u017cna poda\u0107 informacji o u\u017cytkowniku, je\u015bli nie podano hosta"},

      { MsgKey.ER_SCHEME_REQUIRED,
        "Schemat jest wymagany!"}

    };
    return contents;
  }
}
