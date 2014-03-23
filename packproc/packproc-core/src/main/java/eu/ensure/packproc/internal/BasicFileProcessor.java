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

import org.apache.log4j.Logger;
//import org.dom4j.*;
import org.apache.axiom.om.*;

import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.model.*;

import java.io.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public abstract class BasicFileProcessor implements FileProcessor {
    private static final Logger log = Logger.getLogger(BasicFileProcessor.class);

    protected String alias = "BasicFileProcessor"; // Please override
    
    protected ProcessorManager manager = null;

    // Normally the configuration will be conveyed through attributes and element text,
    // but it may also consist of sub-elements (not defining processors).
    protected Map<String, String> configAttributes = new Hashtable<String, String>();
    protected OMElement configElement = null;
    protected String configText = null;

    //
    protected Stack<Processor> outerProcessors = null;

    // Sub-processors (actions) are really not expected on file processors
    protected List<Action> actions = new Vector<Action>();

    public BasicFileProcessor() {
    }

    // Used when created directly (internally)
    public BasicFileProcessor(
        ProcessorManager manager,
        String alias
    ) {
        this.manager = manager;
        this.configAttributes = new HashMap<String, String>();
        this.configText = "";
        this.alias = alias;
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

    protected String getPrefix() {
        return alias + "-";
    }

    public String getAlias() {
        return alias;
    }

    protected String me() {
        return getAlias(); // + "[" + hashCode() + "]";
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

    public void process(
           InputStream inputStream,
           StructureOutputStream outputStream,
           ProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {
        /*-------------------------------------------------------------------------------------
         * Block explicit calls to this method on this object. This method must be overridden
         * by subclasses. If somebody explicitly calls this method we will fail miserably :)
         *------------------------------------------------------------------------------------*/

        String info = "You may not call this method on this object. This method has to be implemented by subclass!";
        Exception syntheticException = new Exception(info); // just to get a stack trace
        throw new UnsupportedOperationException(info, syntheticException);
    }

    public void process(
            InputStream inputStream,
            OutputStream outputStream,
            FileProcessorCallable callable,
            FileProcessor processor,
            ProcessorContext context
    ) throws IOException, ProcessorException {

        if (null == configElement) {
            String info = "The (file) processor does not have any associated configuration! ";
            info += "This can occur if a file processor entry in the configuration file does not have any sub-elements";
            log.warn(me() + ": " + info);
            throw new ProcessorException(info);
        }

        // We really should have no (sub) actions since we are a file processor and not a
        // structure processor.
        for (Action action : actions) {
            String info = "Ignoring unexpected (sub) action on a file processor! " + action;
            info += ". This can occur if a file processor entry in the configuration file has sub-elements ";
            info += "(which is applicable to structure processors only)";
            log.warn(me() + ": " + info);
        }

        ReadableByteChannel inputChannel = null;
        WritableByteChannel outputChannel = null;
        try {
            inputChannel = Channels.newChannel(inputStream);
            if (null != outputStream) {
                // Only create an output channel if we have an output stream!
                outputChannel = Channels.newChannel(outputStream);
            }
            try {
                callable.call(inputChannel, outputChannel, processor, context);

            } catch (Exception e) {
                String info = "Failed to operate on file: " + e.getMessage();
                throw new ProcessorException(info, e);
            }
        } finally {
            if (null != inputChannel) inputChannel.close();
        }
    }

    public void process(
           StructureEntry entry,
           InputStream entryInputStream,
           StructureOutputStream structureOutputStream,
           FileProcessorCallable callable,
           FileProcessor processor,
           ProcessorContext context
    ) throws IOException, ProcessorException {

        // Some sanity checks first...
        if (null == entry) {
            throw new ProcessorException("no (file) entry");
        }

        if (entry.isDirectory()) {
            String info = "You may not operate on a structure with a file processor. ";
            info += "File processors may only operate on individual files. ";
            info += "You tried to operate on the _directory_ " + entry.getName();
            throw new ProcessorException(info);
        }

        if (null == configElement) {
            String info = "The (file) processor does not have any associated configuration! ";
            info += "This can occur if a file processor entry in the configuration file does not have any sub-elements";
            log.warn(me() + ": " + info);
            //throw new ProcessorException(info);
        }

        // We really should have no (sub) actions since we are a file processor and not a
        // structure processor.
        for (Action action : actions) {
            String info = "Ignoring unexpected (sub) action on a file processor! " + action;
            info += ". This can occur if a file processor entry in the configuration file has sub-elements ";
            info += "(which is applicable to structure processors only)";
            log.warn(me() + ": " + info);
        }

        boolean isMutableCall = null != structureOutputStream;

        File tmpOutputFile = null;
        RandomAccessFile outputRaf = null;
        try {
            // Create temporary files (if needed)
            FileChannel outputFileChannel = null;
            if (isMutableCall) {
                // Only need to create temporary output file if we are going to
                // do a mutable call for this entry
                tmpOutputFile = File.createTempFile(getPrefix() + "-output", ".raw");
                outputRaf = new RandomAccessFile(tmpOutputFile, "rw");
                outputFileChannel = outputRaf.getChannel();
            }

            // Transfer from input stream to temporary file
            ReadableByteChannel entryInputChannel = Channels.newChannel(entryInputStream);

            try {
                callable.call(entryInputChannel, outputFileChannel, processor, context);
                if (isMutableCall) {
                    // We only have to take care of output if we did a mutable call
                    // for this entry
                    structureOutputStream.replaceEntry(entry, tmpOutputFile);
                    {
                        WritableByteChannel outputChannel = Channels.newChannel(structureOutputStream);
                        if (tmpOutputFile.length() > 0) {
                            outputFileChannel.transferTo(0L, tmpOutputFile.length(), outputChannel);
                        }
                    }
                    structureOutputStream.closeEntry();
                }
            } catch (Exception e) {
                String info = "Failed to operate on file: " + e.getMessage();
                throw new ProcessorException(info, e);
            }
        } finally {
            // Close temporary resources and such
            if (null != outputRaf) outputRaf.close(); // closes the associated FileChannel as well

            // Remove temporary files
            if (null != tmpOutputFile) tmpOutputFile.delete();
        }
    }

    /**
     *
     * @return
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
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







