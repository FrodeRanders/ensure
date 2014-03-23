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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Some kind of processor operating on _files_
 */
public interface FileProcessor extends Processor {
    /**
     * Process an explicit file (optionally mutable)
     * <p/>
     * @param inputStream - input stream from file
     * @param outputStream - output stream to file (optionally null!)
     * @param context - processor context
     */
    void process(
           InputStream inputStream,
           StructureOutputStream outputStream,
           ProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException;
}
