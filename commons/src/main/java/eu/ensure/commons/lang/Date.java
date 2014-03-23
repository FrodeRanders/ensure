/*
 * Copyright (C) 2011-2014 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package  eu.ensure.commons.lang;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Utility functions for date and time conversions
 */
public class Date {
    public static final int dateStyle = DateFormat.SHORT;
    public static final int timeStyle = DateFormat.MEDIUM;

    /**
     * Turn a Timestamp into a string using the specified locale.
     */
    public static String time2DateString(java.sql.Timestamp time, Locale locale) {
        DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
        return df.format(time);
    }

    /**
     * Turn a Timestamp into a string using the specified locale.
     */
    public static String time2String(java.sql.Timestamp time, Locale locale) {
        DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        return df.format(time);
    }

    public static String date2String(java.util.Date date, Locale locale) {
        DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
        return df.format(date);
    }

    public static java.util.Date string2Date(String date, Locale locale)
            throws ParseException {
        DateFormat df = DateFormat.getDateInstance(dateStyle, locale);
        return df.parse(date);
    }

    public static String toPattern(Locale locale) {
        SimpleDateFormat sdf;

        sdf = (SimpleDateFormat) DateFormat.getDateInstance(dateStyle, locale);
        return sdf.toPattern();
    }

    /**
     * Validates the users inputted date value
     *
     * @param date The Date in String form
     * @param locale          Check format according to this locale
     * @return true if user inputted corect format, otherwise false
     */
    public static boolean validateDateFormat(String date, Locale locale) {
        SimpleDateFormat df;
        SimpleDateFormat sdf;
        java.text.ParsePosition pos;
        java.util.Date dbDate;

        dbDate = null;
        try {
            if (date == null || date.equals("")) {
                return false;
            }

            df = (SimpleDateFormat) DateFormat.getDateInstance(
                    dateStyle,
                    locale
            );

            sdf = new SimpleDateFormat(df.toPattern());
            pos = new java.text.ParsePosition(0);
            sdf.setLenient(false);
            dbDate = sdf.parse(date, pos);

            if (dbDate == null) {
                return false;
            } else {
                return dbDate.getTime() > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Converts the users input date value to java.sql.Date
     *
     * @param date Date to convert
     * @return The converted date in java.sql.date format
     */
    public static java.sql.Date convertDate(String date, Locale locale) {
        SimpleDateFormat df;
        java.util.Date dbDate = null;
        java.sql.Date sqldate = null;
        try {

            if (date == null || date.equals("")) {
                return null;
            }

            df = (SimpleDateFormat) DateFormat.getDateInstance(dateStyle, locale);

            SimpleDateFormat sdf = new SimpleDateFormat(df.toPattern());
            java.text.ParsePosition pos = new java.text.ParsePosition(0);
            sdf.setLenient(false);
            dbDate = sdf.parse(date, pos);
            sqldate = new java.sql.Date(dbDate.getTime());

            if (dbDate.getTime() <= 0) {
                return null;
            } else {
                return sqldate;
            }
        } catch (Exception e) {
            return null;
        }
    }
}

