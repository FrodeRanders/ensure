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

import eu.ensure.commons.io.Closer;
import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.packproc.model.ProcessorContext;
import junit.framework.TestCase;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 */
public class ProcessingTest extends TestCase {
    private static Logger log = LoggingUtils.setupLoggingFor(ProcessingTest.class, "log4j2.xml");

    @Test
    public void testWarcProcessing() {
        try {
            ProcessorManager manager = null;

            try (InputStream config = getClass().getResourceAsStream("warc-processing-configuration.xml");) {
                Properties properties = new Properties();
                manager = new ProcessorManager(properties, config);
                manager.prepare();

            } catch (ProcessorException pe) {
                Throwable cause = Stacktrace.getBaseCause(pe);
                String info = "Failed to initiate processor manager: " + cause.getMessage();
                info += "\n" + Stacktrace.asString(cause);
                log.warn(info);

                throw pe;
            }

            String testWarc = "IAH-urls-wget.warc";
            System.out.println("Processing WARC-file " + testWarc);

            try (InputStream is = getClass().getResourceAsStream(testWarc)) {
                BasicProcessorContext context = new BasicProcessorContext(testWarc);
                manager.apply(testWarc, is, /* OutputStream */ null, context);
            }
        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to process test file system traversing: " + cause.getMessage();
            System.err.println(info + "\n" + Stacktrace.asString(cause));
            fail(info);

        } catch (IOException ioe) {
            String info = "Failed: " + ioe.getMessage();
            System.err.println(info);
            fail(info);
        }
    }
}
