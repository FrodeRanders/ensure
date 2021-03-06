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
package eu.ensure.packproc.model;

import eu.ensure.packproc.ProcessorException;
import org.apache.commons.compress.archivers.ArchiveException;

import java.io.File;
import java.io.IOException;

/**
 * Some kind of processor operating on dispersed _structures_ such as the filesystem.
 */
public interface DispersedStructureProcessor extends Processor {
    /**
     * Processes a non-mutable file system structure
     * <p>
     * @param name - entity name
     * @param inputNode - input node onto entity
     * @throws java.io.IOException
     * @throws org.apache.commons.compress.archivers.ArchiveException
     * @throws eu.ensure.packproc.ProcessorException
     * @throws ClassNotFoundException
     * @throws java.io.FileNotFoundException
     */
    void process(String name, File inputNode, ProcessorContext context)
            throws IOException, ArchiveException, ProcessorException, ClassNotFoundException;
}
