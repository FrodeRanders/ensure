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
package eu.ensure.packproc.internal;

import eu.ensure.packproc.model.AssociatedValues;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Description of TrackedAssociatedValues:
 * <p/>
 *
 * Created by Frode Randers at 2012-03-22 20:55
 */
public class TrackedAssociatedValues extends AssociatedValues {

    private Map</* value */ String, /* claimants */ Set<String>> values =
            new HashMap</* value */ String, /* claimants */ Set<String>>();

    public Set<String> getValues() {
        return values.keySet();
    }

    public Set<String> getClaimants(String value) {
        Set<String> claimants = values.get(value);
        if (null == claimants) {
            claimants = new HashSet<String>();
        }
        return claimants;
    }

    public boolean hasDisagreement() {
        return values.size() > 1;
    }
}
