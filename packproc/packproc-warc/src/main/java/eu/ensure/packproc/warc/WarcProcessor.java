/*
 * Copyright (C) 2015 Frode Randers
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
 */
package eu.ensure.packproc.warc;

import eu.ensure.commons.io.MultiDigestInputStream;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.Action;
import eu.ensure.packproc.internal.FileTool;
import eu.ensure.packproc.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.archive.format.http.HttpResponseMessageObserver;
import org.archive.format.http.HttpResponseMessageParser;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;
import org.archive.io.warc.WARCWriter;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Operates on WARC file streams
 */
public class WarcProcessor implements ContainerStructureProcessor {
    private static final Logger log = LogManager.getLogger(WarcProcessor.class);

    private String alias = "WARC-processor"; // a reasonable default

    private ProcessorManager manager = null;

    // Normally the configuration will be conveyed through attributes and element text,
    // but it may also consist of sub-elements (not defining processors).
    private Map<String, String> configAttributes = new Hashtable<String, String>();
    private OMElement configElement = null;
    private String configText = null;

    //
    private Stack<Processor> outerProcessors = new Stack<Processor>(); // empty by default

    // Sub-processors (actions)
    private List<Action> actions = new Vector<Action>();


    public WarcProcessor() {
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

    @Override
    public void process(
            String name, InputStream inputStream, OutputStream outputStream, ProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {

        BasicProcessorContext basicContext = context.push(new BasicProcessorContext(name));
        boolean isMutableCall = null != outputStream;

        WARCReader reader = null;
        WARCWriter writer = null;

        try {
            // Package readers and writers
            reader = (WARCReader)WARCReaderFactory.get(name, inputStream, /* at first record? */ true);
            if (/* is mutable call? */ null != outputStream) {
                writer = null; // FOR NOW
            }

            Iterator<ArchiveRecord> rit = reader.iterator();

            // Iterate through objects in the input package
            with_next_entry:
            while (rit.hasNext()) {
                ArchiveRecord archiveRecord = rit.next();
                WarcRecordEntry structureEntry = new WarcRecordEntry(archiveRecord);

                String recordID = structureEntry.getRecordID();
                String url = structureEntry.getUrl();
                String contentType = structureEntry.getContentType();

                if (log.isInfoEnabled()) {
                    String info = "### [";
                    info += structureEntry.getType();
                    info += "] " + (null != url ? url : structureEntry.getName());
                    //
                    long size = structureEntry.getSize();
                    info += " (";
                    if (size > eu.ensure.commons.lang.Number.BYTES_MAX)
                        info += eu.ensure.commons.lang.Number.asHumanApproximate(size) + " or ";
                    info += size + " bytes)";
                    log.info(info);
                }

                // There is no notion of a directory in the WARC file, and "file names"
                // are not really interesting. Rather we want to look for file types
                // or URLs

                MultiDigestInputStream entryInputStream = null;
                try {
                    // Directories are not processed per se
                    Iterator<Action> ait = actions.iterator();
                    while (ait.hasNext()) {
                        Action action = ait.next();

                        if (action.matchOnName(structureEntry.getName())
                         || action.matchOnType(structureEntry.getContentType())) {

                            if (log.isDebugEnabled()) {
                                log.debug(me() + ":process container");
                            }

                            entryInputStream = new MultiDigestInputStream(structureEntry.getInputStream());

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
                                            // TODO addEntry(subOutputFile, structureEntry, archiveOutputStream);
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
                                        structureEntry, entryInputStream, /* archiveOutputStream */ null, basicContext
                                );
                                continue with_next_entry; // since we operated on a unique entry
                            }
                        }
                    }

                    if (isMutableCall /* && !addedEntries.contains(structureEntry.getName()) */) {
                        // We may safely copy file
                        // TODO copyEntry(structureEntry, entryInputStream, archiveOutputStream);
                    }
                } finally {
                    if (structureEntry.isResponseRecord()) {
                        // Collect bitstream information - this is where we associate _actual_ values,
                        // i.e. calculated checksums and calculated byte lengths.
                        Map<String, String> bitstreamInfo = new HashMap<>();

                        // OBSERVE: The following might not be completely valid in all circumstances,
                        // as InputStream.getSize() only returns the number of bytes that you can read
                        // and not necessarily the number of bytes in the stream. But in this case,
                        // I believe it to be valid...
                        if (null != entryInputStream && entryInputStream.getSize() > 0) {
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
                                    for (int i = 0; i < digest.length; i++) {
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
                            int start = url.startsWith("/") ? 0 : 1; /* skip [example1]/content/... */

                            String[] parts = url.split("/");
                            for (int i = start; i < parts.length; i++) {
                                contentStream = new File(contentStream, parts[i]);
                            }
                            bitstreamInfo.put("fileName", parts[parts.length - 1]);

                            String path = contentStream.getPath().replace("\\", "/"); // in case we're on Windoze
                            context.associate("CALCULATED", path, path, bitstreamInfo);
                        }
                    }
                }
            }
        } finally {
            if (null != writer) writer.close();
            if (null != reader) reader.close();

            context.pop();
        }
    }
}
