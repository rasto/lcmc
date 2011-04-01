/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.muse.util.xml;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.namespace.QName;

/**
 *
 * XsdUtils is a collection of utility methods related to XML Schema.
 *
 * @author Dan Jemiolo (danj)
 *
 */

public class XsdUtils
{
    /**
     * 
     * The XML Schema namespace URI.
     * 
     */
    public static final String NAMESPACE_URI = 
        "http://www.w3.org/2001/XMLSchema";

    public static final String PREFIX = "xsd";
    
    //
    // Used to format dates in the XMLSchema dateTime format
    //
    private static DateFormat _FORMATTER = 
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    public static final QName ANY_TYPE_QNAME = 
        new QName(NAMESPACE_URI, "anyType", PREFIX);
    
    public static final QName ANY_URI_QNAME = 
        new QName(NAMESPACE_URI, "anyURI", PREFIX);
    
    //
    // below are the names for some XSD built-in types
    //
    
    public static final QName ATTRIBUTE_GROUP_QNAME = 
        new QName(NAMESPACE_URI, "attributeGroup", PREFIX);
    
    public static final QName ATTRIBUTE_QNAME = 
        new QName(NAMESPACE_URI, "attribute", PREFIX);

    public static final QName BOOLEAN_QNAME = 
        new QName(NAMESPACE_URI, "boolean", PREFIX);

    public static final QName COMPLEX_TYPE_QNAME = 
        new QName(NAMESPACE_URI, "complexType", PREFIX);

    public static final QName COMPLEX_CONTENT_QNAME = 
        new QName(NAMESPACE_URI, "complexContent", PREFIX);

    public static final QName EXTENSION_QNAME = 
        new QName(NAMESPACE_URI, "extension", PREFIX);

    public static final QName DATE_QNAME = 
        new QName(NAMESPACE_URI, "date", PREFIX);

    public static final QName DATE_TIME_QNAME = 
        new QName(NAMESPACE_URI, "dateTime", PREFIX);

    public static final QName DOUBLE_QNAME = 
        new QName(NAMESPACE_URI, "double", PREFIX);

    public static final QName DURATION_QNAME = 
        new QName(NAMESPACE_URI, "duration", PREFIX);

    public static final QName ELEMENT_QNAME = 
        new QName(NAMESPACE_URI, "element", PREFIX);

    public static final QName FLOAT_QNAME = 
        new QName(NAMESPACE_URI, "float", PREFIX);

    public static final QName IMPORT_QNAME = 
        new QName(NAMESPACE_URI, "import", PREFIX);

    public static final QName INCLUDE_QNAME = 
        new QName(NAMESPACE_URI, "include", PREFIX);

    public static final QName INT_QNAME = 
        new QName(NAMESPACE_URI, "int", PREFIX);

    public static final QName INTEGER_QNAME = 
        new QName(NAMESPACE_URI, "integer", PREFIX);
    
    //
    // below are the common names for XSD tags
    //

    public static final QName LONG_QNAME = 
        new QName(NAMESPACE_URI, "long", PREFIX);

    public static final String MAX_OCCURS = "maxOccurs";
    
    public static final String MIN_OCCURS = "minOccurs";
    
    public static final long MS_IN_A_DAY = 86400 * 1000;
    
    public static final long MS_IN_A_HOUR = 3600 * 1000;
    
    public static final long MS_IN_A_MINUTE = 60 * 1000;
    
    public static final long MS_IN_A_MONTH = 2629744 * 1000;
    
    public static final long MS_IN_A_YEAR = 31556926 * 1000;
    
    public static final String NAME = "name";
    
    public static final String NAMESPACE = "namespace";

    public static final QName NC_NAME_QNAME = 
        new QName(NAMESPACE_URI, "NCName", PREFIX);

    public static final QName NIL_QNAME = 
        new QName(NAMESPACE_URI, "nil", PREFIX);

    public static final String NILLABLE = "nillable";
    
    public static final QName QNAME_QNAME = 
        new QName(NAMESPACE_URI, "QName", PREFIX);
    
    public static final String REF = "ref";
    
    public static final String BASE = "base";

    public static final String SCHEMA_LOCATION = "schemaLocation";
    
    public static final QName SCHEMA_QNAME = 
        new QName(NAMESPACE_URI, "schema", PREFIX);
    
