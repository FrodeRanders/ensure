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

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Deals with the attributes of a structure processor in order to identify entries
 * within the structure...
 */
public class EntrySelection {
    private static final Logger log = Logger.getLogger(EntrySelection.class);

    private String location;
    private String re;
    private Pattern pattern = null;
    private String name;

    private boolean hasSpecifiedLocation = false;
    private boolean hasSpecifiedName = false;
    private boolean hasSpecifiedRE = false;

    public EntrySelection(Map<String, String> attributes, String method) {
        location = attributes.get("location");
        re = attributes.get("re");
        name = attributes.get("name");

        hasSpecifiedLocation = null != location && location.length() > 0;
        hasSpecifiedName = null != name && name.length() > 0;
        hasSpecifiedRE = null != re && re.length() > 0;

        // Name precedes re, being more specific
        if (hasSpecifiedName && hasSpecifiedRE) {
            hasSpecifiedRE = false;
        }

        if (hasSpecifiedRE) {
            pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        }

        //------------------------------------------------------------------------
        // Adjust attributes if need be.
        //
        // This is a good opportunity to adjust the selection parameters - but
        // in order to do that we need to add some knowledge on how the most
        // generic methods/actions (on structures) behave.
        //
        // 'process' - needs both a 'location' and a 'name' (or a 're'), since we
        //         do not provide processing of directories - only files.
        //
        // If the 'process' action does not have a 'name' (or 're') specified,
        // we will try to determine a 'name' from a specified 'resource'. In
        // this case, we are making assumptions around other parameters to the
        // 'replace' action
        //------------------------------------------------------------------------
        if (method.equalsIgnoreCase("process")) {
            // We want at least a 'name' value if no 're' was provided

            if (!hasSpecifiedRE && !hasSpecifiedName) {
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

    public boolean hasRE() {
        return hasSpecifiedRE;
    }

    public boolean hasName() {
        return hasSpecifiedName;
    }

    public boolean hasConstraint() {
        return hasSpecifiedLocation | hasSpecifiedRE | hasSpecifiedName;
    }

    public String getLocation() {
        return location;
    }

    /**
     * Use as follows:
     * <pre>
     * StructureSelection selection = ...;
     * Pattern p = selection.getRegExPattern();
     * Matcher m = p.matcher("some text example");
     * if (m.find()) {
     *     String klopp = m.group(1);
     * }
     * </pre>
     * @return Pattern, suitable for matching entries against a specified regular expression
     */
    public Pattern getRegExPattern() {
        return pattern;
    }

    public String getRE() {
        return re;
    }
    
    public String getName() {
        return name;
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
            if (hasSpecifiedRE) {
                buf.append(" re=\"").append(re).append("\"");
            }
        } else {
            buf.append(" *");
        }
        buf.append(" }");
        return buf.toString();
    }
}
