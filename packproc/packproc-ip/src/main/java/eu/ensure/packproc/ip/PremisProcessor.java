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

import eu.ensure.commons.xml.Namespaces;
import eu.ensure.commons.xml.XPath;
import eu.ensure.commons.xml.XmlException;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.XmlFileProcessor;
import eu.ensure.packproc.model.ProcessorContext;
import org.apache.axiom.om.OMElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Processes XML files, using XPath-expressions to locate elements
 */
public class PremisProcessor extends XmlFileProcessor {
    private static final Logger log = LogManager.getLogger(PremisProcessor.class);

    public PremisProcessor() {
        alias = "PREMIS-processor"; // a reasonable default
    }

    public XmlFileCallable getWhatToDo() {
        return new XmlFileCallable() {
            public void call(OMElement document, Namespaces namespaces, ProcessorContext context) throws Exception {
                if (null == configElement) {
                    String info = "Cannot process PREMIS-file - no configuration";
                    log.warn(info);
                    throw new ProcessorException(info);
                }

                // Retrieve XPath expressions from the configuration
                for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                    OMElement configuration = ei.next();
                    String operation = configuration.getLocalName(); // Ignore namespace!!!

                    if ("extractRepresentationInformation".equalsIgnoreCase(operation)) {
                        extractRepresentationInformation(configuration, document, namespaces, context);
                    } else if ("extractBitstreamInformation".equalsIgnoreCase(operation)) {
                            extractBitstreamInformation(configuration, document, namespaces, context);
                    } else {
                        throw new ProcessorException("Unknown processor operation: " + operation);
                    }
                }
            }
        };
    }


    private void extractRepresentationInformation(
            OMElement configuration, OMElement document, Namespaces namespaces, ProcessorContext context
    ) throws ProcessorException, XmlException {

        namespaces.defineNamespace("info:lc/xmlns/premis-v2", "premis");
        namespaces.defineNamespace("http://www.w3.org/2001/XMLSchema-instance", "xsi");

        String expression = "//premis:object[@xsi:type='representation']";
        OMElement representation = XPath.getElementFrom(document, namespaces, expression);

        Map<String, String> map = new HashMap<String, String>();

        String originalName = XPath.getTextFrom(representation, namespaces, "premis:originalName/text()");
        map.put("aipPath", originalName);

        List<OMElement> identifiers = XPath.getElementsFrom(representation, namespaces, "premis:objectIdentifier");
        for (OMElement identifier : identifiers) {
            String type = XPath.getTextFrom(identifier, namespaces, "premis:objectIdentifierType");
            String value = XPath.getTextFrom(identifier, namespaces, "premis:objectIdentifierValue");

            map.put(type, value);
        }
        context.associate("PREMIS", "AIP", "AIP", map);
    }

    private void extractBitstreamInformation(
            OMElement configuration, OMElement document, Namespaces namespaces, ProcessorContext context
    ) throws ProcessorException, XmlException {

        XPath xpath = new XPath(namespaces);

        // Get whatever namespace prefix was used
        String ns = document.getQName().getPrefix();
        if (null != ns && ns.length() > 0) {
            ns += ":"; // Just to make things easier later on
        }

        String expr = "//" + ns + "object[@xsi:type='file']";
        String expression = manager.resolve(expr);

        List<OMElement> files = xpath.getElementsFrom(document, expression);
        if (files.size() > 0) {
            if (log.isDebugEnabled()) {
                String info = "This XPath expression \"";
                info += expression;
                info += "\" identifies " + files.size() + " node(s).";
                log.debug(info);
            }

            for (OMElement file : files) {
                String providedPath = xpath.getTextFrom(file, ns + "originalName/text()");

                // Normalize path since path is our "PRIMARY KEY"
                String path = providedPath.replace("\\", "/"); // just in case Windoze was in the loop somewhere
                if (path.startsWith("./")) {
                    path = path.substring(/* lengthOf("./") */ 2);
                }

                if (!path.startsWith("content")) {
                    path = "/content/" + path;
                }

                // Fixity info
                OMElement fixity = xpath.getElementFrom(file, ns + "objectCharacteristics/" + ns + "fixity");
                String checksumName = xpath.getTextFrom(fixity, ns + "messageDigestAlgorithm");
                String checksumValue = xpath.getTextFrom(fixity, ns + "messageDigest");

                // Size info
                String size = xpath.getTextFrom(file, ns + "objectCharacteristics/" + ns + "size");

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
                context.associate("PREMIS", path, providedPath, map);
            }
        } else if (files.size() == 0) {
            handleNoNodes(expression);
        }
    }
}
