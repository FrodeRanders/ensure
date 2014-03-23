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
package eu.ensure.packproc.internal;

import eu.ensure.packproc.model.AssociatedInformation;

import java.util.*;

/**
 * Description of TrackedAssociatedInformation:
 * <p/>
 *
 * Created by Frode Randers at 2012-03-16 13:58
 */
class TrackedAssociatedInformation extends AssociatedInformation {
    private String _path;
    Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> _values;
    private boolean _hasDisagreements = false;

    TrackedAssociatedInformation(final String claimant, final String path, final Map<String, String> providedValues) {
        _path = path;

        // Since we want to be able to see which claimant provided which values, we slightly reorganize
        // the provided values - in the process annotating the values with the current claimant.
        //_values = new HashMap<String, AssociatedInformation.AssociationValue>();
        _values = new HashMap</* key */ String, Map</* value */ String, /* claimants */ Set<String>>>();
        for (final String key : providedValues.keySet()) {
            addValueTo(_values, key, providedValues.get(key), claimant);
        }
    }

    // Official methods
    public String getPath() {
        return _path;
    }

    public Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> getValues() {
        return _values;
    }

    public boolean hasDisagreements() {
        return _hasDisagreements;
    }


    // Local helper methods
    public enum Result {
        COINCIDING_VALUE,
        CONFLICTING_VALUE,
        NEW_VALUE
    }

    static Result addValueTo(Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> values,
             String key, String value, String claimant) {
        if (values.containsKey(key)) {
            // Add value to existing key
            Map</* value */ String, /* claimants */ Set<String>> assocValue = values.get(key);
            if (assocValue.containsKey(value)) {
                // Annotate existing value with additional claimant
                Set<String> claimants = assocValue.get(value);
                claimants.add(claimant);
                return Result.COINCIDING_VALUE;

            } else {
                // Add new value and annotate with claimant
                Set<String> claimants = new HashSet<String>();
                claimants.add(claimant);
                assocValue.put(value, claimants);
                return Result.CONFLICTING_VALUE;
            }
        } else {
            // Create a new associated value (key, value, {claimant})
            Map</* value */ String, /* claimants */ Set<String>> assocValue = new HashMap<String, Set<String>>();
            Set<String> claimants = new HashSet<String>();
            claimants.add(claimant);
            assocValue.put(value, claimants);
            values.put(key, assocValue);
            return Result.NEW_VALUE;
        }
    }
}

