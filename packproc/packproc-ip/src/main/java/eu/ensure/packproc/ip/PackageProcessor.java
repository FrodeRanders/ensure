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
package eu.ensure.packproc.ip;

import eu.ensure.commons.io.MultiDigestInputStream;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.Action;
import eu.ensure.packproc.internal.FileTool;
import eu.ensure.packproc.model.*;

import eu.ensure.commons.lang.Number;

import org.apache.axiom.om.OMElement;
import org.apache.commons.compress.archivers.*;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Operates on file container streams
 */
public class PackageProcessor implements ContainerStructureProcessor {
    private static final Logger log = Logger.getLogger(PackageProcessor.class);

    private String alias = "ip-processor"; // a reasonable default

    private ProcessorManager manager = null;

    // Knows how to open various types of (compression) containers (ZIP, TAR, ...)
    private ArchiveStreamFactory factory = new ArchiveStreamFactory();

    // Normally the configuration will be conveyed through attributes and element text,
    // but it may also consist of sub-elements (which then may not define processors).
    private Map<String, String> configAttributes = new Hashtable<String, String>();
    private OMElement configElement = null;
    private String configText = null;

    //
    private Stack<Processor> outerProcessors = new Stack<Processor>(); // empty by default

    // Sub-processors (actions)
    private List<Action> actions = new Vector<Action>();

    //
    // If a package in addition to being read and processed is also modified, i.e.
    // we are not writing the same entries to an (optional) output stream, we must
    // track changes to the package. Changes such as adding files, removing files or
    // directories must be tracked so that we do not accidentally write the originals
    // to the output stream.
    //
    private final Set<String> addedEntries = new HashSet<String>();
    private final Set<String> removedEntries = new HashSet<String>();


    public PackageProcessor() {
    }

    public String getAlias() {
        return alias;
    }

    private String me() {
        return alias; //  + "[" + hashCode() + "]";
    }

    public void initialize(
            ProcessorManager manager,
            Map<String, String> attributes,
            String text,
            String alias,
            Stack<Processor> outerProcessors
    ) {
        this.manager = manager;
        this.configAttributes = attributes;
        this.configText = text;
        this.alias = alias;
        this.outerProcessors = outerProcessors;
    }