    public static final QName SEQUENCE_QNAME = 
        new QName(NAMESPACE_URI, "sequence", PREFIX);
    
    //
    // millisecond values used when serializing xsd:durations
    //
    
    public static final QName SHORT_QNAME = 
        new QName(NAMESPACE_URI, "short", PREFIX);
    public static final QName SIMPLE_TYPE_QNAME = 
        new QName(NAMESPACE_URI, "simpleType", PREFIX);
    public static final QName STRING_QNAME = 
        new QName(NAMESPACE_URI, "string", PREFIX);
    public static final String TYPE = "type";
    public static final String UNBOUNDED = "unbounded";
    
    /**
     * 
     * @param before
     * @param after
     * 
     * @return A valid xsd:duration value that represents the time between 
     *         the two dates. If the second time is before the first one, 
     *         the duration will have a '-' prefix, meaning it is negative.
     *
     */
    public static String getDuration(Date before, Date after)
    {
        long beforeTime = before.getTime();
        long afterTime = after.getTime();
        long totalTime = afterTime - beforeTime;
        
        return getDuration(totalTime);
    }
    
    /**
     * 
     * @param totalTime
     * 
     * @return A valid xsd:duration value that represents the given time. If 
     *         the value is negative, the duration will have a '-' prefix, 
     *         meaning it is negative.
     *
     */    
    public static String getDuration(long totalTime)
    {
        StringBuffer duration = new StringBuffer();
        
        if (totalTime < 0)
            duration.append('-');
        
        duration.append('P');
        
        long years = totalTime / MS_IN_A_YEAR;
        
        if (years > 0)
        {
            duration.append(years);
            duration.append('Y');
            totalTime -= years * MS_IN_A_YEAR;
        }
        
        long months = totalTime / MS_IN_A_MONTH;
        
        if (months > 0)
        {
            duration.append(months);
            duration.append('M');
            totalTime -= months * MS_IN_A_MONTH;
        }
        
        long days = totalTime / MS_IN_A_DAY;
        
        if (days > 0)
        {
            duration.append(days);
            duration.append('D');
            totalTime -= days * MS_IN_A_DAY;
        }
        
        //
        // there must be at least one second (1000 ms) left for us 
        // to have a time (T) value
        //
        if (totalTime >= 1000)
        {
            duration.append('T');
            
            long hours = totalTime / MS_IN_A_HOUR;
            
            if (hours > 0)
            {
                duration.append(hours);
                duration.append('H');
                totalTime -= hours * MS_IN_A_HOUR;
            }
            
            long minutes = totalTime / MS_IN_A_MINUTE;
            
            if (minutes > 0)
            {
                duration.append(minutes);
                duration.append('M');
                totalTime -= minutes * MS_IN_A_MINUTE;
            }
            
            double seconds = ((double)totalTime) / 1000;
            
            if (seconds > 1.0)
            {
                duration.append(seconds);
                duration.append('S');
            }
        }
        
        return duration.toString();
    }
    
