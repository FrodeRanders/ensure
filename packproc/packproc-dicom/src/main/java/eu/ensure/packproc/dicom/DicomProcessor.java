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
package eu.ensure.packproc.dicom;

import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.Action;
import eu.ensure.packproc.internal.FileTool;
import eu.ensure.packproc.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;

import java.io.*;
import java.util.*;

/**
 * Operates on DICOM (container) streams
 */
public class DicomProcessor implements ContainerStructureProcessor {
    private static final Logger log = LogManager.getLogger(DicomProcessor.class);

    private String alias = "dicom-processor"; // a reasonable default

    private ProcessorManager manager = null;


    // Normally the configuration will be conveyed through attributes and element text,
    // but it may also consist of sub-elements (not defining processors).
    private Map<String, String> configAttributes = new Hashtable<String, String>();
    private OMElement configElement = null;
    private String configText = null;

    // Defines tags that we are especially interested in collecting
    public final String[] interestingTags = {
        "0002,0010", // Transfer Syntax UID := Implicit VR Little Endian (1.2.840.10008.1.2)
        "0008,0005", // Specific CharacterSet := ISO_IR 138

        // DICOM Information Hierarchy
        "0010,0020", // Patient ID (Patient level)
        "0020,000D", // Study Instance UID (Study level)
        "0020,000E", // Series Instance UID (Series level)
        "0008,0018", // SOP Instance UID (Image level)

        // Derivation information
        "0008,2111"  // Derivation Description
    };

    //
    private Stack<Processor> outerProcessors = new Stack<Processor>(); // empty by default

    // Sub-processors (actions)
    private List<Action> actions = new Vector<Action>();

