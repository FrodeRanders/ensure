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
package eu.ensure.ppe.model;

/**
 * Specifically models encryption plugins in ENSURE.
 * <p/>
 * Created by Frode Randers at 2013-01-17 00:13
 */
public class EncryptionPlugin extends Plugin {
    private String algorithm = null;  // ex. AES128

    private String algorithmStrengthRating = null; // ex. COMPUTATIONALLY_SECURE
    private int attackStepsPowerOf2 = 0; // ex. 20
    private int blockSize = 0; // ex. 512

    private int exposureToPeerReview = 0; // ex. 2
    private int keyLengthBits = 0; // ex. 512
    private int numberOfKnownVulnerabilities = 0; // ex. 3
    private int rounds = 0; // ex. 5

    public EncryptionPlugin(String pluginId, String algorithm,
                            String algorithmStrengthRating, String _attackStepsPowerOf2, String _blockSize,
                            String _exposureToPeerReview, String _keyLengthBits, String _numberOfKnownVulnerabilities,
                            String _rounds, String type, String description) {
        super(pluginId, type, description);
        this.algorithm = algorithm;
        this.algorithmStrengthRating = algorithmStrengthRating;
        this.attackStepsPowerOf2 = Integer.parseInt(_attackStepsPowerOf2);
        this.blockSize = Integer.parseInt(_blockSize);
        this.exposureToPeerReview = Integer.parseInt(_exposureToPeerReview);
        this.keyLengthBits = Integer.parseInt(_keyLengthBits);
        this.numberOfKnownVulnerabilities = Integer.parseInt(_numberOfKnownVulnerabilities);
        this.rounds = Integer.parseInt(_rounds);
    }

    public String getEncryptionAlgorithm() {
        return algorithm;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n        [").append("EncryptionPlugin (").append(pluginId).append(") \"").append(getDescription()).append("\"");
        buf.append(" algorithm=\"").append(getEncryptionAlgorithm()).append("\"");
        buf.append("]");
        //buf.append("\n");
        return buf.toString();
    }
}
