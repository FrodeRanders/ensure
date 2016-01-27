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
package eu.ensure.packproc.ip;

import eu.ensure.commons.xml.Attribute;
import eu.ensure.commons.xml.Namespaces;
import eu.ensure.commons.xml.XPath;
import eu.ensure.commons.xml.XmlException;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.XmlFileProcessor;
import eu.ensure.packproc.model.ProcessorContext;
import org.apache.axiom.om.OMElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Processes XML files, using XPath-expressions to locate elements
 */
public class XfduProcessor extends XmlFileProcessor {
    private static final Logger log = LogManager.getLogger(XfduProcessor.class);

    public XfduProcessor() {
        alias = "XFDU-processor"; // a reasonable default
    }

    protected XmlFileCallable getSpecificCallable() {
        return new XmlFileCallable() {
            public void call(OMElement target, Namespaces namespaces, ProcessorContext context) throws Exception {
                // Retrieve XPath expressions from the configuration
                try {
                    for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                        OMElement configuration = ei.next();
                        String operation = configuration.getLocalName(); // Ignore namespace!!!

                        if ("extractBitstreamInformation".equalsIgnoreCase(operation)) {
                            extractBitstreamInformation(configuration, target, namespaces, context);

                        } else {
                            throw new ProcessorException("Unknown processor operation: " + operation);
                        }
                    }
                } catch (Throwable t) {
                    String info = "Cannot process configuration for " + alias + ": ";
                    info += t.getMessage();
                    log.warn(info);

                    throw new ProcessorException(info, t);
                }
            }
        };
    }


    private void extractBitstreamInformation(
            OMElement configuration, OMElement target, Namespaces namespaces, ProcessorContext context
    ) throws ProcessorException, XmlException {

        namespaces.defineNamespace("urn:ccsds:schema:xfdu:1", "xfdu");
        XPath xpath = new XPath(namespaces);
        Attribute attribute = new Attribute(namespaces);

        // Locate the bit streams of this package
        String expression = "//byteStream";
        List<OMElement> nodes = xpath.getElementsFrom(target, expression);
        if (nodes.size() > 0) {
            if (log.isDebugEnabled()) {
                String info = "This XPath expression \"";
                info += expression;
                info += "\" identifies " + nodes.size() + " node(s).";
                log.debug(info);
            }

            for (OMElement node : nodes) {
                String size = attribute.getValueFrom(node, "size");

                // Get location of file
                String xpathExpr = "fileLocation";
                OMElement location = xpath.getElementFrom(node, xpathExpr);
                String type = attribute.getValueFrom(location, "locatorType");
                String locator = attribute.getValueFrom(location, "locator");
                String href = attribute.getValueFrom(location, "href");

                // Get checksum information
                xpathExpr = "checksum";
                OMElement checksum = xpath.getElementFrom(node, xpathExpr);
                String checksumName = attribute.getValueFrom(checksum, "checksumName");
                String checksumValue = checksum.getText().trim();

                // Strip off "file:"
                if (type.equalsIgnoreCase("URL")) {
                    if (locator.startsWith("file:")) {
                        locator = locator.substring(/* lengthOf("file:") */ 5);
                    }
                    if (href.startsWith("file:")) {
                        href = href.substring(/* lengthOf("file:") */ 5);
                    }
                }

                // Strip off "./"
                if (locator.startsWith(("./"))) {
                    locator = locator.substring(/* lengthOf("./") */ 2);
                }
                if (href.startsWith(("./"))) {
                    href = href.substring(/* lengthOf("./") */ 2);
                }

                // Create a package-relative path
                File top = new File("/");
                File contentPackage = new File(top, locator); // relative to top
                File contentStream = new File(contentPackage, href); // relative to contentPackage
                String path = contentStream.getPath().replace("\\", "/"); // in case we're on Windoze

                //
                if (log.isDebugEnabled()) {
                    String info = "@ Stream: path=" + path;
                    info += ", checksum-name=" + checksumName;
                    info += ", checksum-value=" + checksumValue;
                    info += ", size=" + size;
                    log.debug(info);
                }

                // Store information for later
                Map<String, String> map = new HashMap<String, String>();
                map.put(checksumName, checksumValue);
                map.put("size", size);
                context.associate("XFDU", path, path, map); // TODO - use other providedPath (since composite)
            }
        } else if (nodes.size() == 0) {
            handleNoNodes(expression);
        }
    }
}
