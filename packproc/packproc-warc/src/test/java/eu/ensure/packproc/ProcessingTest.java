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
package eu.ensure.packproc;

import eu.ensure.packproc.model.EntryHandler;
import eu.ensure.packproc.model.StructureEntry;
import eu.ensure.packproc.warc.ResourceHandler;
import eu.ensure.packproc.warc.ResourceIndexer;
import eu.ensure.packproc.warc.ResourceMigrater;
import eu.ensure.vopn.lang.LoggingUtils;
import eu.ensure.vopn.lang.Stacktrace;
import eu.ensure.vopn.xml.Namespaces;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 */
public class ProcessingTest extends TestCase {
    private static Logger log = LogManager.getLogger(ProcessingTest.class);

    @Test
    public void testWarcProcessing() {
        try {
            ProcessorManager manager;

            try (InputStream config = getClass().getResourceAsStream("warc-processing-configuration.xml");) {
                Properties properties = new Properties();
                manager = new ProcessorManager(properties, config);

                Map<String, EntryHandler> operations = new HashMap<>();
                //operations.put("migrate", new ResourceMigrater(manager));
                //operations.put("index", new ResourceIndexer(manager));
                operations.put("debug", (ResourceHandler<HttpResponse>) (entryWrapper, entry, contentType, inputStream, configuration, namespaces) -> {

                    System.out.println("----> " + entryWrapper.getName() + " " + entryWrapper.getSize());
                    System.out.println(" ---> " + contentType);

                });
                manager.setHandlers(operations);

                manager.prepare();

            } catch (ProcessorException pe) {
                Throwable cause = Stacktrace.getBaseCause(pe);
                String info = "Failed to initiate processor manager: " + cause.getMessage();
                info += "\n" + Stacktrace.asString(cause);
                log.warn(info);

                throw pe;
            }

            if (true) {
                String testWarc = "IAH-urls-wget.warc";
                System.out.println("Processing WARC-file " + testWarc);

                try (InputStream is = getClass().getResourceAsStream(testWarc)) {
                    BasicProcessorContext context = new BasicProcessorContext(testWarc);
                    manager.apply(testWarc, is, /* OutputStream */ null, context);
                }
            } else {
                File dir = new File("/Users/froran/Projects/preserva/kyrkan");
                File[] files = dir.listFiles((_dir, name) -> name.toLowerCase().endsWith(".warc"));
                BasicProcessorContext context = new BasicProcessorContext("test");
                for (File file : files) {
                    try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                        manager.apply(file.getName(), is, /* OutputStream */ null, context);
                    }
                }
            }
        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to process WARC: " + cause.getMessage();
            System.err.println(info + "\n" + Stacktrace.asString(cause));
            fail(info);

        } catch (IOException ioe) {
            String info = "Failed: " + ioe.getMessage();
            System.err.println(info);
            fail(info);
        }
    }
}
