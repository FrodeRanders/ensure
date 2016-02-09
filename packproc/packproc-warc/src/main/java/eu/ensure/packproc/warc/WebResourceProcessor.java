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

import eu.ensure.vopn.xml.Namespaces;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.internal.BasicFileProcessor;
import eu.ensure.packproc.model.*;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.httpclient.ChunkedInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Iterator;


/**
 *
 */
public class WebResourceProcessor extends BasicFileProcessor {
    private static final Logger log = LogManager.getLogger(WebResourceProcessor.class);
    private static final Charset ascii = Charset.forName("ASCII");

    public WebResourceProcessor() {
        alias = "Resource-processor"; // a reasonable default
    }

    /**
     * Processes a file _entry_ in a structure.
     * <p>
     *
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
        FileProcessorUsingStreamsCallable callable = new FileProcessorUsingStreamsCallable() {
            public void call(
                    InputStream inputStream, OutputStream outputStream,
                    FileProcessor p, ProcessorContext context
            ) throws Exception {

                if (null == configElement) {
                    String info = "Cannot process " + alias + " configuration - no configuration";
                    log.warn(info);
                    throw new ProcessorException(info);
                }

                // Since the XPath expressions need to refer to namespaces in
                // the target file, we will define the relevant namespaces in
                // the processor configuration and refer to those later on.
                Namespaces namespaces = new Namespaces(configElement.getAllDeclaredNamespaces());

                DeferredBodyReadPreparation bodyReadPreparation = null;
                try {
                    // Will we be accessing the same input file multiple times?
                    // This could be the case if we both want to <index/> and
                    // <migrate/> a resource.
                    // Since the inputChannel does not support stream-style mark/resest
                    // (and the underlying WARC stream anyhow does not support this)
                    // we need to create a temporary input file to operate upon.
                    bodyReadPreparation = new DeferredBodyReadPreparation() {
                        private File tmpInputFile = null;
                        private RandomAccessFile inputRaf = null;

                        @Override
                        public FileChannel prepare(WarcInputBuffer inputBuffer) throws Exception {
                            // Create temporary file
                            tmpInputFile = File.createTempFile(getPrefix() + "input-", ".raw");
                            inputRaf = new RandomAccessFile(tmpInputFile, "rw");
                            FileChannel cacheChannel = inputRaf.getChannel();

                            //---------------------------------------------------------------
                            // The InputBuffer wrapping 'inputChannel' may have buffered
                            // the body while we read the header, so the 'inputChannel' may
                            // be exhausted right now. Therefore we will have to create the
                            // temporary file by reading from the InputBuffer.
                            //---------------------------------------------------------------
                            ByteBuffer bytes = ByteBuffer.allocateDirect(0x400);
                            byte[] buf = new byte[0x400];
                            int bytesRead = -1;

                            while ((bytesRead = inputBuffer.read(buf, 0, 0x400)) > 0) {
                                bytes.put(buf, 0, bytesRead);
                                bytes.flip();

                                cacheChannel.write(bytes);
                                bytes.clear();
                            }

                            cacheChannel.position(0L);
                            return cacheChannel;
                        }

                        @Override
                        public void cleanup() {
                            try {
                                // Close temporary resources and such
                                if (null != inputRaf) inputRaf.close(); // closes the associated FileChannel as well

                                // Remove temporary files
                                if (null != tmpInputFile) tmpInputFile.delete();
                            } catch (Throwable ignore) {
                            }
                        }
                    };

                    // Only parse response header once
                    WarcInputBuffer headerInputBuffer = new WarcInputBuffer();
                    headerInputBuffer.bind(inputStream);

                    DefaultHttpResponseParser parser = new DefaultHttpResponseParser(headerInputBuffer);
                    HttpResponse response = parser.parse();

                    int status = response.getStatusLine().getStatusCode();
                    if (status == HttpStatus.SC_OK) {
                        // We have a response to operate on

                        // Content-Length
                        long bodySize = 0L;
                        Header[] contentLengthHeader = response.getHeaders("Content-Length");
                        if (contentLengthHeader.length > 0) {
                            log.debug(contentLengthHeader[0].getName() + " is " + contentLengthHeader[0].getValue());
                            bodySize = Long.parseLong(contentLengthHeader[0].getValue());
                        }

                        // Content-Type
                        String contentType = "binary/octet-stream";
                        Header[] contentTypeHeader = response.getHeaders("Content-Type");
                        if (contentTypeHeader.length > 0) {
                            log.debug(contentTypeHeader[0].getName() + " is " + contentTypeHeader[0].getValue());
                            contentType = contentTypeHeader[0].getValue();
                        }

                        // Transfer-Encoding: chunked
                        String transferEncoding = null;
                        Header[] transferEncodingHeader = response.getHeaders("Transfer-Encoding");
                        if (transferEncodingHeader.length > 0) {
                            log.debug(transferEncodingHeader[0].getName() + " is " + transferEncodingHeader[0].getValue());
                            transferEncoding = transferEncodingHeader[0].getValue();
                        }

                        // Initiate input buffer for body, based on a temporary file
                        // containing just the body.
                        FileChannel bodyChannel = bodyReadPreparation.prepare(headerInputBuffer);

                        // If we could not determine content length, it could be because response is
                        // chunked.
                        boolean isChunked = 0L == bodySize && null != transferEncoding && "chunked".equalsIgnoreCase(transferEncoding);

                        if (bodySize > 0L || isChunked) {
                            for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                                OMElement configuration = ei.next();
                                String operation = configuration.getLocalName(); // Ignore namespace!!!

                                // Adjust (if necessary), skip chunk size
                                bodyChannel.position(0);

                                InputStream bodyInputStream;
                                if (isChunked) {
                                    bodyInputStream = new ChunkedInputStream(Channels.newInputStream(bodyChannel));
                                } else {
                                    bodyInputStream = Channels.newInputStream(bodyChannel);
                                }

                                // Operations on body
                                if ("index".equalsIgnoreCase(operation)) {
                                    index(entry, response, contentType, bodyInputStream, configuration, namespaces);

                                } else if ("migrate".equalsIgnoreCase(operation)) {
                                    // TODO: Migrate has to operate on WritableByteChannel outputChannel
                                    migrate(entry, response, contentType, bodyInputStream, configuration, namespaces);

                                } else {
                                    throw new ProcessorException("Unknown processor operation: " + operation);
                                }
                            }
                        } else {
                            log.debug("Ignoring: empty body");
                        }
                    } else {
                        String info = "Ignoring: status = " + status;
                        log.debug(info);
                    }

                } catch (Throwable t) {
                    String info = "Cannot process configuration for " + alias + ": ";
                    info += t.getMessage();
                    log.warn(info);

                    throw new ProcessorException(info, t);

                } finally {
                    if (null != bodyReadPreparation) bodyReadPreparation.cleanup();
                }

                // Don't close the input channel!
            }
        };

        process(entry, entryInputStream, structureOutputStream, callable, this, context);
    }

    private void index(
            StructureEntry entry, HttpResponse httpResponse, String contentType, InputStream inputStream,
            OMElement configuration, Namespaces namespaces
    ) throws ProcessorException, IOException {

        // We need a 'type'-attribute (the XPath expression)
        OMAttribute expr = configuration.getAttribute(new QName("type"));
        if (null == expr) {
            throw new ProcessorException("Could not locate the 'type'-attribute to the <index /> operation");
        }

        // TODO? Use content type info to select decoder: text/xml;charset=UTF-8
        String type = manager.resolve(expr.getAttributeValue());

        if (contentType.startsWith(type)) {
            byte[] buf = new byte[0x2000];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                ByteBuffer bytes = ByteBuffer.wrap(buf, 0, bytesRead);

            /*
             * TODO: Here is where we handle different types of input.
             *       Right now, this is only a stub.
             */
                if (contentType.startsWith("text")) {
                    // Since we are reading blocks, we may split chars
                    // which will make the decoder crash. We will ignore
                    // this since we are only doing this for test purposes
                    try {
                        String data = ascii.decode(bytes).toString();
                        log.debug("index: \n" + data);

                    } catch (Throwable ignore) {
                        log.debug("index <text> chunk");
                    }
                } else {
                    log.debug("index <binary data> chunk");
                }

