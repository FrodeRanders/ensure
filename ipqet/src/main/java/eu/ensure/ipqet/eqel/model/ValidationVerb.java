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
package eu.ensure.ipqet.eqel.model;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Description of ValidationVerb.
 * <p>
 * Created by Frode Randers at 2012-10-20 14:24
 */
public class ValidationVerb {
    private final String verb;
    private final Collection<VerificationSpecification> specifications = new LinkedList<VerificationSpecification>();

    protected ValidationVerb(String verb) {
        this.verb = verb;
    }

    public Collection<VerificationSpecification> getVerificationSpecifications() {
        return specifications;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("ValidationVerb [").append(verb).append("]:").append("\n");
        for (VerificationSpecification specification : specifications) {
            buf.append("    ").append(specification);
        }

        return buf.toString();
    }
}
