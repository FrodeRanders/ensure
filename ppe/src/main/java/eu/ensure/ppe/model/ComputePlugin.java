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
 * Specifically models compute plugins in ENSURE.
 * <p>
 * Created by Frode Randers at 2013-01-17 00:09
 */
public class ComputePlugin extends LocalizedPlugin {

    public ComputePlugin(String pluginId, String serviceProvider, String systemId, String locality, String type, String description, Visibility visibility) {
        super(pluginId, serviceProvider, systemId, locality, type, description, visibility);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n        [").append("ComputePlugin (").append(pluginId).append(") \"").append(getDescription()).append("\"");
        buf.append(" provider=\"").append(getServiceProvider()).append("\"");
        buf.append(" system=\"").append(getSystemId()).append("\"");
        buf.append(" locality=\"").append(getLocality()).append("\"");
        buf.append(" visibility=\"").append(getVisibility().name().toLowerCase()).append("\"");
        buf.append("]");
        //buf.append("\n");
        return buf.toString();
    }
}
