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

import java.util.*;
import java.util.List;

/**
 * Models all things GPP, such as sets of proposed plans for individual aggregations
 * and calculations that pertain to the super-aggregation level.
 * <p>
 * Created by Frode Randers at 2012-09-10 14:56
 */
public class PreservationPlan {
    private String id = null; // Id of GPP
    private String name = "";

    private Double customerImpact = null;
    private Double providerImpact = null;
    private Double variance = null;

    private List<Aggregation> aggregations = new Vector<Aggregation>();
    private Map<String, Event> systemWideEvents = new HashMap<String, Event>();

    public PreservationPlan() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Aggregation> getAggregations() {
        return aggregations;
    }

    public Map<String, Event> getSystemWideEvents() {
        return systemWideEvents;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n  [").append("Preservation plan ");
        if (null != id && id.length() > 0) {
            buf.append("(").append(id).append(")");
        }
        buf.append("]").append(": ");
        for (Aggregation aggregation : aggregations) {
            buf.append(aggregation);
        }
        buf.append("\n    [<System events>]: ");
        for (Event event : systemWideEvents.values()) {
            buf.append(event);
        }
        //buf.append("\n");
        return buf.toString();
    }
}
