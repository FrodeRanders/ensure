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

import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.model.StructureEntry;
import eu.ensure.packproc.model.StructureOutputStream;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;


public class FileTool {
    private static final Logger log = Logger.getLogger(FileTool.class);

    protected String alias = null;

    public FileTool(
        String alias
    ) {
        this.alias = alias;
    }

    protected String getPrefix() {
        return alias + "-";
    }

    public void addEntry(
            File inputFile,
            StructureEntry inputEntry,
            StructureOutputStream structureOutputStream
    ) throws IOException {
        RandomAccessFile raf = null;
        try {
            // Wrap the input and output streams in some nice garment
            WritableByteChannel outputChannel = Channels.newChannel(structureOutputStream);

            // Wrap this temporary file in a file channel
            raf = new RandomAccessFile(inputFile, "r");
            FileChannel fileChannel = raf.getChannel();

            try {
                structureOutputStream.replaceEntry(inputEntry, inputFile);
                fileChannel.transferTo(0L, inputFile.length(), outputChannel);
            } finally {
                structureOutputStream.closeEntry();
            }
        } finally {
            // Close temporary resources and such
            if (null != raf) raf.close(); // closes fileChannel as well
        }
    }

    public void copyEntry(
            StructureEntry fileEntry,
            InputStream entryInputStream,
            StructureOutputStream structureOutputStream
    ) throws IOException, ProcessorException {

        File tmpFile = null;
        RandomAccessFile raf = null;
        try {
            if (fileEntry.isDirectory() || null == entryInputStream) {
                // Just copy entry - ignore contents
                try {
                    structureOutputStream.copyEntry(fileEntry);
                } finally {
                    structureOutputStream.closeEntry();
                }
            } else {
                // Wrap the input and output streams in some nice garment
                ReadableByteChannel inputChannel = Channels.newChannel(entryInputStream);
                WritableByteChannel outputChannel = Channels.newChannel(structureOutputStream);

                // Create a temporary random-access file
                tmpFile = File.createTempFile(getPrefix(), ".raw");

                // Wrap this temporary file in a file channel
                raf = new RandomAccessFile(tmpFile, "rw");
                FileChannel fileChannel = raf.getChannel();

                // Transfer from input stream to temporary file
                long size = fileEntry.getSize(); // the decompressed size in case of compressed entries
                fileChannel.transferFrom(inputChannel, 0L, size);

                // Copy the entry
                try {
                    structureOutputStream.copyEntry(fileEntry);
                    fileChannel.transferTo(0L, size, outputChannel);
                } finally {
                    structureOutputStream.closeEntry();
                }
            }
        } finally {
            // Close temporary resources and such
            if (null != raf) raf.close(); // closes fileChannel as well

            // Remove temporary file
            if (null != tmpFile) tmpFile.delete();
        }
    }

    /*
     * We need this exact implementation where we only read as much from the input stream
     * as says the fileEntry.getSize() because we are operating on a compressed input stream
     * where file entries are "muxed" onto the same InputStream!
     */
    public File extractEntry(
            StructureEntry fileEntry,
            InputStream entryInputStream
    ) throws IOException, ProcessorException {

        File file = null;
        RandomAccessFile raf = null;
        try {
            // Wrap the input stream in some nice garment
            ReadableByteChannel inputChannel = Channels.newChannel(entryInputStream);

            // Create a random-access file
            file = File.createTempFile(getPrefix(), ".raw");

            // Wrap this file in a file channel
            raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel();

            // Transfer from input stream to file
            long size = fileEntry.getSize(); // the decompressed size in case of ZIP
            fileChannel.transferFrom(inputChannel, 0L, size);

        } finally {
            // Close resources and such
            if (null != raf) raf.close(); // closes fileChannel as well
        }

        return file;
    }
}