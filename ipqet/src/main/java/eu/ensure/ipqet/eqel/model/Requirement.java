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

import eu.ensure.ipqet.eqel.EqelException;

/**
 * Description of Requirement.
 * <p/>
 * Created by Frode Randers at 2012-10-20 12:44
 */
public class Requirement {

    private final String item;
    private final String verb;
    private final String requirement;
    private final boolean isMandatory;

    public Requirement(final String reference, final String requirement, final String type) {
        this.requirement = requirement;
        isMandatory = null != type && type.equalsIgnoreCase("[mandatory]");

        String[] part = reference.split(":");
        if (part.length != 2) {
            String info = "A requirement reference should consist of the pair <target>:<requirement>. ";
            info += "Instead it was simply \"" + reference + "\"";
            throw new EqelException(info);
        }

        item = part[0]; // IP
        verb = part[1]; // contains
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("Requirement: ").append(item).append(" ").append(verb).append(" ").append(requirement);

        return buf.toString();
    }
}
