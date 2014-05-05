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
 * Models plugins that are used to implement functionality in ENSURE.
 * <p/>
 * Created by Frode Randers at 2012-09-10 14:56
 */
public class Plugin {
    private static final String NOT_APPLICABLE = "N/A";
    protected String pluginId;
    private String serviceProvider;
    private String systemId;
    private String type;
    private String description;

    private Double mtbf = null;
    private String mtbfUnit = null;

    private Plugin parentPlugin = null;

    public Plugin(String pluginId, String type, String description) {
        this.pluginId = pluginId;
        this.serviceProvider = NOT_APPLICABLE;
        this.systemId = NOT_APPLICABLE;
        this.type = type;
        this.description = description;
    }

    public Plugin(String pluginId, String serviceProvider, String systemId, String type, String description) {
        this.pluginId = pluginId;
        this.serviceProvider = serviceProvider;
        this.systemId = systemId;
        this.type = type;
        this.description = description;
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getMtbfUnit() {
        return mtbfUnit;
    }

    public void setMtbfUnit(String unit) {
        this.mtbfUnit = unit;
    }

    public Double getMtbf() {
        return mtbf; // may be null!
    }

    public void setMtbf(Double mtbf) {
        this.mtbf = mtbf;
    }

    public Plugin getParentPlugin() {
        return parentPlugin;
    }

    public void setParentPlugin(Plugin parentPlugin) {
        this.parentPlugin = parentPlugin;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\n        [").append("Plugin (").append(pluginId).append(") \"").append(description).append("\"");
        buf.append(" provider=\"").append(serviceProvider).append("\"");
        buf.append(" system=\"").append(systemId).append("\"");
        buf.append("]");
        //buf.append("\n");
        return buf.toString();
    }
}
