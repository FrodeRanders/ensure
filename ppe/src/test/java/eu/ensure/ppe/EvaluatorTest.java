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

import eu.ensure.vopn.db.DatabaseException;
import eu.ensure.vopn.io.Closer;
import eu.ensure.vopn.lang.Stacktrace;
import eu.ensure.vopn.xml.Namespaces;
import eu.ensure.ppe.model.Plugin;
import eu.ensure.ppe.model.PreservationPlan;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * <p>
 * Created by Frode Randers at 2012-09-08 14:00
 */
public class EvaluatorTest extends TestCase {
    private static Logger log = LogManager.getLogger(EvaluatorTest.class);

    private EvaluationManager evaluationManager = EvaluationManager.getInstance();

    @Test
    public void testDatamodel() {
        try {
            evaluationManager.setup(new PrintWriter(System.out));

        } catch (Exception e) {
            fail("Failed to test datamodel: " + e.getMessage());
        }
    }


    @Test
    public void testConfigurationReader() throws IOException {

        ReadableByteChannel inputChannel = null;
        try {
            Namespaces namespaces = new Namespaces();
            ConfigurationReader reader = evaluationManager.getReader();

            // --- Plugin configuration ---
            InputStream is = this.getClass().getResourceAsStream("plugins-configuration.xml");
            inputChannel = Channels.newChannel(is);

            Map<String, Plugin> configuration = reader.readPluginConfiguration(inputChannel, namespaces);
            inputChannel.close();

            // --- Preservation plan ---
            is = this.getClass().getResourceAsStream("preservation-plan.xml");
            inputChannel = Channels.newChannel(is);

            PreservationPlan proposal = reader.readAggregations(inputChannel, namespaces, configuration);
            inputChannel.close();

            // --- Requirement set ---
            is = this.getClass().getResourceAsStream("requirement-set.xml");
            inputChannel = Channels.newChannel(is);

            reader.readRequirements(inputChannel, namespaces, proposal);
            inputChannel.close();

            // --- Present proposal ---
            System.out.println("\n--- Global Preservation Plan contents ---");
            System.out.println(proposal);

            //
            System.out.println("\n--- Global Preservation Plan score ---");

            Calculator calculator = evaluationManager.getCalculator();

            Map<String, String> aggregationMap = new HashMap<String, String>();
            Collection<AggregationLevelScore> aggregationScores = new LinkedList<AggregationLevelScore>();
            CustomerLevelScore totalScore =
                    calculator.calculateScore(proposal, aggregationScores, aggregationMap);

            for (AggregationLevelScore aggScore : aggregationScores) {
                StringBuilder buf = new StringBuilder();
                buf.append("Aggregation: id=\"").append(aggScore.getId())
                        .append("\" name=\"").append(aggScore.getName())
                        .append("\" score=").append(aggScore.getScore())
                        .append("% CV=").append(aggScore.getCV()).append("%");

                System.out.println(buf.toString());
            }

            StringBuilder buf = new StringBuilder();
            buf.append("GPP: customerImpact=").append(totalScore.getCustomerImpact());
            buf.append(" providerImpact=").append(totalScore.getProviderImpact());
            buf.append(" score=").append(totalScore.getScore()).append("%");
            buf.append(" CV=").append(totalScore.getCV()).append("%");
            System.out.println(buf.toString());

        } catch (DatabaseException de) {
            fail(de.getMessage());

        } catch (Exception e) {
            String info = "Failed to operate on GPP: ";
            info += Stacktrace.getBaseCause(e).getMessage();
            info += "\n";
            info += Stacktrace.asString(e);
            fail(info);

        } finally {
            Closer.close(inputChannel);
        }
    }

    @Test
    public void testFixityAssessments() {
        final String[] a = {};

        for (String alg : FixityAssessment.unbrokenAlgorithms.toArray(a)) {
            if (FixityAssessment.isBroken(alg)) {
                fail("Fixity algorithm " + alg + ", assumed to be unbroken is listed as broken as well");
            }
            if (FixityAssessment.isWeakened(alg)) {
                fail("Fixity algorithm " + alg + ", assumed to be unbroken is listed as suspect as well");
            }
        }

        for (String alg : FixityAssessment.weakenedAlgorithms.toArray(a)) {
            if (FixityAssessment.isBroken(alg)) {
                fail("Fixity algorithm " + alg + ", assumed to be suspect is listed as broken as well");
            }
            if (FixityAssessment.isUnbroken(alg)) {
                fail("Fixity algorithm " + alg + ", assumed to be suspect is listed as unbroken as well");
            }
        }

        for (String alg : FixityAssessment.brokenAlgorithms.toArray(a)) {
            if (FixityAssessment.isUnbroken(alg)) {
                fail("Fixity algorithm " + alg + ", assumed to be broken is listed as unbroken as well");
            }
            if (FixityAssessment.isWeakened(alg)) {
                fail("Fixity algorithm " + alg + ", assumed to be broken is listed as suspect as well");
            }
        }

        if (!FixityAssessment.isUnbroken("SHA3")) {
            fail("Does not correctly identify SHA3 (alias to SHA-3) as unbroken");
        }

        if (FixityAssessment.rank("SHA3") != 100.0) {
            fail("Does not correctly identify SHA3 (alias to SHA-3) as strong");
        }
    }
}