                bytes.clear();
            }
        } else {
            log.debug("Not indexing content of type: " + contentType);
        }
    }

    private void migrate(
            StructureEntry entry, HttpResponse httpResponse, String contentType, InputStream inputStream,
            OMElement configuration, Namespaces namespaces
    ) throws ProcessorException, IOException {

        // We need a 'target-format'-attribute (the XPath expression)
        OMAttribute expr = configuration.getAttribute(new QName("target-format"));
        if (null == expr) {
            throw new ProcessorException("Could not locate the 'target-format'-attribute to the <migrate /> operation");
        }

        String targetFormat = manager.resolve(expr.getAttributeValue());

        // We need a 'type'-attribute (the XPath expression)
        expr = configuration.getAttribute(new QName("type"));
        if (null == expr) {
            throw new ProcessorException("Could not locate the 'type'-attribute to the <migrate /> operation");
        }

        // TODO? Use content type info to select decoder: text/xml;charset=UTF-8
        String type = manager.resolve(expr.getAttributeValue());

        if (contentType.startsWith(type)) {
            byte[] buf = new byte[0x2000];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                ByteBuffer bytes = ByteBuffer.wrap(buf, 0, bytesRead);

                /*
                 * TODO: Here is where we handle different types of input.
                 *       Right now, this is only a stub.
                 */
                if (contentType.startsWith("text")) {
                    log.debug("migrate (" + targetFormat + ") <text> chunk");
                } else {
                    log.debug("migrate (" + targetFormat + ") <binary data> chunk");
                }
                break;

                // bytes.clear();
            }
        } else {
            log.debug("Not migrating content of type: " + contentType);
        }
    }

    public interface DeferredBodyReadPreparation {
        FileChannel prepare(WarcInputBuffer inputBuffer) throws Exception;
        default void cleanup() {}
    }
}
