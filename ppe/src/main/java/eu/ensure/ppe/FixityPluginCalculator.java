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


import eu.ensure.ppe.model.Aggregation;
import eu.ensure.ppe.model.Consequence;
import eu.ensure.ppe.model.FixityPlugin;
import eu.ensure.ppe.model.Purpose;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.util.Collection;

/**
 * Calculates score for fixity plugins.
 * <p>
 * Hash functions that are collision-resistant are called cryptographic hash functions
 * and are used for message authentication, digital signatures, obscuring passwords, etc.
 * (Valerie Aurora, 2007)
 *
 * The most important characteristics of cryptographic hashes are that the collision-
 * resistance property degrades over time.
 * (Valerie Aurora, 2007)
 * <p>
 * We will try to assess the "strength" - or collision-resistance in this case - of such
 * hash functions for the purpose of (message) authentication in digital preservation.
 * <p>
 * Something about computational complexity - output from 'openssl speed' on my Mac:
 * <pre>
 *     The 'numbers' are in 1000s of bytes per second processed.
 *
 *     type                16 bytes     64 bytes    256 bytes   1024 bytes   8192 bytes
 *     ---------------------------------------------------------------------------------
 *     md2                     0.00         0.00         0.00         0.00         0.00
 *     mdc2                13280.99k    14230.25k    14517.85k    14617.94k    14589.95k
 *     md4                 65025.25k   194980.74k   436904.96k   629422.08k   725985.96k
 *  >  md5                 57078.16k   167039.30k   372963.41k   537889.11k   610697.22k
 *     hmac(md5)           44052.99k   139992.83k   337815.64k   520850.43k   613045.59k
 *  >  sha1                60183.44k   165975.62k   350891.92k   481671.51k   538367.32k
 *     rmd160              44779.27k   111781.72k   210174.72k   270058.50k   293890.73k
 *     rc4                334910.12k   338016.79k   338140.76k   336662.87k   338288.64k
 *     des cbc             61933.29k    63664.47k    64104.28k    63516.08k    63552.39k
 *     des ede3            23780.62k    24126.95k    24117.25k    24428.39k    24010.75k
 *     idea cbc            74954.72k    77145.32k    78375.42k    78339.75k    77348.86k
 *     seed cbc            71288.71k    71052.97k    72086.87k    71490.90k    72409.09k
 *     rc2 cbc             39953.50k    40591.55k    40767.23k    40735.74k    40801.62k
 *     rc5-32/12 cbc           0.00         0.00         0.00         0.00         0.00
 *     blowfish cbc        98663.20k   102552.11k   105834.41k   106222.93k   106419.54k
 *     cast cbc            97867.40k   102050.94k   103629.23k   103718.91k   103596.03k
 *     aes-128 cbc        101857.83k   110674.28k   112864.68k   113051.65k   113344.51k
 *     aes-192 cbc         87443.71k    92048.73k    93776.21k    94922.41k    95434.07k
 *     aes-256 cbc         75620.14k    79754.24k    80887.38k    81003.86k    81611.43k
 *     camellia-128 cbc    94469.38k   142572.46k   161806.25k   167092.91k   168867.16k
 *     camellia-192 cbc    82195.39k   112039.94k   122957.23k   123904.68k   125930.15k
 *     camellia-256 cbc    79995.30k   110689.73k   120597.42k   124403.03k   126088.53k
 *  >  sha256              42478.12k    90702.25k   151412.48k   182895.96k   194458.97k
 *  >  sha512              31591.85k   123904.41k   191998.38k   267320.32k   302891.01k
 *  >  whirlpool           25096.19k    52831.42k    86744.83k   103815.11k   109513.39k
 *     aes-128 ige        103631.91k   107370.37k   107753.47k   108189.01k   108475.73k
 *     aes-192 ige         88378.82k    90306.75k    90747.82k    90861.57k    91138.73k
 *     aes-256 ige         76361.37k    77571.03k    77977.86k    78257.83k    78337.37k
 *     ghash              833276.33k  1410963.22k  1645274.45k  1722450.94k  1739508.39k
 *
 * </pre>
 * <p>
 * Created by Frode Randers at 2012-10-16 17:57
 */
public class FixityPluginCalculator {
    private static final Logger log = LogManager.getLogger(FixityPluginCalculator.class);

    private AssessmentInfoRetriever infoRetriever;
    private DataSource dataSource;

