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

import eu.ensure.packproc.model.Processor;

/**
 * An action on a file, such as "process", ...
 * May or may not have a selection (constraints on which file to apply action to).
 */
public class Action {
    private EntrySelection selection = null;
    private Processor processor = null;
    private String method = null;
    private boolean matchedRegularExpression = false;


    public Action(EntrySelection selection, Processor processor, String method) {
        this.selection = selection;
        this.processor = processor;
        this.method = method;
    }

    public boolean hasSelection() {
        return null != selection;
    }

    public boolean wasMatchOnRegularExpression() {
        return matchedRegularExpression;
    }

    public EntrySelection getSelection() {
        return selection;
    }

    public Processor getProcessor() {
        return processor;
    }

    public String getMethod() {
        return method;
    }

    public boolean match(String path) {
        matchedRegularExpression = false;

        // Possibly align path with the provided location. If it was "absolute" within the structure,
        // then we will make the path "absolute" as well.
        if (selection.hasLocation() && selection.getLocation().startsWith("/") && !path.startsWith("/")) {
            path = "/" + path;
        }
        //System.out.println("Match? Matching " + path + " against action (" + selection + ")");
        
        // Check whether we should match entry against the entry selection
        // (which is based on the "location", "re" and "name" attributes...
        if (null != selection && !selection.hasConstraint()) {
            //System.out.println("File " + path + " does not match anything");
            return false;
        }

        // We have a specified constraint - matching a directory path or a entry name
        String[] parts = path.split("/");
        String entryName = parts[parts.length-1];
        int endPos = path.lastIndexOf(entryName);
        String baseName = path.substring(0, endPos); // including trailing "/"

        // Does it match the location?
        if (selection.hasLocation()) {
            if (selection.hasName() || selection.hasRE()) {
                if (!selection.getLocation().equalsIgnoreCase(baseName)) {
                    return false;
                }
                //System.out.println("Match? Basename " + baseName + " of path " + path + " matches location " + selection.getLocation());
            } else {
                if (!selection.getLocation().equalsIgnoreCase(path)) {
                    return false;
                }
                //System.out.println("Match? Path " + path + " matches location " + selection.getLocation());
            }
        }

        // Does it match name of entry?
        if (selection.hasName()) {
            if (!selection.getName().equalsIgnoreCase(entryName)) {
                return false;
            }
            //System.out.println("Match? Entry " + entryName + " matches name " + selection.getContextName());
        }

        // Does the name match a regular expression
        if (selection.hasRE()) {
            if (!entryName.matches(selection.getRE())) {
                return false;
            }
            matchedRegularExpression = true;
            //System.out.println("Match? Entry " + entryName + " matches RE " + selection.getRE());
        }

        // By now, we must match some entry selection constraint
        return true;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        //buf.append("action[").append(processor.hashCode()).append("]{ ");
        buf.append("action{ ");
        String className = processor.getClass().getName();
        String[] parts = className.split("\\.");
        if (parts.length > 0) {
            buf.append(parts[parts.length-1]);
        } else {
            buf.append(className);
        }
        buf.append(":").append(method);
        if (null != selection) {
            buf.append(" on ").append(selection);
        }
        buf.append(" }");
        return buf.toString();
    }
}
