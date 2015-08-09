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
package eu.ensure.commons.db.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Vector;


/**
 * Description of BatchReader:
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class BatchReader {
    private Characteristics characteristics = null;
    private Options options = null;

    public BatchReader(Options options, Characteristics characteristics) {
        this.options = options;
        this.characteristics = characteristics;
    }


    /**
     * Reads an SQL-script and identifies individual statements.
     * The batch separator handling is dependent on the database manager
     */
    public List<String> readFile(Reader reader) throws Exception {
        try {
            if (characteristics.doIgnoreCase()) {
                if (characteristics.aloneOnLine()) {
                    // Batch separator is located alone on a line
                    // beginning at first position. Ignore case.
                    //
                    // Example with separator "GO":
                    //
                    //   create index blipp ( blopp )
                    //   go
                    //
                    // -or-
                    //
                    //   CREATE INDEX blipp ( blopp )
                    //   GO
                    //
                    return readFileIgnoreCaseAloneOnLine(reader);
                } else {
                    // Batch separator may be located anywhere on a
                    // line. Ignore case.
                    //
                    // Example with separator ";":
                    //
                    //   CREATE INDEX blipp ( blopp );
                    //
                    // Beware of case (separator "GO"):
                    //
                    //   CREATE TABLE blipp (
                    //     pogopop INTEGER
                    //   )
                    //
                    // which will accept "go" in pogopop as separator.
                    //
                    // Since this is a "dangerous" case we do not support it!
                    throw new Exception("Batch separator detection scheme not implemented");
                }
            } else {
                if (characteristics.aloneOnLine()) {
                    // Batch separator is located alone on a line
                    // beginning at first position. Consider case.
                    //
                    // Example with separator "GO":
                    //
                    //   CREATE INDEX blipp ( blopp )
                    //   GO
                    //
                    // is handled while
                    //
                    //   create index blipp ( blopp )
                    //   go
                    //
                    // is not handled
                    //
                    return readFileConsiderCaseAloneOnLine(reader);
                } else {
                    // Batch separator may be located anywhere on a
                    // line. Consider case.
                    //
                    // Example with separator ";":
                    //
                    //   CREATE INDEX blipp ( blopp );
                    //
                    // If we follow our coding standards (keeping SQL uppercased
                    // and identifiers/names lowercased) the following
                    // is also handled correctly (separator "GO"):
                    //
                    //   CREATE TABLE blipp (
                    //     pogopop INTEGER
                    //   )
                    //   GO
                    //
                    // which will -NOT- accidentaly accept "go" in pogopop as separator.
                    // In the latter case it would be better still to use the alone-on-line
                    // model.
                    //
                    return readFileConsiderCase(reader);
                }
            }
        } catch (IOException e) {
            throw new Exception("Failed to read file: " + e.getMessage());

        } catch (Exception e) {
            throw new Exception("Failed to read file: " + e.getMessage());

        } finally {
            if (options.debug) System.out.println("Done reading");
        }
    }

    private List<String> readFileIgnoreCaseAloneOnLine(Reader reader) throws IOException {

        List<String> sql = new Vector<String>();
        BufferedReader in = null;

        try {
            StringBuffer data = new StringBuffer();
            String tmp = null;

            int pos = 0;

            in = new BufferedReader(reader);

            while ((tmp = in.readLine()) != null) {
                tmp = tmp.trim();

                // ignore comment lines
                if (tmp.length() > 0 && !tmp.startsWith("--")) {

                    // ignore trailing comments
                    if ((pos = tmp.indexOf("--")) >= 0) {
                        tmp = tmp.substring(0, pos);
                        tmp.trim();
                    }

                    // accept batch separator only if alone on line,
                    // ignoring case
                    if (tmp.equalsIgnoreCase(characteristics.batchSeparator)) {
                        // store
                        if (data.length() > 0)
                            sql.add(data.toString());

                        data = new StringBuffer();
                    } else {
                        // accumulate
                        data.append(tmp).append(" ");
                    }
                }
            }
        } finally {
            if (null != in) in.close();
        }

        return sql;
    }

    private List<String> readFileConsiderCaseAloneOnLine(Reader reader) throws IOException {

        List<String> sql = new Vector<String>();

        BufferedReader in = null;

        try {
            StringBuffer data = new StringBuffer();
            String tmp = null;

            int pos = 0;

            in = new BufferedReader(reader);

            while ((tmp = in.readLine()) != null) {
                tmp = tmp.trim();

                // ignore comment lines
                if (tmp.length() > 0 && !tmp.startsWith("--")) {

                    // ignore trailing comments
                    if ((pos = tmp.indexOf("--")) >= 0) {
                        tmp = tmp.substring(0, pos);
                        tmp.trim();
                    }

                    // accept batch separator only if alone on line,
                    // considering case
                    if (tmp.equals(characteristics.batchSeparator)) {
                        // store
                        if (data.length() > 0)
                            sql.add(data.toString());

                        data = new StringBuffer();
                    } else {
                        // accumulate
                        data.append(tmp).append(" ");
                    }
                }
            }
        } finally {
            if (null != in) in.close();
        }

        return sql;
    }


    private List<String> readFileConsiderCase(Reader reader) throws IOException {
        List<String> sql = new Vector<String>();

        BufferedReader in = null;

        try {
            StringBuffer data = new StringBuffer();
            String tmp = null;

            int pos = 0;
            boolean foundSeparator = false;

            in = new BufferedReader(reader);

            while ((tmp = in.readLine()) != null) {
                tmp = tmp.trim();

                // ignore comment lines
                if (tmp.length() > 0 && !tmp.startsWith("--")) {

                    // ignore trailing comments
                    if ((pos = tmp.indexOf("--")) >= 0) {
                        tmp = tmp.substring(0, pos);
                        tmp.trim();
                    }

                    // accept batch separator anywhere on line,
                    // considering case
                    //
                    if ((pos = tmp.indexOf(characteristics.batchSeparator)) >= 0) {
                        foundSeparator = true;
                        tmp = tmp.substring(0, pos);
                        tmp.trim();

                        // accumulate
                        data.append(tmp).append(" ");

                        // store
                        if (data.length() > 0)
                            sql.add(data.toString());

                        data = new StringBuffer();
                    } else {
                        // accumulate
                        data.append(tmp).append(" ");
                    }
                }
            }
        } finally {
            if (null != in) in.close();
        }

        return sql;
    }
}
