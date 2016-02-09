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

import eu.ensure.vopn.xml.Attribute;
import eu.ensure.vopn.xml.Namespaces;
import eu.ensure.vopn.xml.XPath;
import eu.ensure.vopn.xml.XmlException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Processes XML files, using XPath-expressions to locate elements
 */
public class RdfProcessor extends XmlFileProcessor {
    private static final Logger log = LogManager.getLogger(RdfProcessor.class);

    public RdfProcessor() {
        alias = "RDF-processor"; // a reasonable default
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
            OMElement configration, OMElement target, Namespaces namespaces, ProcessorContext context
    ) throws ProcessorException, XmlException {

        Map<String, List<OMElement>> statements = new HashMap<String, List<OMElement>>();

        namespaces.defineNamespace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        namespaces.defineNamespace("http://www.ensure.eu/fhg/ibmt/ontologies/ltdpao#", "ltdpao");
        namespaces.defineNamespace("http://aperture.semanticdesktop.org/ontology/2007/08/12/filesystemds#", "fs");

        // NEPOMUK Information Element (NIE)
        namespaces.defineNamespace("http://www.semanticdesktop.org/ontologies/2007/01/19/nie#", "nie");

        // NEPOMUK File Ontology (NFO)
        namespaces.defineNamespace("http://www.semanticdesktop.org/ontologies/2007/03/22/nfo#", "nfo");

        // DICOM Data Model Ontology (DDMO)
        namespaces.defineNamespace("http://www.ensure.eu/fhg/ibmt/ontologies/ddmo#", "ddmo");

        // Define these namespaces in the document - needed later when querying
        namespaces.applyNamespacesOn(target);

        XPath xpath = new XPath(namespaces);
        Attribute attribute = new Attribute(namespaces);

        // Extract AIP information
        String aipResourceId;
        {
            String expression = "//rdf:Description[(./rdf:type[contains(@rdf:resource, 'http://www.ensure.eu/fhg/ibmt/ontologies/ltdpao#ArchivalInformationPackage')])]";
            OMElement node = xpath.getElementFrom(target, expression);
    
            aipResourceId = attribute.getValueFrom(node, "rdf", "about");
    
            String aipVersionId = xpath.getTextFrom(node, "ltdpao:aipVersionId");
            String aipCopyId = xpath.getTextFrom(node, "ltdpao:aipCopyId");
            String aipLogicalId = xpath.getTextFrom(node, "ltdpao:aipLogicalId");
            String aipPath = xpath.getTextFrom(node, "ltdpao:aipPath");
    
            Map<String, String> map = new HashMap<String, String>();
            map.put("aipLogicalId", aipLogicalId);
            map.put("aipVersionId", aipVersionId);
            map.put("aipCopyId", aipCopyId);
            map.put("aipPath", aipPath);
            context.associate("RDF", "AIP", "AIP", map); // OBSERVE: Associated with whole AIP
        }
        
        // Get rootFolder for AIP - needed in order to generate package relative paths later on...
        String rootFolder;
        {
            String expression = "//rdf:Description[contains(@rdf:about, '" + aipResourceId + "')]/fs:rootFolder";
            String path = xpath.getTextFrom(target, expression);

            // Normalize path
            path = path.replace("\\", "/"); // just in case Windoze was in the loop somewhere


            // Validate
            String urn;

            // TODO hardcoded functionality - could it be anything else than file:/
            if (path.startsWith("file:/")) {
                urn = path;
            } else {
                urn =  "file:/" + path;
            }

            expression = "//rdf:Description[contains(@rdf:about, '" + urn + "')]" +
                         "/ltdpao:isContentDataObjectOf[contains(@rdf:resource, '" + aipResourceId + "')]";
            List<OMElement> nodes = xpath.getElementsFrom(target, expression);
            if (nodes.size() != 1) {
                String info = "Validation failure: The AIP root folder is not marked as content data object for AIP: ";
                info += "Using search expression: " + expression;
                log.warn(info);
            }

            expression = "//rdf:Description[contains(@rdf:about, '" + urn + "')]" +
                         "/nie:rootElementOf[contains(@rdf:resource, '" + aipResourceId + "')]";
            nodes = xpath.getElementsFrom(target, expression);
            if (nodes.size() != 1) {
                String info = "Validation failure: The AIP root folder is not marked as root element for AIP: ";
                info += "Using search expression: " + expression;
                log.warn(info);
            }

            //
            rootFolder = path;
            log.debug("AIP root folder: " + rootFolder);
        }

        // Is root path WIN-based? and if so what is the casing (may differ from the path information for
        // this single file
        boolean rootFolderIsPolluted = false;
        boolean rootDriveIsLowerCase = false;
        char rootDriveLetter = 0;

        //
        final String WIN_DRIVE_LETTER_RE = "([a-z]|[A-Z])\\:.*?";
        final Pattern pattern = Pattern.compile(WIN_DRIVE_LETTER_RE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher match = pattern.matcher(rootFolder);
        if (match.find()) {
            rootFolderIsPolluted = true;
            rootDriveLetter = match.group(1).charAt(0);
            rootDriveIsLowerCase = ('a' <= rootDriveLetter && rootDriveLetter <= 'z');
        }

        // DICOM SPECIFIC!!
        {
            String expression = "//rdf:Description[(./rdf:type[contains(@rdf:resource, 'http://www.ensure.eu/fhg/ibmt/ontologies/ddmo#DICOM_IOD')])]";
            List<OMElement> nodes = xpath.getElementsFrom(target, expression);
            for (OMElement node : nodes) {
                // This is a DICOM file
                String providedURI = attribute.getValueFrom(node, "rdf", "about");

                String fileName = xpath.getTextFrom(node, "nfo:fileName");
                String fileSize = xpath.getTextFrom(node, "nfo:fileSize");
                String mimeType = xpath.getTextFrom(node, "nie:mimeType");

                // Get Patient Module information
                String patientId;
                String patientName;
                {
                    OMElement moduleRef = xpath.getElementFrom(node, "ddmo:includesIE");
                    String moduleResource = attribute.getValueFrom(moduleRef, "rdf", "resource");

                    expression = "//rdf:Description[contains(@rdf:about, '" + moduleResource + "')]/ddmo:includesPatientModule";
                    OMElement moduleRefRef = xpath.getElementFrom(target, expression);
                    String patientModuleResource = attribute.getValueFrom(moduleRefRef, "rdf", "resource");

                    expression = "//rdf:Description[contains(@rdf:about, '" + patientModuleResource + "')]";
                    OMElement patientModule = xpath.getElementFrom(target, expression);
                    patientId = xpath.getTextFrom(patientModule, "ddmo:attributePatientID");
                    patientName = xpath.getTextFrom(patientModule, "ddmo:attributePatientName");
                }

                // Validate
                expression = "nie:dataSource[contains(@rdf:resource, '" + aipResourceId + "')]";
                List<OMElement> elems = xpath.getElementsFrom(node, expression);
                if (elems.size() != 1) {
                    String info = "Validation failure: The DICOM file " + fileName + " is not marked as belonging to AIP: ";
                    info += "Using search expression: " + expression;
                    log.warn(info);
                }
                String path;
                if (providedURI.startsWith("file:/")) {
                    path = providedURI.substring(/* lengthOf("file:/") */ 6);
                } else {
                    path = providedURI;
                }
                String providedPath = path; // without "file:/" if present

                // If needed, align case of Windoze drive-letter in path with that of rootFolder.
                // Needed below
                match = pattern.matcher(path);
                if (match.find()) {
                    // Path contains a drive letter.
                    path = path.substring(/* lengthOf("C:") */ 2);

                    char driveLetter = match.group(1).charAt(0);
                    boolean isLowerCase = ('a' <= driveLetter && driveLetter <= 'z');
                    if (rootFolderIsPolluted) {
                        // Root folder also has a drive letter. Align case of these two
                        if (rootDriveIsLowerCase && !isLowerCase) {
                            String preamble = ("" + driveLetter).toLowerCase() + ":";
                            path = preamble + path;
                        }
                        else if (!rootDriveIsLowerCase && isLowerCase) {
                            String preamble = ("" + driveLetter).toUpperCase() + ":";
                            path = preamble + path;
                        }
                        else {
                            path = "" + driveLetter + ":" + path; // Re-combine
                        }
                    }
                }

                path = path.replace(rootFolder, "/content").replace("\\", "/"); // in case we're on Windoze

                //
                Map<String, String> map = new HashMap<String, String>();
                map.put("fileName", fileName);
                map.put("size", fileSize);
                map.put("mimeType", mimeType);

                map.put("0010,0010", patientName); // PatientName
                map.put("0010,0020", patientId); // PatientID (Patient level)

                context.associate("RDF", path, providedPath, map); // OBSERVE: Associated with individual files
            }
        }
    }
}