    /**
     * 
     * Parses an xsd:duration string into its time value (in millseconds). The 
     * format of a xsd:duration is:
     * <br><br>
     * (-)PaYbMcD(TxHyMzS)
     * <br><br>
     * Where a, b, and c are the years, months, and days, respectively. The 
     * time value (following the 'T') is optional. Its values - x, y, and z - 
     * are the hours, minutes, and seconds, respectively. The value of the 
     * duration may be negative.
     * 
     * @param durationString
     *        A valid xsd:duration string.
     * 
     * @return The time, in milliseconds, represented by the given xsd:duration.
     *
     */
    public static long getDuration(String durationString)
    {
        //
        // I'm sure there's a more elegant way to do this using regex groups, 
        // but I don't have time for that. What is with this format, anyway? 
        // Milliseconds weren't good enough for these people?
        //
        
        int years = 0;
        int months = 0;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        double seconds = 0.0;
        
        boolean isNegative = false;
        int start = 0;
        
        if (durationString.startsWith("-"))
        {
            isNegative = true;
            ++start;
        }
        
        if (durationString.indexOf('P') != start)
            throw new IllegalArgumentException(durationString);
        
        ++start;
        
        int end = durationString.indexOf('Y');
        
        if (end >= 0)
        {
            String yearsString = durationString.substring(start, end);
            years = Integer.parseInt(yearsString);
            start = end + 1;
        }
        
        //
        // the xsd:duration format uses M for month AND minutes, so we 
        // have to check to make sure this M is for months. if there's 
        // no T, or T is after the M, then it's a month value.
        //
        end = durationString.indexOf('M');
        int time = durationString.indexOf('T');
        
        if (end >= 0 && (time == -1 || time > end))
        {
            String monthsString = durationString.substring(start, end);
            months = Integer.parseInt(monthsString);
            start = end  + 1;
        }
        
        end = durationString.indexOf('D');
        
        if (end >= 0)
        {
            String daysString = durationString.substring(start, end);
            days = Integer.parseInt(daysString);
            start = end  + 1;
        }
        
        if (start != durationString.length())
        {
            if (durationString.charAt(start) != 'T')
                throw new IllegalArgumentException(durationString);
            
            ++start;
            
            if (start == durationString.length())
                throw new IllegalArgumentException(durationString);
            
            end = durationString.indexOf('H');
            
            if (end >= 0)
            {
                String hoursString = durationString.substring(start, end);
                hours = Integer.parseInt(hoursString);
                start = end  + 1;
            }
            
            end = durationString.indexOf('M');
            
            if (end >= 0)
            {
                String minString = durationString.substring(start, end);
                minutes = Integer.parseInt(minString);
                start = end  + 1;
            }
            
            end = durationString.indexOf('S');
            
            if (end >= 0)
            {
                String secString = durationString.substring(start, end);
                seconds = Double.parseDouble(secString);
            }
        }
        
        double totalMilliseconds = 
            (years * MS_IN_A_YEAR) + (months * MS_IN_A_MONTH) + (days * MS_IN_A_DAY) + 
            (hours * MS_IN_A_HOUR) + (minutes * MS_IN_A_MINUTE) + (seconds * 1000);
        
        if (isNegative)
            totalMilliseconds *= -1;
        
        return (long)totalMilliseconds;
    }
    
    /**
     * 
     * Parses the given XSD date string into a Date object.
     *
     * @param dateTimeString
     *        The text description of a date, in standard XSD format.
     * 
     * @return The Date equivalent of the date string.
     * 
     * @throws ParseException
     *         <ul>
     *         <li>If the format of the date string was incorrect.</li>
     *         </ul>
     *
     */
    public static Date getLocalTime(String dateTimeString)
        throws ParseException
    {
        return _FORMATTER.parse(dateTimeString);
    }
    
    /**
     * 
     * This is a convenience method that returns the text version of the 
     * current time, in standard XSD format.
     *
     * @see #getLocalTimeString(Date)
     *
     */
    public static String getLocalTimeString()
    {
        return getLocalTimeString(new Date());
    }
    
    /**
     * 
     * Returns the text version of the given date, in standard XSD format. 
     * It will include a +/- suffix that adds or subtracts hours from the 
     * UTC time in order to express local time.
     *
     * @param date
     *        The date to serialize into text.
     * 
     * @return A string that describes the given date, in XSD format, with 
     *         a +/- suffix to denote the local time.
     *
     */
    public static String getLocalTimeString(Date date)
    {
        //
        // if we leave this date w/o a +N:00 postfix, calling code 
        // will assume it is UTC
        //
        String dateWithoutTimeZone  = _FORMATTER.format(date);
        
        //
        // figure out whether we're before (-) or after (+) UTC
        //
        TimeZone timeZone = _FORMATTER.getTimeZone();
        long offset = timeZone.getOffset(date.getTime());
        
        char sign = offset < 0 ? '-' : '+';
        offset = Math.abs(offset);
        
        //
        // figure out what comes after the +/-
        // 
        long hours = offset / 3600000;
        long minutes = (offset % 3600000) / 60000;
        
        //
        // add the +/- suffix and call it a day
        //        
        int length = dateWithoutTimeZone.length() + 6;
        StringBuffer buffer = new StringBuffer(length);
        
        DecimalFormat twoDigits = new DecimalFormat("00");
        
        buffer.append(dateWithoutTimeZone);
        buffer.append(sign);
        buffer.append(twoDigits.format(hours));
        buffer.append(':');
        buffer.append(twoDigits.format(minutes));
        
        return buffer.toString();
    }
}