    public DicomProcessor() {
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
     * extracts an entry in DICOM file
     * <p>
     * @param dicomObject
     * @param objectInputStream
     * @throws IOException
     * @throws eu.ensure.packproc.ProcessorException
     */
    public void extractInformation(
           final StructureEntry dicomObject,
           final InputStream objectInputStream,
           final StructureOutputStream structureOutputStream,
           ProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {

        //log.info(me() + ":extractInformation dicom-element");
        DicomElement element = ((DicomStructureEntry)dicomObject).getWrappedObject();

        // Class of element seems to be either:
        //    org.dcm4che2.data.SimpleDicomElement, or
        //    org.dcm4che2.data.SequenceDicomElement

        // Intercept sequence (SQ) elements
        if (element.hasDicomObjects()) {
            // Perform some kind of logging for SQ elements
            String info = ValueHandler.indentDepth((DicomProcessorContext) context);
            info += ValueHandler.getTagInfo(element);

            if (element.hasItems()) {
                info += "has " + element.countItems() + " item(s)";
            }
            if (element.hasFragments()) {
                if (info.length() > 0) {
                    info += " and  ";
                }
                info = "has fragment(s)";
            }
            log.debug(info);

            int items = element.countItems();
            for (int i=0; i < items; i++) {
                DicomObject obj = element.getDicomObject();
                Iterator<DicomElement> deit = obj.iterator();
                try {
                    DicomProcessorContext dicomContext = context.push(
                            new DicomProcessorSubContext((DicomProcessorContext)context)
                    );
                    while (deit.hasNext()) {
                        DicomElement elem = deit.next();
                        StructureEntry structureEntry = new DicomStructureEntry(elem);
                        manager.applyOnEntry(this, "extractInformation", structureEntry, objectInputStream, /* OutputStream */ null, dicomContext);
                    }
                } finally {
                    context.pop();
                }
            }
        }
        else {
            try {
                ValueHandler.process(element, (DicomProcessorContext) context);
            }
            catch (Exception e) {
                String tagName = DicomDictionary.tag2Name(element);
                String info = "Failed to analyze element " + tagName + ": ";
                info += Stacktrace.asString(e);
                log.warn(info);
            }
        }
    }

    /**
     * Generic entry to the DICOM-processor. Will route to more specific actions based on the
     * plugin-specific configuration.
     * <p>
     * <p>
     * @param inputStream
     * @param context
     * @throws java.io.IOException
     * @throws ProcessorException
     * @throws ClassNotFoundException
     */
    public void process(String name, InputStream inputStream, OutputStream outputStream, ProcessorContext context)
            throws IOException, ProcessorException, ClassNotFoundException {

        DicomProcessorContext dicomContext = context.push(new DicomProcessorContext(name, interestingTags));
        boolean isMutableCall = null != outputStream;

        DicomInputStream dicomInputStream = null;
        DicomOutputStream dicomOutputStream = null;
        try {
            // Package readers and writers
            dicomInputStream = new DicomInputStream(inputStream);

            if (isMutableCall) {
                dicomOutputStream = new DicomOutputStream(outputStream);
            }

            // Iterate through objects
            DicomObject dicomObject = dicomInputStream.readDicomObject();
            Iterator<DicomElement> deit = dicomObject.iterator();

            with_next_entry:
            while (deit.hasNext()) {
                DicomElement dicomElement = deit.next();

                InputStream objectInputStream = null;
                try {
                    StructureEntry structureEntry = new DicomStructureEntry(dicomElement);
                    objectInputStream = dicomInputStream; // As it happens to be!

                    // Directories are not processed per se
                    Iterator<Action> ait = actions.iterator();
                    while (ait.hasNext()) {
                        Action action = ait.next();

                        if (action.match(structureEntry.getName())) {
                            if (log.isDebugEnabled()) {
                                // log.debug(me() + ":process dicom-file"); TODO
                            }
                            Processor processor = action.getProcessor();
                            if (processor instanceof ContainerStructureProcessor) {
                                if (action.getMethod().equalsIgnoreCase("process")) {
                                    //-----------------------------------------------------------------------------
                                    // Since we are referring to a structure (processor), we are probably just
                                    // going to process an embedded TAR-file (or the like). We have to create a
                                    // temporary file and recursively feed it to the processor manager...
                                    //-----------------------------------------------------------------------------
                                    File subInputFile = extractEntry(structureEntry, objectInputStream);
                                    File subOutputFile = null;
                                    if (isMutableCall) {
                                        subOutputFile = File.createTempFile("temporary-processed", ".dcm");
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
                                                    action.getProcessor(), action.getMethod(), structureEntry.getName(),
                                                    subInputStream, subOutputStream, dicomContext
                                            );
                                        } finally {
                                            if (null != subOutputStream) subOutputStream.close();
                                            if (null != subInputStream) subInputStream.close();
                                        }
                                        if (isMutableCall) {
                                            // Add the temporary file to the output stream instead of the original
                                            addEntry(subOutputFile, structureEntry, /* archiveOutputStream */ null);
                                        }

                                    } finally {
                                        if (null != subOutputFile && subOutputFile.exists()) subOutputFile.delete();
                                        if (null != subInputFile && subInputFile.exists()) subInputFile.delete();
                                    }
                                    continue with_next_entry; // since we operated on a unique entry

                                } else if (action.getMethod().equalsIgnoreCase("extractInformation")) {
                                    //
                                    manager.applyOnEntry(
                                            action.getProcessor(), action.getMethod(), structureEntry, objectInputStream, /* OutputStream */ null, dicomContext
                                    );
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
                                        action.getProcessor(), action.getMethod(), structureEntry, objectInputStream, /* OutputStream */ null, dicomContext
                                );
                                continue with_next_entry; // since we operated on a unique entry
                            }
                        }
                    }
                } finally {
                    /*
                     * Don't close the objectInputStream! It is just a reference to the dicomInputStream
                     * which we want to continue operating upon.
                     */
                }
            }
        } finally {
            if (null != dicomInputStream) dicomInputStream.close();

            Map<String, String> collectedValues = dicomContext.getCollectedValues();
            List<String> toRemove = new Vector<String>();
            for (String key : collectedValues.keySet()) {
                if (null == collectedValues.get(key)) {
                    toRemove.add(key);
                }
            }
            for (String key : toRemove) {
                collectedValues.remove(key);
            }

            if (! collectedValues.isEmpty()) {
                // Create a package-relative path...
                File top = new File("/");
                File contentStream = top; // starting point relative to top

                // ...and reassemble
                int start = name.startsWith("/") ? 0 : 1; /* skip [example1]/content/... */

                String[] parts = name.split("/");
                for (int i=start; i < parts.length; i++) {
                    contentStream = new File(contentStream, parts[i]);
                }

                String path = contentStream.getPath().replace("\\", "/");  // in case we're on Windoze
                context.associate("DICOM", path, path, collectedValues);
            }

            context.pop();
        }
    }

    /**
     *
     * @param entry
     * @param entryInputStream
     * @return
     * @throws ProcessorException
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
