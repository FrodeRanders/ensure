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

import eu.ensure.commons.xml.Attribute;
import eu.ensure.commons.xml.Namespaces;
import eu.ensure.commons.xml.XPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

/**
 * Makes assessments of encryption algorithms.
 * <p/>
 * Created by Frode Randers at 2012-10-24 22:19
 */
public class EncryptionAssessment {
    private static final Logger log = Logger.getLogger(EncryptionAssessment.class);

    public static final double STRONG_ALGORITHM_SCORE = 100.0;
    public static final double COMPETENT_ALGORITHM_SCORE = 90.0;
    public static final double WEAK_ALGORITHM_SCORE = 50.0;
    public static final double INAPPROPRIATE_ALGORITHM_SCORE = 0.0;


    public static boolean isSuspect(String algorithm) {
        return false;
    }

    public static boolean isBroken(String algorithm) {
        return false;
    }

    public static boolean isUnbroken(String algorithm) {
        return true;
    }

    /**
     * Returns a ranking for any (assumed unbroken) algorithm.
     * <p/>
     * @param algorithm
     * @return
     */
    public static double rank(String algorithm) {
        // Will only rank assumed unbroken algorithms
        if (!isUnbroken(algorithm.toLowerCase())) {
            return INAPPROPRIATE_ALGORITHM_SCORE;
        }
        /*
        if (strongAlgorithms.contains(algorithm.toLowerCase())) {
            return STRONG_ALGORITHM_SCORE; // TODO - CONFIGURABLE
        }

        if (competentAlgorithms.contains(algorithm.toLowerCase())) {
            return COMPETENT_ALGORITHM_SCORE; // TODO - CONFIGURABLE
        }
        */

        // This has to be a weak algorithm
        return WEAK_ALGORITHM_SCORE; // TODO - CONFIGURABLE
    }
}
