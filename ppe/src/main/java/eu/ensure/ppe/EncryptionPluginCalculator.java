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
import eu.ensure.ppe.model.EncryptionPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.util.Collection;

/**
 * Calculates score for encryption plugins.
 * <p>
 * Created by Frode Randers at 2012-10-16 17:57
 */
public class EncryptionPluginCalculator {
    private static final Logger log = LogManager.getLogger(EncryptionPluginCalculator.class);

    private AssessmentInfoRetriever infoRetriever;
    private DataSource dataSource;

    public EncryptionPluginCalculator(DataSource dataSource) {
        this.dataSource = dataSource;
        this.infoRetriever = new AssessmentInfoRetriever(dataSource);
    }

    public Double calculateScore(
            Aggregation aggregation,
            String event,
            EncryptionPlugin plugin,
            Collection<String> storyLine,
            Collection<Consequence> consequences
    ) throws EvaluationException {

        String algorithm = plugin.getEncryptionAlgorithm();

        return null; // No score
    }
}