    public void define(List<Action> actions) {
        this.actions = actions;
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setConfiguration(OMElement configuration) {
        this.configElement = configuration;
    }

    /**
     * Structural modifications.
     */
    enum StructuralModificationType {
        ADDED,
        REMOVED
    }

    /**
     * Tracks structural changes to an information package.
     * <p>
     * Entries in a package may not be ordered, which is typically the case with entries in ZIP-files,
     * so that additional files may be appended to existing entries. We need to handle this kind of
     * behavior.
     */
    public void trackStructuralChange(String path, StructuralModificationType modification) {
        switch (modification) {
            case REMOVED:
                removedEntries.add(path);
                break;

            case ADDED:
                String[] parts = path.split("[\\\\/]");
                if (parts.length > 0) {
                    String partial = "";
                    for (int i=0; i<parts.length-1; i++) {
                        partial += parts[i];
                        partial += "/"; // The POSIX separator is never wrong!

                        addedEntries.add(partial);
                    }
                }
                break;
        }
    }

    private void reportAddedEntry(String path) {
        if (outerProcessors.empty()) {
            trackStructuralChange(path, StructuralModificationType.ADDED);
        }
        else {
            Processor parent = outerProcessors.peek();
            if (null != parent && parent instanceof PackageProcessor) {
                ((PackageProcessor)parent).trackStructuralChange(path, StructuralModificationType.ADDED);
            }
        }
    }

    private void reportRemovedEntry(String path) {
        if (outerProcessors.empty()) {
            trackStructuralChange(path, StructuralModificationType.REMOVED);
        }
        else {
            Processor parent = outerProcessors.peek();
            if (null != parent && parent instanceof PackageProcessor) {
                ((PackageProcessor)parent).trackStructuralChange(path, StructuralModificationType.REMOVED);
            }
        }
    }

    /**
     * Adds an entry.
     */
    public void add(
            PackageEntry currentEntry, // where to add
            StructureOutputStream structureOutputStream // in what to add
    ) throws IOException, ProcessorException {
        String info = "Adding files to an information package is not supported!";
        throw new UnsupportedOperationException(info);
    }

    /**
     * Replaces an entry.
     */
    public void replace(
            PackageEntry currentEntry, // where to add
            StructureOutputStream structureOutputStream // in what to add
    ) throws IOException, ProcessorException {
        String info = "Adding files to an information package is not supported!";
        throw new UnsupportedOperationException(info);
    }

    /**
     * Generic entry to the information package-processor.
     * <p>
     * Will route to more specific actions based on the plugin-specific configuration.
     * <p>
     * @param name - name of entity (information package)
     * @param inputStream - input stream onto information package
     * @param outputStream - [optionally] output stream onto (new) information package
     * @param context - a context for this processor
     * @throws IOException - if file I/O fails
     * @throws ArchiveException - if information package has unknown packaging format
     * @throws ProcessorException - if processing of information package fails
     * @throws ClassNotFoundException - if action not found
     */
    public void process(String name, InputStream inputStream, OutputStream outputStream, ProcessorContext context)
            throws IOException, ArchiveException, ProcessorException, ClassNotFoundException {

        BasicProcessorContext basicContext = context.push(new BasicProcessorContext(name));
        boolean isMutableCall = null != outputStream;

        ArchiveInputStream archiveInputStream = null;
        PackageOutputStream archiveOutputStream = null;
        try {
            // Package readers and writers
            archiveInputStream = factory.createArchiveInputStream(
                    new BufferedInputStream(inputStream)
            );

            if (isMutableCall) {
                archiveOutputStream = PackageOutputStream.createOutputStreamFrom(archiveInputStream, outputStream);
            }

            // Iterate through objects in the input package
            ArchiveEntry archiveEntry = null;

            with_next_entry:
            while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {

                String entryName = archiveEntry.getName();
                if (archiveEntry.isDirectory()) {
                    entryName += "/";
                }

                if (log.isInfoEnabled()) {
                    log.info("");
                    String info = "### " + entryName;
                    long size = archiveEntry.getSize();
                    info += " (~" + Number.asHumanApproximate(size) + " or " + size + " bytes)";
                    log.info(info);
                }

                // TODO: Triggers for "/" will have to be processed manually here!

                MultiDigestInputStream entryInputStream = null;
                try {
                    PackageEntry structureEntry = new PackageEntry(archiveEntry);
                    entryInputStream = new MultiDigestInputStream(archiveInputStream); // As it happens to be!

                    // Directories are not processed per se
                    Iterator<Action> ait = actions.iterator();
                    while (ait.hasNext()) {
                        Action action = ait.next();

                        if (action.match(structureEntry.getName())) {
                            if (log.isDebugEnabled()) {
                                log.debug(me() + ":process container");
                            }
                            Processor processor = action.getProcessor();
                            if (processor instanceof ContainerStructureProcessor) {
                                if (action.getMethod().equalsIgnoreCase("process")) {
                                    //-----------------------------------------------------------------------------
                                    // Since we are referring to a structure (processor), we are probably just
                                    // going to process an embedded TAR-file (or the like). We create a
                                    // temporary file and recursively feed it to the processor manager...
                                    //-----------------------------------------------------------------------------
                                    File subInputFile = extractEntry(structureEntry, entryInputStream);
                                    File subOutputFile = null;
                                    if (isMutableCall) {
                                        subOutputFile = File.createTempFile("temporary-processed", ".package");
                                    }
                                    try {
                                        InputStream subInputStream = null;
                                        OutputStream subOutputStream = null;
                                        try {
                                            subInputStream = new BufferedInputStream(new FileInputStream(subInputFile));
                                            if (isMutableCall) {
                                                subOutputStream = new BufferedOutputStream(new FileOutputStream(subOutputFile));
                                            }

                                            // Run it through the processor manager which knows what to do with it
                                            manager.applyOnContainerWithStructure(
                                                    action.getProcessor(), action.getMethod(),
                                                    structureEntry.getName(), subInputStream, subOutputStream, basicContext
                                            );
                                        } finally {
                                            if (null != subInputStream) subInputStream.close();
                                            if (null != subOutputStream) subOutputStream.close();
                                        }

                                        if (isMutableCall) {
                                            // Add the temporary file to the output stream instead of the original
                                            addEntry(subOutputFile, structureEntry, archiveOutputStream);
                                        }
                                    } finally {
                                        if (null != subInputFile && subInputFile.exists()) subInputFile.delete();
                                        if (null != subOutputFile && subOutputFile.exists()) subOutputFile.delete();
                                    }
                                    continue with_next_entry; // since we operated on a unique entry

                                } else {
                                    // Unknown operation on a container file
                                    throw new ProcessorException("Unknown action on container: " + action.getMethod());
                                }
                            } else if (processor instanceof FileProcessor) {
                                //---------------------------------------------------------------------------------
                                // Since we are referring to a file processor, we will just pass the entry with it's
                                // input stream back to the processor manager that will know what to do with it.
                                //---------------------------------------------------------------------------------
                                manager.applyOnEntry(
                                        action.getProcessor(), action.getMethod(),
                                        structureEntry, entryInputStream, archiveOutputStream, basicContext
                                );
                                continue with_next_entry; // since we operated on a unique entry
                            }
                        }
                    }

                    if (isMutableCall && !addedEntries.contains(structureEntry.getName())) {
                        // We may safely copy file
                        copyEntry(structureEntry, entryInputStream, archiveOutputStream);
                    }
                } finally {
                    /*
                     * Don't close the entryInputStream! It is just a reference to the archiveInputStream
                     * which we want to continue operating upon.
                     */

                    if (! archiveEntry.isDirectory()) {
                        // Collect bitstream information - this is where we associate _actual_ values,
                        // i.e. calculated checksums and calculated byte lengths.
                        Map<String, String> bitstreamInfo = new HashMap<String, String>();

                        // OBSERVE: The following might not be completely valid in all circumstances,
                        // as InputStream.getSize() only returns the number of bytes that you can read
                        // and not necessarily the number of bytes in the stream. But in this case,
                        // I believe it to be valid...
                        if (entryInputStream.getSize() > 0) {
                            bitstreamInfo.put("size", "" + entryInputStream.getSize());

                            Map<String, byte[]> digests = entryInputStream.getDigests();
                            for (String key : digests.keySet()) {
                                byte[] digest = digests.get(key);

                                if (digest.length == 8) {
                                    ByteBuffer buf = ByteBuffer.wrap(digest);
                                    String value = "" + buf.getLong();
                                    bitstreamInfo.put(key, value);
                                } else {
                                    StringBuffer hexString = new StringBuffer();
                                    for (int i=0;i<digest.length;i++) {
                                        hexString.append(Integer.toHexString(0xFF & digest[i]));
                                    }
                                    String value = hexString.toString();
                                    bitstreamInfo.put(key, value);
                                }
                            }

                            // Create a package-relative path...
                            File top = new File("/");
                            File contentStream = top; // starting point relative to top

                            // ...and reassemble
                            int start = entryName.startsWith("/") ? 0 : 1; /* skip [example1]/content/... */

                            String[] parts = entryName.split("/");
                            for (int i=start; i < parts.length; i++) {
                                contentStream = new File(contentStream, parts[i]);
                            }
                            bitstreamInfo.put("fileName", parts[parts.length-1]);

                            String path = contentStream.getPath().replace("\\", "/"); // in case we're on Windoze
                            context.associate("CALCULATED", path, path, bitstreamInfo);
                        }
                    }
                }
            }
        } finally {
            if (null != archiveOutputStream) archiveOutputStream.close();
            if (null != archiveInputStream) archiveInputStream.close();

            context.pop();
        }
    }

    /**
     *
     * @param entry
     * @param entryInputStream
     * @return
     * @throws eu.ensure.packproc.ProcessorException
     * @throws java.io.IOException
     */
    private File extractEntry(
            StructureEntry entry,
            InputStream entryInputStream
    ) throws ProcessorException, IOException {

        FileTool fileTool = new FileTool(alias);
        return fileTool.extractEntry(entry, entryInputStream);
    }

    /**
     *
     */
    private void addEntry(
            File inputFile,
            StructureEntry inputEntry,
            StructureOutputStream structureOutputStream
    ) throws IOException {

        FileTool fileTool = new FileTool(alias);
        fileTool.addEntry(inputFile, inputEntry, structureOutputStream);
    }

    /**
     *
     */
    private void copyEntry(
            StructureEntry entry,
            InputStream entryInputStream,
            StructureOutputStream structureOutputStream
    ) throws IOException, ProcessorException {

        FileTool fileTool = new FileTool(alias);
        fileTool.copyEntry(entry, entryInputStream, structureOutputStream);
    }

    /**
     * 
     * @return
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(me()).append("[ ");
        for (Map.Entry<String, String> stringStringEntry : configAttributes.entrySet()) {
            String value = stringStringEntry.getValue();
            buf.append(stringStringEntry.getKey()).append("=\"").append(null != value ? value : "").append("\" ");
        }
        buf.append("]{ ");
        for (Action action : actions) {
            buf.append(action);
            buf.append(" ");
        }
        buf.append("}");
        return buf.toString();
    }
}
