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
package eu.ensure.packproc.fs;

import eu.ensure.packproc.model.StructureEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileSystemEntry extends StructureEntry<File> {
    private File entry = null;

    /**
     * This constructor is used to wrap a File.
     * <p>
     * @param entry
     */
    public FileSystemEntry(File entry) {
        this.entry = entry;
    }

    public  String getName() {
        return entry.getAbsolutePath();
    }
    
    public  boolean isDirectory() {
        return entry.isDirectory();
    }

    public long getSize() {
        return entry.length();
    }

    public File getWrappedObject()
    {
        return entry;
    }

    public InputStream getInputStream() throws FileNotFoundException {
        if (entry.isFile()) {
            return new FileInputStream(entry);
        }
        return null; // otherwise
    }
}
