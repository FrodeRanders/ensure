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

import eu.ensure.commons.xml.Namespaces;

import eu.ensure.packproc.internal.*;
import eu.ensure.packproc.model.*;

import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.util.StAXParserConfiguration;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.log4j.Logger;
import org.apache.axiom.om.*;

import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Iterator;


/**
 * Processes XML files, using XPath-expressions to locate elements
 */
public class XmlFileProcessor extends BasicFileProcessor {
    private static final Logger log = Logger.getLogger(XmlFileProcessor.class);

    public XmlFileProcessor() {
        alias = "XML-processor"; // a reasonable default
    }

    public interface XmlFileCallable {
        // Non-mutable processing of input XML file
        void call(OMElement root, Namespaces namespaces, ProcessorContext context) throws Exception;
    }

    public XmlFileCallable getWhatToDo() {
        return new XmlFileCallable() {
            public void call(OMElement document, Namespaces namespaces, ProcessorContext context) throws Exception {
                if (null == configElement) {
                    String info = "Cannot process XML-file - no configuration";
                    log.warn(info);
                    throw new ProcessorException(info);
                }

                // Retrieve XPath expressions from the configuration
                for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                    OMElement element = ei.next();
                    String operation = element.getLocalName(); // Ignore namespace!!!

                    if ("contains".equalsIgnoreCase(operation)) {
                        contains(element, document, namespaces);

                    } else {
                        throw new ProcessorException("Unknown processor operation: " + operation);
                    }
                }
            }
        };
    }

    private FileProcessorCallable getCallable() {
        return new FileProcessorCallable() {
            public void call(ReadableByteChannel inputChannel, WritableByteChannel outputChannel, FileProcessor p, ProcessorContext context) throws Exception {

                // Since the XPath expressions need to refer to namespaces in
                // the target file, we will define the relevant namespaces in
                // the processor configuration and refer to those later on.
                Namespaces namespaces = new Namespaces(configElement.getAllDeclaredNamespaces());

                // XML document to operate on.
                // This reader will ignore DTS's and such...
                XMLStreamReader reader = StAXUtils.createXMLStreamReader(
                        StAXParserConfiguration.STANDALONE, Channels.newReader(inputChannel, "utf-8")
                );

                if (null != reader) {
                    StAXOMBuilder builder = new StAXOMBuilder(reader);
                    OMElement document = builder.getDocumentElement();

                    //
                    XmlFileCallable xmlCallable = getWhatToDo();
                    xmlCallable.call(document, namespaces, context);

                    // In case we have been mutating the document, dump it to the output
                    if (null != outputChannel) {
                        Writer writer = Channels.newWriter(outputChannel, "utf-8");
                        document.serialize(writer);
                    }
                }

                // Don't close the input channel!
            }
        };
    }

    /**
     * Processes an individual XML file.
     * <p/>
     * @param inputStream
     * @param outputStream
     * @throws IOException
     * @throws ProcessorException
     * @throws ClassNotFoundException
     */
    public void process(
           InputStream inputStream,
           OutputStream outputStream,
           ProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {

        log.info(me() + ":process xml-file");
        FileProcessorCallable callable = getCallable();
        process(inputStream, outputStream, callable, this, context);
    }

    /**
     * Processes a file _entry_ in a structure.
     * <p/>
     * @param entry
     * @param entryInputStream
     * @param structureOutputStream
     * @throws IOException
     * @throws ProcessorException
     */
    public void process(
           final StructureEntry entry,
           final InputStream entryInputStream,
           final StructureOutputStream structureOutputStream,
           ProcessorContext context
    ) throws IOException, ProcessorException {

        //log.info(me() + ":process xml-file");
        FileProcessorCallable callable = getCallable();
        process(entry, entryInputStream, structureOutputStream, callable, this, context);
    }


    private void contains(OMElement element, OMElement document, Namespaces namespaces) throws ProcessorException {

        // We need a 'node'-attribute (the XPath expression)
        OMAttribute expr = element.getAttribute(new QName("node"));
        if (null == expr) {
            throw new ProcessorException("Could not locate the 'node'-attribute to the <contains /> operation");
        }

        // String expression = "//ns:param-value[(../ns:param-name = 'mets-this-or-that')]";
        String expression = manager.resolve(expr.getAttributeValue());

        try {
            AXIOMXPath xpath = new AXIOMXPath(expression);
            namespaces.applyNamespacesOn(xpath);

            List<OMNode> nodes = xpath.selectNodes(document);
            if (nodes.size() > 0) {
                String info = "This XPath expression \"";
                info += expression;
                info += "\" identifies " + nodes.size() + " node(s).";
                log.info(info);

            } else if (nodes.size() == 0) {
                handleNoNodes(expression);
            }
        } catch (JaxenException je) {
            throw new ProcessorException("Could not query using expression \"" + expression + "\": " + je.getMessage(), je);
        }
    }

	public void handleNoNodes(String expression) {
		String info = "This XPath expression \"";
		info += expression;
		info += "\" does not identify any nodes.";
		log.warn(info);
	}
}
