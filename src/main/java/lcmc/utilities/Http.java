/*
 * This file is part of LCMC written by Rasto Levrinc.
 *
 * Copyright (C) 2013, Rastislav Levrinc.
 *
 * The LCMC is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * The LCMC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LCMC; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package lcmc.utilities;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides Http functions.
 *
 * @author Rasto Levrinc
 *
 */
public final class Http {
    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(Http.class);
    static final String URL_STRING = "http://lcmc.sourceforge.net/cgi-bin/exc";
    static final String ENCODING = "UTF-8";
    /** Private constructor, cannot be instantiated. */
    private Http() {
        /* Cannot be instantiated. */
    }

    public static void post(final String from, final String exception) {
        final URL url;
        try {
            url = new URL(URL_STRING);
        } catch (final MalformedURLException ex) {
            LOG.appWarning("post: malformed URL: " + ex.getMessage());
            return;
        }

        final HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (final IOException ex) {
            LOG.appWarning("post: could not connect: " + ex.getMessage());
            return;
        }

        conn.setUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);

        try {
            conn.setRequestMethod("POST");
        } catch (final ProtocolException ex) {
            LOG.appWarning("post: protocol error: " + ex.getMessage());
            return;
        }

        try {
            conn.connect();
        } catch (final IOException ex) {
            LOG.appWarning("post: connection error: " + ex.getMessage());
            return;
        }

        DataOutputStream output = null;

        try {
            output = new DataOutputStream(conn.getOutputStream());
        } catch (final IOException ex) {
            LOG.appWarning("post: error opening for writing: " + ex.getMessage());
        }
        //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        final Map<String, String> params = new HashMap<String, String>();
        params.put("f", from);
        params.put("e", exception);


        try {
            output.writeBytes(getPostParams(params));
            output.flush();
            output.close();
        } catch (final IOException ex) {
            LOG.appWarning("post: error writing: " + ex.getMessage());
            return;
        }

        /* Response */
        try {
            final BufferedReader input = new BufferedReader(
                                  new InputStreamReader(conn.getInputStream()));
            String str;
            while (null != ((str = input.readLine()))) {
                LOG.info("post: " + str);
            }
            input.close();
        } catch (final IOException ex) {
            LOG.appWarning("post: error reading: " + ex.getMessage());
        }
    }

    private static String getPostParams(final Map<String, String> params)
        throws UnsupportedEncodingException {
        String postParams = "";
        String delim = "";
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            postParams += delim + entry.getKey() + '='
                        + URLEncoder.encode(entry.getValue(), ENCODING);
            delim = "&";
        }
        return postParams;
    }
}
