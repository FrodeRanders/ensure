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

import eu.ensure.commons.xml.Namespaces;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.internal.BasicFileProcessor;
import eu.ensure.packproc.model.*;
import it.unimi.dsi.fastutil.chars.CharSet;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.http.*;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;


/**
 *
 */
public class WebResourceProcessor extends BasicFileProcessor {
    private static final Logger log = LogManager.getLogger(WebResourceProcessor.class);

    public WebResourceProcessor() {
        alias = "Resource-processor"; // a reasonable default
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

        //log.info(me() + ":process web-resource");
        FileProcessorCallable callable = new FileProcessorCallable() {
            public void call(ReadableByteChannel inputChannel, WritableByteChannel outputChannel, FileProcessor p, ProcessorContext context) throws Exception {

                if (null == configElement) {
                    String info = "Cannot process " + alias + " configuration - no configuration";
                    log.warn(info);
                    throw new ProcessorException(info);
                }

                // Since the XPath expressions need to refer to namespaces in
                // the target file, we will define the relevant namespaces in
                // the processor configuration and refer to those later on.
                Namespaces namespaces = new Namespaces(configElement.getAllDeclaredNamespaces());

                // Retrieve XPath expressions from the configuration
                try {
                    for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                        OMElement configuration = ei.next();
                        String operation = configuration.getLocalName(); // Ignore namespace!!!

                        WarcRecordEntry warcRecord = (WarcRecordEntry)entry;

                        if ("index".equalsIgnoreCase(operation)) {
                            //log.debug("INDEX " + warcRecord.getContentType());
                            parse(inputChannel);

                        } else if ("migrate".equalsIgnoreCase(operation)) {
                            //log.debug("MIGRATE " + warcRecord.getContentType());
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

                // Don't close the input channel!
            }
        };

        process(entry, entryInputStream, structureOutputStream, callable, this, context);
    }

    private void parse(final ReadableByteChannel inputChannel) throws IOException, HttpException {

        HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
        InputBuffer inputBuffer = new InputBuffer(metrics, /* buffersize */ 1024);
        inputBuffer.bind(inputChannel);

        DefaultHttpResponseParser parser = new DefaultHttpResponseParser(inputBuffer);
        HttpResponse response = parser.parse();

        int status = response.getStatusLine().getStatusCode();
        log.debug("Status: " + status);
        if (status >= HttpStatus.SC_OK
            && status != HttpStatus.SC_NO_CONTENT
            && status != HttpStatus.SC_NOT_MODIFIED
            && status != HttpStatus.SC_RESET_CONTENT) {

            Header[] contentType = response.getHeaders("Content-Type");
            if (contentType.length > 0) {
                log.debug(contentType[0].getName() + " is " + contentType[0].getValue());
            }

            /*
            ByteBuffer bytes = ByteBuffer.allocateDirect(1024);
            CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
            int bytesRead = inputChannel.read(bytes);
            while ((bytesRead = inputChannel.read(bytes)) > 0) {
                bytes.flip();
                log.debug(decoder.decode(bytes));
                bytes.clear();
            }
            */
        }
    }

    private void index(OMElement configuration, OMElement target, Namespaces namespaces) throws ProcessorException {

        // We need a 'node'-attribute (the XPath expression)
        OMAttribute expr = configuration.getAttribute(new QName("node"));
        if (null == expr) {
            throw new ProcessorException("Could not locate the 'node'-attribute to the <contains /> operation");
        }

        // String expression = "//ns:param-value[(../ns:param-name = 'mets-this-or-that')]";
        String expression = manager.resolve(expr.getAttributeValue());

        log.info("Index");
    }

    private void migrate(OMElement configuration, OMElement target, Namespaces namespaces) throws ProcessorException {
        log.info("Migrate (XML)");
    }

    private void migrate(StructureEntry entry, InputStream is, StructureOutputStream os, ProcessorContext context) {
        log.info("Migrate");
    }

	public void handleNoNodes(String expression) {
		String info = "This XPath expression \"";
		info += expression;
		info += "\" does not identify any nodes.";
		log.warn(info);
	}
}
