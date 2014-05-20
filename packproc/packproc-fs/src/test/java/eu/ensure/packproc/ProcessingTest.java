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

import eu.ensure.commons.io.Closer;
import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.*;

/**
 */
public class ProcessingTest extends TestCase {
    private static Logger log = LoggingUtils.setupLoggingFor(ProcessingTest.class, "log-configuration.xml");

    @Test
    public void testFilesystemProcessing() {
        try {
            ProcessorManager manager = null;

            InputStream config = null;
            try {
                config = getClass().getResourceAsStream("filesystem-processing-configuration.xml");
                Properties properties = new Properties();
                manager = new ProcessorManager(properties, config);
                manager.prepare();

            } catch (ProcessorException pe) {
                Throwable cause = Stacktrace.getBaseCause(pe);
                String info = "Failed to initiate processor manager: " + cause.getMessage();
                info += "\n" + Stacktrace.asString(cause);
                log.warn(info);

                throw pe;

            } finally {
                Closer.close(config);
            }

            File cwd = new File(System.getProperty("user.dir"));
            System.out.println("Processing filesystem; cwd=" + cwd.getPath());

            BasicProcessorContext context = new BasicProcessorContext(/* just a name */ cwd.getPath());
            manager.apply(cwd, context);

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
