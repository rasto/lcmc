/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
 * Copyright (C) 2011-2012, Rastislav Levrinc.
 *
 * DRBD Management Console is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * DRBD Management Console is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with drbd; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.utilities;

import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
// import java.util.regex.Matcher;
import java.util.Map;

import javax.swing.text.DefaultStyledDocument;

/**
 * An implementation of a document where user can specify a regexp, that
 * disables entering of characters, that do not match it.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public final class PatternDocument extends DefaultStyledDocument {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PatternDocument.class);
    /** Abbreviations, from one character to the string, e.g. when user presses
     * i, the word infinity will be written. */
    private final Map<String, String> abbreviations;

    /**
     * Prepares a new {@code PatternDocument} object.
     *
     * @param regexp
     *          regexp that the document should match
     */
    public PatternDocument(final String regexp) {
        super();
        abbreviations = null;
    }

    /** Prepares a new {@code PatternDocument} object. */
    public PatternDocument(final String regexp,
                           final Map<String, String> abbreviations) {
        super();
        this.abbreviations = abbreviations;
    }


    /** Inserts the string if the resulting string matches the pattern. */
    @Override
    public void insertString(final int offs, String str, final AttributeSet a) {
        try {
            final String text = getText(0, getLength());
            if (abbreviations != null && abbreviations.containsKey(str)) {
                str = abbreviations.get(str);
            }
            final String texta;
            if (text.isEmpty()) {
                texta = str;
            } else {
                texta = ((offs >= 0) ? text.substring(0, offs) : "")
                        + str
                        + text.substring(offs);
            }
            if (matches(texta)) {
                super.insertString(offs, str, a);
            }
        } catch (final BadLocationException e) {
            LOG.appError("insertString: bad location exception", e);
        }
    }

    /** Returns true if the text matches the pattern. */
    private boolean matches(final String text) {
        return true; // TODO: disabled, because it does not work good
        //final Matcher m = pattern.matcher(text);
        //return m.matches();
    }
}
