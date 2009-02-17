/*
 * This file is part of DRBD Management Console by LINBIT HA-Solutions GmbH
 * written by Rasto Levrinc.
 *
 * Copyright (C) 2009, LINBIT HA-Solutions GmbH.
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

package drbd.utilities;

import javax.swing.text.BadLocationException;
import javax.swing.text.AttributeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.swing.text.DefaultStyledDocument;

/**
 * An implementation of a document where user can specify a regexp, that
 * disables entering of characters, that do not match it.
 *
 * @author Rasto Levrinc
 * @version $Id$
 *
 */
public class PatternDocument extends DefaultStyledDocument {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;
    /** Pattern object that the document should match. */
    private final Pattern pattern;

    /**
     * Prepares a new <code>PatternDocument</code> object.
     *
     * @param regexp
     *          regexp that the document should match
     */
    public PatternDocument(final String regexp) {
        super();
        pattern = Pattern.compile(regexp);
    }

    /**
     * Inserts the string if the resulting string matches the pattern.
     */
    public final void insertString(final int offs,
                                   final String s,
                                   final AttributeSet a)
    throws BadLocationException {
        try {
            final String text = getText(0, getLength());
            String texta;
            if (text.length() > 0) {
                texta = ((offs >= 0) ? text.substring(0, offs) : "")
                        + s
                        + text.substring(offs);
            } else {
                texta = s;
            }
            if (matches(texta)) {
                super.insertString(offs, s, a);
            }
        } catch (BadLocationException e) {
            return;
        }
    }

    /**
     * Returns true if the text matches the pattern.
     */
    private boolean matches(final String text) {
        final Matcher m = pattern.matcher(text);
        return m.matches();
    }
}
