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
package eu.ensure.packproc;

import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.packproc.internal.TrackingProcessorContext;
import eu.ensure.packproc.model.AssociatedInformation;
import eu.ensure.packproc.model.EvaluationStatement;
import eu.ensure.packproc.model.ProcessorContext;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Test;

import java.io.*;
import java.util.*;

// Used when loading the Log4J configuration manually
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 */
public class ProcessingTest extends TestCase {
    private static Logger log = LoggingUtils.setupLoggingFor(ProcessingTest.class, "log-configuration.xml");

    /*
     * Small utility to create a DICOM tag maps file from a (manually) manipulated dump of
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/DICOM.html
     *
    void dump(File outputFile) {
        / *
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">

        <properties>
            <comment>Maps DICOM tags "gggg,eeee" to description</comment>
            <entry key="0002,0002">UIMedia Storage SOP Class UID</entry>
            <entry key="0002,0003">UIMedia Storage SOP Inst UID</entry>
            <entry key="0002,0010">UITransfer Syntax UID</entry>
            <entry key="0002,0012">UIImplementation Class UID</entry>
            <entry key="0002,0013">SHImplementation Version Name</entry>
            ...
        </properties>
        * /
        org.dom4j.Document document = DocumentHelper.createDocument();
        document.addDocType("properties", null, "http://java.sun.com/dtd/properties.dtd");
        Element root = document.addElement("properties");
        root.addElement("comment").addText("Maps DICOM tags \"xxxx,yyyy\" to description");

        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("dicom-tags.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\t");
                if (parts.length == 2) {
                    root.addElement("entry")
                        .addAttribute("key", parts[0])
                        .addText(parts[1]);
                }
            }

            Writer writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(outputFile, false));
                document.write(writer);
                writer.flush();
            } catch (IOException ioe) {
                try  {
                    if (null != writer) writer.close();
                } catch (Exception ignore) {}

                System.err.println("Failure! " + ioe.getMessage());
            }
        } catch (IOException ioe) {
            try  {
                if (null != is) is.close();
            } catch (Exception ignore) {}

            System.err.println("Failure! " + ioe.getMessage());
        }
    }
    */

    @Test
    public void testProcessingDICOM() {
    }
}
