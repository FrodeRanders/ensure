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

import eu.ensure.vopn.xml.Namespaces;
import eu.ensure.packproc.internal.BasicFileProcessor;
import eu.ensure.packproc.model.*;
import org.apache.axiom.om.*;
import org.apache.axiom.om.util.StAXParserConfiguration;
import org.apache.axiom.om.util.StAXUtils;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.JaxenException;
import org.jaxen.UnresolvableException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;


/**
 * Processes XML files, using XPath-expressions to locate elements
 */
public class XmlFileProcessor extends BasicFileProcessor {
    private static final Logger log = LogManager.getLogger(XmlFileProcessor.class);

    public XmlFileProcessor() {
        alias = "XML-processor"; // a reasonable default
    }

    /**
     * A specific callable for all file processors derived from XmlFileProcessor,
     * defining what to do with the XML document being operated upon.
     */
    public interface XmlFileCallable {
        // Non-mutable processing of input XML file
        void call(OMElement root, Namespaces namespaces, ProcessorContext context) throws Exception;
    }

    /**
     * A callable for a processor that want to operate upon a file, which has
     * to be implemented by classes derived form BasicFileProcessor.
     */
    protected XmlFileCallable getSpecificCallable() {
        return (target, namespaces, context) -> {
            // Retrieve XPath expressions from the configuration
            try {
                for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                    OMElement configuration = ei.next();
                    String operation = configuration.getLocalName(); // Ignore namespace!!!

                    if ("contains".equalsIgnoreCase(operation)) {
                        contains(configuration, target, namespaces);

                    } else {
                        throw new ProcessorException("Unknown processor operation: " + operation);
                    }
                }
            } catch (Throwable t) {
                String info = "Cannot process configuration for " + alias + ": ";
                info += t.getMessage();
                log.warn(info);

                throw new ProcessorException(info, t);
            }
        };
    }

    private FileProcessorUsingChannelsCallable getCallable() {
        return (inputChannel, outputChannel, p, context) -> {

            if (null == configElement) {
                String info = "Cannot process " + alias + " configuration - no configuration";
                log.warn(info);
                throw new ProcessorException(info);
            }

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
                OMXMLParserWrapper builder = OMXMLBuilderFactory.createStAXOMBuilder(reader);
                OMElement document = builder.getDocumentElement();

                //
                XmlFileCallable xmlCallable = getSpecificCallable();
                xmlCallable.call(document, namespaces, context);

                // In case we have been mutating the document, dump it to the output
                if (null != outputChannel) {
                    Writer writer = Channels.newWriter(outputChannel, "utf-8");
                    document.serialize(writer);
                }
            }

            // Don't close the input channel!
        };
    }

    /**
     * Processes an individual XML file.
     * <p>
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
        FileProcessorUsingChannelsCallable callable = getCallable();
        process(inputStream, outputStream, callable, this, context);
    }

    /**
     * Processes a file _entry_ in a structure.
     * <p>
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
        FileProcessorUsingChannelsCallable callable = getCallable();
        process(entry, entryInputStream, structureOutputStream, callable, this, context);
    }


    private void contains(OMElement configuration, OMElement document, Namespaces namespaces) throws ProcessorException {

        // We need a 'node'-attribute (the XPath expression)
        OMAttribute expr = configuration.getAttribute(new QName("node"));
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
        } catch (UnresolvableException ure) {
            String info = "Could not resolve namespace(s) in expression \"" + expression + "\": ";
            info += "Known namespaces are: [";
            for (OMNamespace namespace : namespaces.getNamespaces().values())
            {
                String prefix = namespace.getPrefix();
                String uri = namespace.getNamespaceURI();
                info += "{\"" + prefix + "\"->\"" + uri + "\"}";
            }
            info += "]: ";
            info +=  ure.getMessage();
            throw new ProcessorException(info, ure);

        } catch (JaxenException je) {
            String info = "Could not query using expression \"" + expression + "\": " + je.getMessage();
            throw new ProcessorException(info, je);
        }
    }

	public void handleNoNodes(String expression) {
		String info = "This XPath expression \"";
		info += expression;
		info += "\" does not identify any nodes.";
		log.warn(info);
	}
}
