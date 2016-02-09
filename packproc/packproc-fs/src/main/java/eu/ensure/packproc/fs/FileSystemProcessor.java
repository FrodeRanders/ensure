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

import eu.ensure.vopn.io.MultiDigestInputStream;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.Action;
import eu.ensure.packproc.internal.FileTool;
import eu.ensure.packproc.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Operates on file container streams
 */
public class FileSystemProcessor implements DispersedStructureProcessor {
    private static final Logger log = LogManager.getLogger(FileSystemProcessor.class);

    private String alias = "fs-processor"; // a reasonable default

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


    public FileSystemProcessor() {
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
     * Generic entry to the file system-processor. Will route to more specific actions based on the
     * plugin-specific configuration.
     * <p>
     * <p>
     * @param inputNode
     * @throws java.io.IOException
     * @throws eu.ensure.packproc.ProcessorException
     * @throws ClassNotFoundException
     */
    public void process(String name, File inputNode, ProcessorContext context)
            throws IOException, ProcessorException, ClassNotFoundException {

        if (log.isInfoEnabled()) {
            log.info("Processing " + inputNode.getPath());
        }

        BasicProcessorContext basicContext = context.push(new BasicProcessorContext(name));

        try {
            // Iterate through objects
            File[] children = inputNode.listFiles();
            int i = 0;

            with_next_entry:
            while (i < children.length) {
                File child = children[i++];
                FileSystemEntry structureEntry = new FileSystemEntry(child);

                String entryName = structureEntry.getName();
                if (structureEntry.isDirectory()) {
                    entryName += "/";
                }

                if (log.isInfoEnabled()) {
                    log.info("");
                    String info = "### " + entryName;
                    log.info(info);
                }

                // Directories are not processed per se. The following construction
                // implements a depth-first approach to traversing a file system.
                if (structureEntry.isDirectory()) {
                    process(entryName, structureEntry.getWrappedObject(), basicContext);
                    continue; // with next entry
                }

                MultiDigestInputStream entryInputStream = null;
                try {
                    Iterator<Action> ait = actions.iterator();
                    while (ait.hasNext()) {
                        Action action = ait.next();

                        if (action.matchOnName(structureEntry.getName())) {
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
                                    InputStream subInputStream = null;
                                    OutputStream subOutputStream = null;
                                    try {
                                        subInputStream = new BufferedInputStream(new FileInputStream(subInputFile));

                                        // Run it through the processor manager which knows what to do with it
                                        manager.applyOnContainerWithStructure(
                                                action.getProcessor(), action.getMethod(),
                                                structureEntry.getName(), subInputStream, subOutputStream, basicContext
                                        );
                                    } finally {
                                        if (null != subInputStream) subInputStream.close();
                                        if (null != subInputFile && subInputFile.exists()) subInputFile.delete();
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
                                        structureEntry, entryInputStream, /* OutputStream */ null, basicContext
                                );
                                continue with_next_entry; // since we operated on a unique entry
                            }
                        }
                    }
                } finally {
                    try {
                        if (! structureEntry.isDirectory() && null != entryInputStream) {
                            // Collect bitstream information - this is where we associate _actual_ values,
                            // i.e. calculated checksums and calculated byte lengths.
                            Map<String, String> bitstreamInfo = new HashMap<String, String>();

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
                                        for (int j=0;j<digest.length;j++) {
                                            hexString.append(Integer.toHexString(0xFF & digest[j]));
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
                                for (int j=start; j < parts.length; j++) {
                                    contentStream = new File(contentStream, parts[j]);
                                }
                                bitstreamInfo.put("fileName", parts[parts.length-1]);

                                String path = contentStream.getPath().replace("\\", "/"); // in case we're on Windoze
                                context.associate("CALCULATED", path, path, bitstreamInfo);
                            }
                        }
                    }
                    finally {
                        if (null != entryInputStream) {
                            entryInputStream.close();
                            entryInputStream = null;
                        }
                    }
                }
            }
        } finally {
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
