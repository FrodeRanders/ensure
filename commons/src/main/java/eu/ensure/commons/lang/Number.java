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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

/*
 * Description of Number
 * <p/>
 * <p/>
 * Created by Frode Randers at 2012-02-29 10:10
 */
public class Number {

    private final static long BYTES_MAX = 2048L;
    private final static long KB_MAX = BYTES_MAX * 1024L;
    private final static long MB_MAX = KB_MAX * 1024L;
    private final static long GB_MAX = MB_MAX * 1024L;
    private final static long TB_MAX = GB_MAX * 1024L;
    private final static double KB_DIV = 1024D;
    private final static double MB_DIV = KB_DIV * 1024D;
    private final static double GB_DIV = MB_DIV * 1024D;
    private final static double TB_DIV = GB_DIV * 1024D;

    private static NumberFormat dec2Format = NumberFormat.getNumberInstance();

    public static String asHumanApproximate(long bytes) {
        dec2Format.setMaximumFractionDigits(0);

        if (bytes < BYTES_MAX) {
            return bytes + " B";
        } else if (bytes < KB_MAX) {
            return dec2Format.format((double) bytes / KB_DIV) + " KB";
        } else if (bytes < MB_MAX) {
            return dec2Format.format((double) bytes / MB_DIV) + " MB";
        } else if (bytes < GB_MAX) {
            return dec2Format.format((double) bytes / GB_DIV) + " GB";
        } else if (bytes < TB_MAX) {
            return dec2Format.format((double) bytes / TB_DIV) + " TB";
        } else {
            // out of range
            return bytes + " B";
        }
    }

    private static DecimalFormatSymbols usSymbols = new DecimalFormatSymbols(new Locale("us"));
    private static DecimalFormatSymbols seSymbols = new DecimalFormatSymbols(new Locale("se"));

    public static double roundTwoDecimals(double d) {
        // TODO - fix this!
        DecimalFormat formatter = new DecimalFormat("#.##", seSymbols); // Swedish
        try {
            return Double.valueOf(formatter.format(d));
        }
        catch (NumberFormatException nfe) {
            try {
                formatter = new DecimalFormat("#,##", usSymbols); // US
                return Double.valueOf(formatter.format(d));
            }
            catch (NumberFormatException nfe2) {
                return 0.00;
            }
        }
    }
}
