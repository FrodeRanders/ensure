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
package eu.ensure.ppe;

import org.gautelis.vopn.xml.Attribute;
import org.gautelis.vopn.xml.Namespaces;
import org.gautelis.vopn.xml.XPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Makes assessments of message digest/hash/fixity algorithms.
 * <p>
 * Created by Frode Randers at 2012-10-24 22:19
 */
public class FixityAssessment {
    private static final Logger log = LogManager.getLogger(FixityAssessment.class);

    public static final double STRONG_ALGORITHM_SCORE = 100.0;
    public static final double COMPETENT_ALGORITHM_SCORE = 90.0;
    public static final double WEAK_ALGORITHM_SCORE = 50.0;
    public static final double INAPPROPRIATE_ALGORITHM_SCORE = 0.0;

    private static final String FIXITY_ALGORITHMS_FILE = "fixity-algorithms.xml";
    private static final String FIXITY_ALGORITHMS_SCHEMA_FILE = "fixity-algorithms.xsd";

    public static final Set<String> unbrokenAlgorithms = new LinkedHashSet<String>();
    public static final Set<String> weakenedAlgorithms = new LinkedHashSet<String>(); // weakened algorithms
    public static final Set<String> brokenAlgorithms = new LinkedHashSet<String>();

    public static final Set<String> strongAlgorithms = new LinkedHashSet<String>();
    public static final Set<String> competentAlgorithms = new LinkedHashSet<String>();

    static {

        ReadableByteChannel xmlInput = null;
        try {
            xmlInput = Channels.newChannel(FixityAssessment.class.getResourceAsStream(FIXITY_ALGORITHMS_FILE));

            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(Channels.newReader(xmlInput, "utf-8"));
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createStAXOMBuilder(reader);
            OMElement root = builder.getDocumentElement();

            Namespaces namespaces = new Namespaces();
            namespaces.defineNamespace("http://ensure.eu/ltu/schema/fixity-algorithms-1.0/", "f");

            // Query objects
            XPath xpath = new XPath(namespaces);
            Attribute attribute = new Attribute(namespaces);

            String expression = "//f:algorithm";
            List<OMElement> algorithms = xpath.getElementsFrom(root, expression);
            if (algorithms.size() > 0) {
                if (log.isDebugEnabled()) {
                    String info = "Loading data for " + algorithms.size() + " hash algorithms from configuration.";
                    log.debug(info);
                }

                for (OMElement algorithm : algorithms) {
                    String name = attribute.getValueFrom(algorithm, "f", "name");
                    String _aliases = attribute.getValueFrom(algorithm, "f", "alias", /* accept failure? */ true);
                    String[] aliases = {};
                    if (null != _aliases && _aliases.length() > 0) {
                        aliases = _aliases.split(",");
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Algorithm: " + name);
                    }

                    // Status {broken, weakened, unbroken}
                    String status = xpath.getTextFrom(algorithm, "f:status");
                    if ("broken".equalsIgnoreCase(status)) {
                        brokenAlgorithms.add(name.toLowerCase());

                        for (String alias : aliases) {
                            alias = alias.trim();
                            if (null != alias && alias.length() > 0) {
                                brokenAlgorithms.add(alias.toLowerCase());
                            }
                        }
                    }
                    else if ("weakened".equalsIgnoreCase(status)) {
                        weakenedAlgorithms.add(name.toLowerCase());

                        for (String alias : aliases) {
                            alias = alias.trim();
                            if (null != alias && alias.length() > 0) {
                                weakenedAlgorithms.add(alias.toLowerCase());
                            }
                        }
                    }
                    else if ("unbroken".equalsIgnoreCase(status)) {
                        unbrokenAlgorithms.add(name.toLowerCase());

                        for (String alias : aliases) {
                            alias = alias.trim();
                            if (null != alias && alias.length() > 0) {
                                unbrokenAlgorithms.add(alias.toLowerCase());
                            }
                        }
                    }
                    else {
                        String info = "Erroneous configuration for fixity algorithm \"";
                        info += name + "\": unknown status \"" + status + "\"  [ignoring configured algorithm]";
                        log.warn(info);
                        continue;
                    }

                    // Classification {strong, competent, suspect, weak}
                    String classification = xpath.getTextFrom(algorithm, "f:classification");
                    if ("strong".equalsIgnoreCase(classification)) {
                        strongAlgorithms.add(name.toLowerCase());

                        for (String alias : aliases) {
                            alias = alias.trim();
                            if (null != alias && alias.length() > 0) {
                                strongAlgorithms.add(alias.toLowerCase());
                            }
                        }
                    }
                    else if ("competent".equalsIgnoreCase(classification)) {
                        competentAlgorithms.add(name.toLowerCase());

                        for (String alias : aliases) {
                            alias = alias.trim();
                            if (null != alias && alias.length() > 0) {
                                competentAlgorithms.add(alias.toLowerCase());
                            }
                        }
                    }
                    else if ("suspect".equalsIgnoreCase(classification)
                          || "weak".equalsIgnoreCase(classification)) {
                        // Don't do anything - assuming weak
                    }
                    else {
                        String info = "Erroneous configuration for fixity algorithm \"";
                        info += name + "\": unknown classification \"" + classification + "\"  [assuming weak]";
                        log.warn(info);
                        // Don't do anything - assuming weak
                    }
                }
            }
        } catch (Exception e) {
            String info = "Failed to load (all) algorithms from resource: ";
            info += e.getMessage();
            log.warn(info, e);

        } finally {
            try {
                if (null != xmlInput) xmlInput.close();
            }
            catch (Throwable ignore) {}
        }

        if (log.isDebugEnabled()) {
            log.debug("----------------------------------------------------------------------");
            log.debug("We currently know about (approximately since there may be duplicates):");
            log.debug(" * " + unbrokenAlgorithms.size() + " unbroken algorithms");
            log.debug(" * " + weakenedAlgorithms.size() + " weakened algorithms");
            log.debug(" * " + brokenAlgorithms.size() + " broken algorithms");
            log.debug("among which there are:");
            log.debug(" * " + strongAlgorithms.size() + " strong algorithms");
            log.debug(" * " + competentAlgorithms.size() + " competent algorithms");
            log.debug("Rest of the algorithms are considered weak");
            log.debug("----------------------------------------------------------------------");
        }
    }


    public static boolean isWeakened(String algorithm) {
        return weakenedAlgorithms.contains(algorithm.toLowerCase());
    }

    public static boolean isBroken(String algorithm) {
        return brokenAlgorithms.contains(algorithm.toLowerCase());
    }

    public static boolean isUnbroken(String algorithm) {
        return unbrokenAlgorithms.contains(algorithm.toLowerCase());
    }

    /**
     * Returns a ranking for any (assumed unbroken) algorithm.
     * <p>
     * @param algorithm
     * @return
     */
    public static double rank(String algorithm) {
        // Will only rank assumed unbroken algorithms
        if (!isUnbroken(algorithm.toLowerCase())) {
            return INAPPROPRIATE_ALGORITHM_SCORE;
        }

        if (strongAlgorithms.contains(algorithm.toLowerCase())) {
            return STRONG_ALGORITHM_SCORE; // TODO - CONFIGURABLE
        }

        if (competentAlgorithms.contains(algorithm.toLowerCase())) {
            return COMPETENT_ALGORITHM_SCORE; // TODO - CONFIGURABLE
        }

        // This has to be a weak algorithm
        return WEAK_ALGORITHM_SCORE; // TODO - CONFIGURABLE
    }
}
