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
 * $Id: SerializerMessages_tr.java,v 1.2 2004/12/16 19:19:57 minchau Exp $
 */

package org.apache.xml.serializer.utils;

import java.util.ListResourceBundle;

public class SerializerMessages_tr extends ListResourceBundle {
  public Object[][] getContents() {
    Object[][] contents =  new Object[][] {
        // BAD_MSGKEY needs translation
        // BAD_MSGFORMAT needs translation
      { MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
        "Diziselle\u015ftirici s\u0131n\u0131f\u0131 ''{0}'' org.xml.sax.ContentHandler i\u015flevini uygulam\u0131yor."},

      { MsgKey.ER_RESOURCE_COULD_NOT_FIND,
        "Kaynak [ {0} ] bulunamad\u0131.\n {1}"},

      { MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
        "Kaynak [ {0} ] y\u00fckleyemedi: {1} \n {2} \n {3}"},

      { MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
        "Arabellek b\u00fcy\u00fckl\u00fc\u011f\u00fc <=0"},

      { MsgKey.ER_INVALID_UTF16_SURROGATE,
        "UTF-16 yerine kullan\u0131lan de\u011fer ge\u00e7ersiz: {0} ?"},

      { MsgKey.ER_OIERROR,
        "G\u00c7 hatas\u0131"},

      { MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
        "Alt d\u00fc\u011f\u00fcmlerden sonra ya da bir \u00f6\u011fe \u00fcretilmeden \u00f6nce {0} \u00f6zniteli\u011fi eklenemez. \u00d6znitelik yoksay\u0131lacak."},

      { MsgKey.ER_NAMESPACE_PREFIX,
        "''{0}'' \u00f6nekine ili\u015fkin ad alan\u0131 bildirilmedi."},

        // ER_STRAY_ATTRIBUTE needs translation
      { MsgKey.ER_STRAY_NAMESPACE,
        "''{0}''=''{1}'' ad alan\u0131 bildirimi \u00f6\u011fenin d\u0131\u015f\u0131nda."},

      { MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
        "''{0}'' y\u00fcklenemedi (CLASSPATH de\u011fi\u015fkeninizi inceleyin), yaln\u0131zca varsay\u0131lanlar kullan\u0131l\u0131yor"},

        // ER_ILLEGAL_CHARACTER needs translation
      { MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
        "''{1}'' \u00e7\u0131k\u0131\u015f y\u00f6ntemi i\u00e7in ''{0}'' \u00f6zellik dosyas\u0131 y\u00fcklenemedi (CLASSPATH de\u011fi\u015fkenini inceleyin)"},

      { MsgKey.ER_INVALID_PORT,
        "Kap\u0131 numaras\u0131 ge\u00e7ersiz"},

      { MsgKey.ER_PORT_WHEN_HOST_NULL,
        "Anasistem bo\u015f de\u011ferliyken kap\u0131 tan\u0131mlanamaz"},

      { MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
        "Anasistem do\u011fru bi\u00e7imli bir adres de\u011fil"},

      { MsgKey.ER_SCHEME_NOT_CONFORMANT,
        "\u015eema uyumlu de\u011fil."},

      { MsgKey.ER_SCHEME_FROM_NULL_STRING,
        "Bo\u015f de\u011ferli dizgiden \u015fema tan\u0131mlanamaz"},

      { MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
        "Yol ge\u00e7ersiz ka\u00e7\u0131\u015f dizisi i\u00e7eriyor"},

      { MsgKey.ER_PATH_INVALID_CHAR,
        "Yol ge\u00e7ersiz karakter i\u00e7eriyor: {0}"},

      { MsgKey.ER_FRAG_INVALID_CHAR,
        "Par\u00e7a ge\u00e7ersiz karakter i\u00e7eriyor"},

      { MsgKey.ER_FRAG_WHEN_PATH_NULL,
        "Yol bo\u015f de\u011ferliyken par\u00e7a tan\u0131mlanamaz"},

      { MsgKey.ER_FRAG_FOR_GENERIC_URI,
        "Par\u00e7a yaln\u0131zca soysal URI i\u00e7in tan\u0131mlanabilir"},

      { MsgKey.ER_NO_SCHEME_IN_URI,
        "URI i\u00e7inde \u015fema bulunamad\u0131: {0}"},

      { MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
        "Bo\u015f de\u011fi\u015ftirgelerle URI kullan\u0131ma haz\u0131rlanamaz"},

      { MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
        "Par\u00e7a hem yolda, hem de par\u00e7ada belirtilemez"},

      { MsgKey.ER_NO_QUERY_STRING_IN_PATH,
        "Yol ve sorgu dizgisinde sorgu dizgisi belirtilemez"},

      { MsgKey.ER_NO_PORT_IF_NO_HOST,
        "Anasistem belirtilmediyse kap\u0131 belirtilemez"},

      { MsgKey.ER_NO_USERINFO_IF_NO_HOST,
        "Anasistem belirtilmediyse kullan\u0131c\u0131 bilgisi belirtilemez"},

      { MsgKey.ER_SCHEME_REQUIRED,
        "\u015eema gerekli!"}

    };
    return contents;
  }
}
