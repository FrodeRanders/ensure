/*
 * Copyright (C) 2012-2014 Frode Randers
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

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/*
 * Description of TimeDelta
 * <p/>
 * <p/>
 * Created by Frode Randers at 2013-01-10 00:56
 */
public class TimeDelta {

    private final static BigInteger MILLISECONDS_MAX = new BigInteger("1000");
    private final static BigInteger SECONDS_MAX = MILLISECONDS_MAX.multiply(new BigInteger("60"));
    private final static BigInteger MINUTES_MAX = SECONDS_MAX.multiply(new BigInteger("60"));
    private final static BigInteger HOURS_MAX = MINUTES_MAX.multiply(new BigInteger("24"));
    private final static BigInteger DAYS_MAX = HOURS_MAX.multiply(new BigInteger("30")); // approx
    private final static BigInteger MONTHS_MAX = DAYS_MAX.multiply(new BigInteger("12"));

    private final static BigInteger SECONDS_DIV = MILLISECONDS_MAX;
    private final static BigInteger MINUTES_DIV = SECONDS_MAX;
    private final static BigInteger HOURS_DIV = MINUTES_MAX;
    private final static BigInteger DAYS_DIV = HOURS_MAX;
    private final static BigInteger MONTHS_DIV = DAYS_MAX; // approx
    private final static BigInteger YEARS_DIV = MONTHS_MAX;

    private static NumberFormat dec2Format = NumberFormat.getNumberInstance();

    public static String asHumanApproximate(BigInteger milliseconds) {
        dec2Format.setMaximumFractionDigits(0);

        if (milliseconds.compareTo(BigInteger.ZERO) == 0)
            return "";

        String result;
        if (milliseconds.compareTo(MILLISECONDS_MAX) < 0) {
            int value = milliseconds.intValue();
            result = value + "ms ";
        } else if (milliseconds.compareTo(SECONDS_MAX) < 0) {
            BigInteger[] quot = milliseconds.divideAndRemainder(SECONDS_DIV);
            int value = quot[0].intValue();
            result = dec2Format.format(value) + "s " + asHumanApproximate(quot[1]);
        } else if (milliseconds.compareTo(MINUTES_MAX) < 0) {
            BigInteger[] quot = milliseconds.divideAndRemainder(MINUTES_DIV);
            int value = quot[0].intValue();
            result = dec2Format.format(value) + "min" + (value == 1 ? " " : "s ") + asHumanApproximate(quot[1]);
        } else if (milliseconds.compareTo(HOURS_MAX) < 0) {
            BigInteger[] quot = milliseconds.divideAndRemainder(HOURS_DIV);
            int value = quot[0].intValue();
            result = dec2Format.format(value) + "hour" + (value == 1 ? " " : "s ") + asHumanApproximate(quot[1]);
        } else if (milliseconds.compareTo(DAYS_MAX) < 0) {
            BigInteger[] quot = milliseconds.divideAndRemainder(DAYS_DIV);
            int value = quot[0].intValue();
            result = dec2Format.format(value) + "day" + (value == 1 ? " " : "s ") + asHumanApproximate(quot[1]);
        } else if (milliseconds.compareTo(MONTHS_MAX) < 0) {
            BigInteger[] quot = milliseconds.divideAndRemainder(MONTHS_DIV);
            int value = quot[0].intValue();
            result = dec2Format.format(value) + "month" + (value == 1 ? " " : "s ") + asHumanApproximate(quot[1]);
        } else {
            BigInteger[] quot = milliseconds.divideAndRemainder(YEARS_DIV);
            int value = quot[0].intValue();
            result = dec2Format.format(value) + "year" + (value == 1 ? " " : "s ") + asHumanApproximate(quot[1]);
        }
        return result.trim();
    }
}
