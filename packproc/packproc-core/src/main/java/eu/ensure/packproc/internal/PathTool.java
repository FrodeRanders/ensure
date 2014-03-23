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

import java.util.HashSet;

/**
 * Tools to apply on paths
 */
public class PathTool {
    private PathTool() {}

    /**
     * Returns basename of path, i.e. "/path/to/file" returns "file".
     * <p/>
     * @param path
     * @return
     */
    public static String getBasename(String path) {
        String[] parts = path.split("[\\\\/]");
        if (parts.length > 0) {
            return parts[parts.length-1];
        } else {
            return path;
        }
    }

    /**
     * Returns dirname of path, i.e. "/path/to/file" returns "/path/to".
     * <p/>
     * @param path
     * @return
     */
    public static String getDirname(String path) {
        String[] parts = path.split("[\\\\/]");
        if (parts.length > 1) {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < parts.length-1; i++) {
                buf.append(parts[i]);
                if (i < parts.length-1) {
                    buf.append("/");
                }
            }
            return buf.toString();
        } else {
            return path;
        }
    }

    /**
     * Generates a substitute path, where basename in original path
     * is replaced by filename.
     * <p/>
     * We have to generate a path, so that "path/to/old-file" is replaced
     * by "path/to/replacement-file".
     * <p/>
     * @param path
     * @param filename
     * @return
     */
    public static String substituteFilenameInPath(String path, String filename) {
        String accumulatedPath = "";

        String[] parts = path.split("[\\\\/]");
        if (parts.length > 0) {
            for (int i=0; i < parts.length-1; i++) {
                accumulatedPath += parts[i];
                accumulatedPath += "/";
            }
        }
        accumulatedPath += filename;
        return accumulatedPath;
    }

    /**
     * Determines whether we are referring to an entry within a substructure.
     * <p/>
     * By checking whether the entry (a file or a directory) or any parent directories
     * are found among the 'removedEntries', we may be able to deduce that an entry should
     * not be processed.
     * <p/>
     * @param path
     * @param subStructures
     * @return
     */
    public static boolean isInSubstructure(String path, HashSet<String> subStructures) {
        String[] parts = path.split("[\\\\/]");
        if (parts.length > 0) {
            String partialPath = "";
            for (int i = 0; i < parts.length - 1; i++) {
                partialPath += parts[i];
                partialPath += "/";

                if (subStructures.contains(partialPath)) {
                    return true;
                }
            }
        }
        return false;
    }
}