    public FixityPluginCalculator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.infoRetriever = new AssessmentInfoRetriever(dataSource);
    }

    public Double calculateScore(
            Aggregation aggregation,
            String event,
            FixityPlugin plugin,
            Collection<String> storyLine,
            Collection<Consequence> consequences
    ) throws EvaluationException {

        String algorithm = plugin.getFixityAlgorithm();
        String description = plugin.getDescription();

        if (event.equalsIgnoreCase("on_ingest")
         || event.equalsIgnoreCase("on_access")) {
            // In this case, the choice of fixity algorithm is extremely important.
            // Note however, that multiple fixity algorithms may be used during ingest,
            // but we will calculate scores individually.

            for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                String purposeName = purpose.toString();
                if (Purpose.EVIDENCE.equalsIgnoreCase(purposeName)) {

                    // Fail if not good
                    if (FixityAssessment.isBroken(algorithm) || FixityAssessment.isWeakened(algorithm)) {
                        // ---- Consequences ----
                        double probability = 75.0; // TODO
                        double suitability = 0.0; // TODO

                        //
                        StringBuilder _description = new StringBuilder();
                        _description.append("During ").append(event.substring(3).toLowerCase()).append(" no fixity check is done. ");

                        _description.append("Hence the suitability is assessed to be ")
                           .append(Double.toString(suitability)).append(" % with probability ")
                           .append(Double.toString(probability)).append(" % of this happening. ");

                        Consequence consequence = new Consequence(probability, suitability, _description.toString());
                        consequences.add(consequence);

                        if (log.isInfoEnabled()) {
                            log.info(_description.toString());
                        }

                        // ---- Story line ----
                        StringBuilder story = new StringBuilder();
                        story.append("purpose=\"").append(purpose).append("\"")
                             .append(" event=\"").append(event).append("\"")
                             .append(" algorithm=\"").append(algorithm).append("\"")
                             .append(" plugin-description=\"").append(description).append("\"")
                             .append(" (").append(plugin.getType()).append(")")
                             .append(": \"Inappropriate choice of fixity algorithm. It does not guarantee integrity!").append("\"");
                        storyLine.add(story.toString());

                        return FixityAssessment.INAPPROPRIATE_ALGORITHM_SCORE;
                    }

                    return FixityAssessment.rank(algorithm);
                }
            }
        } else if (event.equalsIgnoreCase("on_time")) {
            // In this case, the choice of fixity algorithm is not very important
            if (FixityAssessment.isUnbroken(algorithm)) {
                return FixityAssessment.rank(algorithm);
            }
            if (FixityAssessment.isWeakened(algorithm)) {

                // ---- Consequences ----
                double probability = 15.0; // TODO
                double suitability = 75.0; // TODO

                //
                StringBuilder _description = new StringBuilder();
                _description.append("A weak fixity check is scheduled to be used during the retention period. ");

                _description.append("Hence the suitability is assessed to be ")
                   .append(Double.toString(suitability)).append(" % with probability ")
                   .append(Double.toString(probability)).append(" % of this happening. ");

                Consequence consequence = new Consequence(probability, suitability, _description.toString());
                consequences.add(consequence);

                if (log.isInfoEnabled()) {
                    log.info(_description.toString());
                }

                // ---- Story line ----
                StringBuilder story = new StringBuilder();
                story.append("purposes={");
                for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                    story.append(purpose).append(" ");
                }
                story.append("}");
                story.append(" event=\"").append(event).append("\"");
                story.append(" algorithm=\"").append(algorithm).append("\"");
                story.append(" plugin-description=\"").append(description).append("\"");
                story.append(" (").append(plugin.getType()).append(")");
                story.append(": \"This fixity algorithm is weak and may not be appropriate for guaranteeing information integrity, but it may suffice for assessing system integrity").append("\"");
                storyLine.add(story.toString());

                return FixityAssessment.COMPETENT_ALGORITHM_SCORE - 10.0; // Still useful for bit rot detection
            }
            if (FixityAssessment.isBroken(algorithm)) {
                // ---- Consequences ----
                double probability = 15.0; // TODO
                double suitability = 50.0; // TODO

                //
                StringBuilder _description = new StringBuilder();
                _description.append("A broken fixity check is scheduled to be used during the retention period. ");

                _description.append("Hence the suitability is assessed to be ")
                   .append(Double.toString(suitability)).append(" % with probability ")
                   .append(Double.toString(probability)).append(" % of this happening. ");

                Consequence consequence = new Consequence(probability, suitability, _description.toString());
                consequences.add(consequence);

                if (log.isInfoEnabled()) {
                    log.info(_description.toString());
                }

                // ---- Story line ----
                StringBuilder story = new StringBuilder();
                story.append("purposes={");
                for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                    story.append(purpose).append(" ");
                }
                story.append("}");
                story.append(" event=\"").append(event).append("\"");
                story.append(" algorithm=\"").append(algorithm).append("\"");
                story.append(" plugin-description=\"").append(description).append("\"");
                story.append(" (").append(plugin.getType()).append(")");
                story.append(": \"This fixity algorithm is broken and cannot be used to guarantee information integrity, but it may suffice for assessing system integrity").append("\"");
                storyLine.add(story.toString());

                return FixityAssessment.COMPETENT_ALGORITHM_SCORE - 15.0; // Still useful for bit rot detection
            }

            StringBuilder story = new StringBuilder();
            story.append("purposes={");
            for (Purpose purpose : aggregation.getPrimaryPurposes()) {
                story.append(purpose).append(" ");
            }
            story.append("}");
            story.append(" event=\"").append(event).append("\"");
            story.append(" algorithm=\"").append(algorithm).append("\"");
            story.append(" plugin-description=\"").append(description).append("\"");
            story.append(" (").append(plugin.getType()).append(")");
            story.append(": \"Don't know this fixity algorithm!").append("\"");
            storyLine.add(story.toString());

            return FixityAssessment.WEAK_ALGORITHM_SCORE;
        }

        // Other types of events
        return null; // No score
    }
}
