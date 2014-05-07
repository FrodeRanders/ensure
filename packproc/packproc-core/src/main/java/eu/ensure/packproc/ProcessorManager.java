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

package eu.ensure.packproc;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.log4j.Logger;

import eu.ensure.commons.lang.DynamicInitializer;
import eu.ensure.commons.lang.DynamicLoader;
import eu.ensure.commons.lang.Stacktrace;

import eu.ensure.packproc.internal.Action;
import eu.ensure.packproc.internal.EntrySelection;
import eu.ensure.packproc.model.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.util.*;

/**
 * Loads the configuration from file and applies the processor machinery
 */
public class ProcessorManager {
    private static final Logger log = Logger.getLogger(ProcessorManager.class);

    private DynamicLoader<Processor> processorLoader = new DynamicLoader<Processor>("processor");

    private OMElement configuration = null;
    private Properties properties = null;
    private List<Action> outermostActions = new Vector<Action>();

    public ProcessorManager(Properties properties, InputStream configStream) throws ProcessorException {

        this.properties = properties;

        // Validate configuration file
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(configStream);
            /*
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            reader.setFeature("http://apache.org/xml/features/validation/schema", false);
            // other feature(s) that could be of interest...
            //reader.setFeature("http://apache.org/xml/features/validation/dynamic", false);
            */

            StAXOMBuilder builder = new StAXOMBuilder(reader);
            configuration = builder.getDocumentElement(); // <configuration />

            if (isInvalid(configuration)) {
                log.error("The configuration is invalid");
            }
        } catch (XMLStreamException se) {
            log.error("The configuration is invalid: " + se.getMessage(), se);
        }
    }

    public void prepare() throws ProcessorException {
        try {
            //-------------------------------------------------------------------------------
            // The first element (below <configuration /> is expected to correspond to a
            // structure processor, the next level indicating what to do with the individual
            // file in the structure.
            //
            // As an effect of this, providing location information through attributes on
            // this level has no effect!
            //-------------------------------------------------------------------------------
            Stack<Processor> outerProcessors = new Stack<Processor>(); // none sofar

            // Iterate through child elements of <configuration />.
            for (Iterator<OMElement> ei = configuration.getChildElements(); ei.hasNext(); ) {
                OMElement childElement = ei.next();
                Hashtable<String, String> childAttributes = new Hashtable<String, String>();

                for (Iterator<OMAttribute> ai = childElement.getAllAttributes(); ai.hasNext();) {
                    OMAttribute attribute = ai.next();
                    childAttributes.put(attribute.getLocalName(), resolve(attribute.getAttributeValue()));
                }

                // Collect processors
                Processor childProcessor = prepare(childElement, childAttributes, outerProcessors);
                if (null == childProcessor) {
                    // The child was not a processor at all, but at the <configuration/> level
                    // we are not prepared to handle non-processor configuration.
                    String info = "Encountered non-processor XML within the <configuration/> scope, ";
                    info += "i.e. XML elements not belonging to any \"classpath:\"-based namespace or ";
                    info += "with no namespace at all";
                    throw new ProcessorException(info);
                }

                // Collect child invocation into list of outermost actions
                String method = childElement.getLocalName();
                outermostActions.add(new Action(new EntrySelection(childAttributes, method), childProcessor, method));
            }

        } catch (ClassNotFoundException cnfe) {
            String info = "Failed to load processor: " + cnfe.getMessage();
            throw new ProcessorException(info, cnfe);

        } catch (RuntimeException re) {
            String info = "Failure only visible in runtime: " + re.getMessage();
            log.error(info, re);
            throw new ProcessorException(info, re);
        }
    }

    private Processor prepare(
            OMElement element,
            final Hashtable<String, String> elementAttributes,
            final Stack<Processor> outerProcessors
    ) throws ClassNotFoundException, ProcessorException {
        //----------------------------------------------------------------------------------------
        // To begin, this may not be a processor definition at all! It could easily be an XML
        // configuration belonging to a processor. The absence of a "classpath:..." namespace will
        // indicate if this is the case!
        // This is not an error, but on the other hand this element does not describe a
        // processor either. We will return 'null' and let the caller determine what to do.
        //----------------------------------------------------------------------------------------
        OMNamespace namespace = element.getNamespace();
        if (null == namespace ||
            null == namespace.getNamespaceURI() ||
            namespace.getNamespaceURI().length() == 0 ||
            !namespace.getNamespaceURI().startsWith("classpath:")) {

            return null;
        }
        String classSpecification = namespace.getNamespaceURI(); // xmlns:ns="classpath:com.xyz..."

        // Load current processor and initialize, class of processor identified via the namespace
        Processor processor;
        {
            // Determine classname and alias (from the xmlns:<alias>="classpath:...")
            String className = classSpecification.substring(/* right after "classpath:" */ 10);
            String _alias = namespace.getPrefix();
            if (null == _alias || _alias.length() == 0) {
                String[] parts = className.split("\\.");
                if (parts.length > 0) {
                    _alias = parts[parts.length-1];
                } else {
                    _alias = className;
                }
            }
            final String alias = _alias;
            final String text = resolve(element.getText());
            final ProcessorManager manager = this;

            DynamicInitializer<Processor> di = new DynamicInitializer<Processor>() {
                public void initialize(Processor processor) {
                    // By now, we know what processor we are loading, but all processors
                    // are initialized in the same way...
                    processor.initialize(
                            manager,
                            elementAttributes,
                            text,
                            alias,
                            outerProcessors
                    );
                }
            };

            // We don't yet know what kind of processor we are loading
            processor = processorLoader.load(className, di);
        }

        if (null == processor) {
            String info = "Could not locate processor ";
            info += classSpecification + ":";
            info += element.getLocalName();
            info += ". The class of the processor is identified by it's namespace (\"classpath:com.xyz...\") in the configuration file.";
            throw new ProcessorException(info);
        }

        Stack<Processor> nestedOuterProcessors = (Stack<Processor>) outerProcessors.clone();
        nestedOuterProcessors.push(processor);

        // Now we have to determine what to do for entries within this structure.
        // We will iterate through the child elements and try to locate entries to
        // operate upon.
        boolean containsPrivateData = false;

        List<Action> actions = new Vector<Action>();
        for (Iterator<OMElement> ei = element.getChildElements(); ei.hasNext(); ) {
            OMElement childElement = ei.next();
            Hashtable<String, String> childAttributes = new Hashtable<String, String>();

            for (Iterator<OMAttribute> ai = childElement.getAllAttributes(); ai.hasNext();) {
                OMAttribute attribute = ai.next();
                childAttributes.put(attribute.getLocalName(), resolve(attribute.getAttributeValue()));
            }

            // Collect nested processors
            Processor childProcessor = prepare(childElement, childAttributes, nestedOuterProcessors);
            if (null == childProcessor) {
                // The child was not a processor at all. We will treat _ALL_ child elements
                // as configuration to the current element - even elements that had the
                // correct "classpath:"-based namespace!
                // Therefore, we will escape this loop and reset the action list.
                actions.clear();
                containsPrivateData = true;
                break;
            }

            // Collect child invocation into parent processor
            String method = childElement.getLocalName();
            actions.add(new Action(new EntrySelection(childAttributes, method), childProcessor, method));
        }
        if (containsPrivateData) {
            // ...and thus no action definitions
            processor.setConfiguration(element);

        } else {
            processor.define(actions);
        }

        if (log.isDebugEnabled())
        {
            log.debug("Loaded configuration for " + namespace.getPrefix() + ":" + element.getLocalName() + " --> " + processor);
        }
        return processor;
    }

    /**
     * Lookup (value) variable in properties.
     * <p/>
     * Handles weird cases such as prefix-${property-${nested-property}}-suffix
     * <p/>
     * @param txt
     * @return
     */
    public String resolveNested(String txt) {
        String trimmedTxt = txt.trim();
        if (!trimmedTxt.contains("${")) {
            return trimmedTxt;
        }

        int startPos = trimmedTxt.indexOf("${");
        int endPos = trimmedTxt.lastIndexOf("}");

        if (startPos+2 < endPos) { // demand at least "${x}"
            // Located a pair of "${" and "}"
            String prefix = trimmedTxt.substring(0, startPos);
            String suffix = trimmedTxt.substring(endPos+1);
            String propertyName = resolveNested(trimmedTxt.substring(startPos+2, endPos));
            if (null != properties) {
                String propertyValue = (String) properties.get(propertyName);
                if (null != propertyValue) {
                    return prefix + propertyValue.trim() + suffix;
                }
            }
        }
        return trimmedTxt;
    }

    /**
     * Lookup (value) variable in properties.
     * <p/>
     * Handles cases such as prefix-${property-1}-${property-2}-suffix
     * <p/>
     * @param txt
     * @return
     */
    public String resolve(String txt) {
        if (null == txt) {
            return "";
        }

        String trimmedTxt = txt.trim();
        if (!trimmedTxt.contains("${")) {
            return trimmedTxt;
        }

        int startPos = trimmedTxt.indexOf("${");
        int endPos = trimmedTxt.indexOf("}");

        if (startPos+2 < endPos) { // demand at least "${x}"
            // Located a pair of "${" and "}"
            String prefix = trimmedTxt.substring(0, startPos);
            String suffix = trimmedTxt.substring(endPos+1);
            if (null != properties) {
                String propertyName = trimmedTxt.substring(startPos+2, endPos);
                String propertyValue = (String) properties.get(propertyName);
                if (null != propertyValue) {
                    return resolve(prefix + propertyValue.trim() + suffix);
                }
            }
        }
        return trimmedTxt;
    }

    private boolean isInvalid(OMElement element) {
        for (Iterator<OMElement> i = element.getChildElements(); i.hasNext(); ) {
            OMElement e = i.next();
            if (isInvalid(e)) {
                return true;
            }
        }
        return false;
    }

    public void apply(String name, InputStream inputStream, OutputStream outputStream, ProcessorContext context)
            throws ProcessorException, IOException {

        try {
            for (Action action : outermostActions) {
                // We can ignore entry selection on outermost processors
                Processor processor = action.getProcessor();
                String method = action.getMethod();

                if (processor instanceof ContainerStructureProcessor) {
                    applyOnContainerWithStructure(processor, method, name, inputStream, outputStream, context);

                } else if (processor instanceof FileProcessor) {
                    applyOnPlainFile(processor, method, name, inputStream, outputStream, context);
                }
            }
        } catch (ClassNotFoundException cnfe) {
            String info = "Failed to load processor: " + cnfe.getMessage();
            throw new ProcessorException(info, cnfe);

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            if (null != cause && cause instanceof ProcessorException) {
                pe = (ProcessorException) cause;
            }
            throw pe;

        } catch (IOException ioe) {
            String info = "Failed to process input: " + ioe.getMessage();
            throw new ProcessorException(info, ioe);

        } catch (RuntimeException re) {
            String info = "Failure only visible in runtime: " + re.getMessage();
            log.error(info, re);
            throw new ProcessorException(info, re);
        }
    }

    public void apply(File file, ProcessorContext context)
            throws ProcessorException, IOException {

        try {
            for (Action action : outermostActions) {
                // We can ignore entry selection on outermost processors
                Processor processor = action.getProcessor();
                String method = action.getMethod();

                if (processor instanceof DispersedStructureProcessor) {
                    if (!file.isDirectory()) {
                        String info = "Not a directory: ";
                        info += file.getAbsolutePath();
                        throw new ProcessorException(info);
                    }

                    if (!file.canRead()) {
                        String info = "Can not access (read) directory: ";
                        info += file.getAbsolutePath();
                        throw new ProcessorException(info);
                    }
                    applyOnDirectory(processor, method, file.getPath(), file, context);
                }
                else if (processor instanceof FileProcessor) {
                    String info = "Applying processors on individual files are not currently supported. ";
                    info += "Consider using the stream-based apply() instead.";
                    throw new UnsupportedOperationException(info);
                }
            }
        } catch (ClassNotFoundException cnfe) {
            String info = "Failed to load processor: " + cnfe.getMessage();
            throw new ProcessorException(info, cnfe);

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            if (null != cause && cause instanceof ProcessorException) {
                pe = (ProcessorException) cause;
            }
            throw pe;

        } catch (IOException ioe) {
            String info = "Failed to process input: " + ioe.getMessage();
            throw new ProcessorException(info, ioe);

        } catch (RuntimeException re) {
            String info = "Failure only visible in runtime: " + re.getMessage();
            log.error(info, re);
            throw new ProcessorException(info, re);
        }
    }

    public void applyOnContainerWithStructure(
            Processor processor,
            String method,
            String name,
            InputStream inputStream,
            OutputStream outputStream,
            ProcessorContext context
    )
            throws ClassNotFoundException, IOException, ProcessorException
    {
        if (log.isDebugEnabled()) {
            log.debug("Processing " + processor.getAlias() + ":" + method);
        }

        // Locate method (name matching current element name) and call. We have to explicitly
        // define the parameter types since we need to refer to (abstract) base types and
        // interfaces
        Object[] parameters = { name, inputStream, outputStream, context };
        Class[] types = { String.class, InputStream.class, OutputStream.class, ProcessorContext.class };
        try {
            processorLoader.callMethodOn(processor, method, parameters, types);

        } catch (Throwable t) {
            // Chances are you configured a second processor within <configuration />?
            Throwable cause = Stacktrace.getBaseCause(t);
            String info = "Call to " + processor.getAlias() + ":" + method + " returns error: " + cause.getMessage();
            log.warn(info);

            if (cause instanceof ProcessorException) {
                throw (ProcessorException) cause;
            } else {
                throw new ProcessorException(info, cause);
            }
        }
    }

    public void applyOnDirectory(
            Processor processor,
            String method,
            String name,
            File directory,
            ProcessorContext context
    )
            throws ClassNotFoundException, IOException, ProcessorException
    {
        if (log.isDebugEnabled()) {
            log.debug("Processing " + processor.getAlias() + ":" + method);
        }

        // Locate method (name matching current element name) and call. We have to explicitly
        // define the parameter types since we need to refer to (abstract) base types and
        // interfaces
        Object[] parameters = { name, directory, context };
        Class[] types = { String.class, File.class, ProcessorContext.class };
        try {
            processorLoader.callMethodOn(processor, method, parameters, types);

        } catch (Throwable t) {
            // Chances are you configured a second processor within <configuration />?
            Throwable cause = Stacktrace.getBaseCause(t);
            String info = "Call to " + processor.getAlias() + ":" + method + " returns error: " + cause.getMessage();
            log.warn(info);

            if (cause instanceof ProcessorException) {
                throw (ProcessorException) cause;
            } else {
                throw new ProcessorException(info, cause);
            }
        }
    }

    /**
     * Operates on a file where we ignore any possible structure. I.e. we could be
     * operating on a container file, but in this case we don't want to treat the file as
     * a container file. Normally this method is used with "plain" files such as text-files,
     * XML-files, etc.
     * <p/>
     * @param processor
     * @param method
     * @param inputStream
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ProcessorException
     */
    public void applyOnPlainFile(
            Processor processor,
            String method,
            String name,
            InputStream inputStream,
            OutputStream outputStream,
            ProcessorContext context
    )
            throws ClassNotFoundException, IOException, ProcessorException
    {
        if (log.isDebugEnabled()) {
            log.debug("Processing " + processor.getAlias() + ":" + method);
        }

        // Locate method (name matching current element name) and call. We have to explicitly
        // define the parameter types since we need to refer to (abstract) base types and
        // interfaces
        Object[] parameters = { name, inputStream, outputStream, context };
        Class[] types = { String.class, InputStream.class, OutputStream.class, ProcessorContext.class };
        try {
            processorLoader.callMethodOn(processor, method, parameters, types);

        } catch (Throwable t) {
            // Chances are you configured a second processor within <configuration />?
            Throwable cause = Stacktrace.getBaseCause(t);
            String info = "Call to " + processor.getAlias() + ":" + method + " returns error: " + cause.getMessage();
            log.warn(info);

            if (cause instanceof ProcessorException) {
                throw (ProcessorException) cause;
            } else {
                throw new ProcessorException(info, cause);
            }
        }
    }

    /**
     * Operates on an entry where we ignore any possible structure. I.e. we could be
     * operating on a container file, but in this case we don't want to treat the file as
     * a container file. Normally this method is used with "plain" files such as text-files,
     * source code-files, XML-files, but also individual entries in a DICOM file.
     * <p/>
     * Called when operating on an encompassing structure, in which we have configured to
     * process a single file entry.
     * <p/>
     * @param processor
     * @param method
     * @param structureEntry
     * @param entryInputStream
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ProcessorException
     */
    public void applyOnEntry(
            Processor processor,
            String method,
            StructureEntry structureEntry,
            InputStream entryInputStream,
            StructureOutputStream entryOutputStream,
            ProcessorContext context
    )
            throws ClassNotFoundException, IOException, ProcessorException
    {
        /*
        if (log.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder();
            buf.append("Processing ").append(processor.getAlias()).append(":").append(method);
            buf.append(" - ").append(structureEntry.getName());
            log.debug(buf.toString());
            System.out.println(buf.toString());
        }
        */

        // Locate method (name matching current element name) and call. We have to explicitly
        // define the parameter types since we need to refer to (abstract) base types
        Object[] parameters = { structureEntry, entryInputStream, entryOutputStream, context };
        Class[] types = { StructureEntry.class, InputStream.class, StructureOutputStream.class, ProcessorContext.class };
        try {
            processorLoader.callMethodOn(processor, method, parameters, types);

        } catch (Throwable t) {
            // Get base cause
            Throwable cause = Stacktrace.getBaseCause(t);
            String info = "Call to " + processor.getAlias() + ":" + method + " returns error: " + cause.getMessage();
            log.warn(info);

            if (cause instanceof ProcessorException) {
                throw (ProcessorException) cause;
            } else {
                throw new ProcessorException(info, cause);
            }
        }
    }
}
