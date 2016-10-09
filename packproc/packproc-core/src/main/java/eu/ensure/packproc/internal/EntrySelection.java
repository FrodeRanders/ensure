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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deals with the attributes of a structure processor in order to identify entries
 * within the structure...
 */
public class EntrySelection {
    private static final Logger log = LogManager.getLogger(EntrySelection.class);

    private String location;

    private String name;
    private String nameRE;
    private Pattern namePattern = null;

    private String type;
    private String typeRE;
    private Pattern typePattern = null;

    private boolean hasSpecifiedLocation = false;
    private boolean hasSpecifiedName = false;
    private boolean hasSpecifiedNameRE = false;
    private boolean hasSpecifiedType = false;
    private boolean hasSpecifiedTypeRE = false;

    public EntrySelection(Map<String, String> attributes, String method) {
        location = attributes.get("location");
        name = attributes.get("name");
        nameRE = attributes.get("name-re");
        type = attributes.get("type");
        typeRE = attributes.get("type-re");

        hasSpecifiedLocation = null != location && location.length() > 0;
        hasSpecifiedName = null != name && name.length() > 0;
        hasSpecifiedNameRE = null != nameRE && nameRE.length() > 0;
        hasSpecifiedType = null != type && type.length() > 0;
        hasSpecifiedTypeRE = null != typeRE && typeRE.length() > 0;

        // Name precedes re, being more specific
        if (hasSpecifiedName && hasSpecifiedNameRE) {
            hasSpecifiedNameRE = false;
        }
        if (hasSpecifiedNameRE) {
            namePattern = Pattern.compile(nameRE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }

        // Type precedes type re, being more specific
        if (hasSpecifiedType && hasSpecifiedTypeRE) {
            hasSpecifiedTypeRE = false;
        }
        if (hasSpecifiedTypeRE) {
            typePattern = Pattern.compile(typeRE, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }

        //------------------------------------------------------------------------
        // Adjust attributes if need be.
        //
        // This is a good opportunity to adjust the selection parameters - but
        // in order to do that we need to add some knowledge on how the most
        // generic methods/actions (on structures) behave.
        //
        // 'process' - needs both a 'location' and a 'name' (or a 'name-re'), since we
        //         do not provide processing of directories - only files.
        //
        // If the 'process' action does not have a 'name' (or 'name-re') specified,
        // we will try to determine a 'name' from a specified 'resource'. In
        // this case, we are making assumptions around other parameters to the
        // 'replace' action
        //------------------------------------------------------------------------
        if (method.equalsIgnoreCase("process")) {
            // We want at least a 'name' value if no 'name-re' was provided

            if (!hasSpecifiedNameRE && !hasSpecifiedName) {
                // Try to deduce a name from the resource filename
                String resource = attributes.get("resource");
                if (null != resource && resource.length() > 0) {
                    name = PathTool.getBasename(resource);
                    hasSpecifiedName = null != name && name.length() > 0;

                    String info = "'process' action did not have a 'name' specification. ";
                    info += " Assuming 'name' being same as specified 'resource' filename: ";
                    info += name;
                    info += ".";
                    log.warn(info);
                }
            }
        }
    }

    public boolean hasLocation() {
        return hasSpecifiedLocation;
    }

    public boolean hasName() {
        return hasSpecifiedName;
    }

    public boolean hasNameRE() {
        return hasSpecifiedNameRE;
    }

    public boolean hasType() {
        return hasSpecifiedType;
    }

    public boolean hasTypeRE() {
        return hasSpecifiedTypeRE;
    }

    public boolean hasConstraint() {
        return hasSpecifiedLocation | hasSpecifiedName | hasSpecifiedNameRE | hasSpecifiedType | hasSpecifiedTypeRE;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getNameRE() {
        return nameRE;
    }

    public boolean nameMatches(String indice) {
        Matcher m = namePattern.matcher(indice);
        return m.matches();
    }

    public String getType() {
        return type;
    }

    public String getTypeRE() {
        return typeRE;
    }

    public boolean typeMatches(String indice) {
        Matcher m = typePattern.matcher(indice);
        return m.matches();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("selection{");
        if (hasConstraint()) {
            if (hasSpecifiedLocation) {
                buf.append(" location=\"").append(location).append("\"");
            }
            if (hasSpecifiedName) {
                buf.append(" name=\"").append(name).append("\"");
            }
            if (hasSpecifiedNameRE) {
                buf.append(" name-re=\"").append(nameRE).append("\"");
            }
            if (hasSpecifiedType) {
                buf.append(" type=\"").append(type).append("\"");
            }
            if (hasSpecifiedTypeRE) {
                buf.append(" type-re=\"").append(typeRE).append("\"");
            }
        } else {
            buf.append(" *");
        }
        buf.append(" }");
        return buf.toString();
    }
}
