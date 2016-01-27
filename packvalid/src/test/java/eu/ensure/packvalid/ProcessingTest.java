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
package eu.ensure.packvalid;

import eu.ensure.commons.io.Closer;
import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.TrackingProcessorContext;
import eu.ensure.packproc.model.AssociatedInformation;
import eu.ensure.packproc.model.EvaluationStatement;
import eu.ensure.packproc.model.ProcessorContext;
import junit.framework.TestCase;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.Collection;
import java.util.Properties;


/**
 */
public class ProcessingTest extends TestCase {
    private static Logger log = LoggingUtils.setupLoggingFor(ProcessingTest.class, "log4j2.xml");


    @Test
    public void testAIPProcessing() {
        InputStream testPackage = null;
        try {
            DicomEvaluation evaluator = new DicomEvaluation(getClass(), "test-configuration.xml");

            //
            String aipName = "philips/aipexample1.tar";
            testPackage = getClass().getResourceAsStream(aipName);
            if (null != log) {
                log.info("### Processing " + aipName);
            }

            Collection<? extends AssociatedInformation> assocInfo1;
            assocInfo1 = evaluator.evaluateAip("test", aipName, testPackage);
            testPackage.close();

            //
            aipName = "philips/aipexample1_transformed.tar";
            testPackage = getClass().getResourceAsStream(aipName);
            if (null != log) {
                log.info("### Processing " + aipName);
            }
            Collection<? extends AssociatedInformation> assocInfo2;
            assocInfo2 = evaluator.evaluateAip("test", aipName, testPackage);
            testPackage.close();
            testPackage = null;

            evaluator.evaluateDelta("test", assocInfo1, assocInfo2);

            Collection<EvaluationStatement> evaluationStatements = TrackingProcessorContext.extractEvaluationStatements();
            TrackingProcessorContext.debugEvaluationStatements(evaluationStatements);

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to process test package: " + cause.getMessage();
            System.err.println(info + "\n" + Stacktrace.asString(cause));
            fail(info);

        } catch (IOException ioe) {
            String info = "Failed: " + ioe.getMessage();
            System.err.println(info);
            fail(info);

        } finally {
            try {
                if (null != testPackage) testPackage.close();
            } catch (Throwable t) {
                String info = "Failed: ";
                Throwable base = Stacktrace.getBaseCause(t);
                info += base.getMessage();
                System.err.println(info);
                log.error(info, base);
                fail(info);
            }
        }
    }

    @Test
    public void testMutableProcessing() {
        InputStream testPackage = null;
        OutputStream mutatedPackage = null;
        try {
            ProcessorManager manager = null;
            InputStream config = null;
            try {
                config = getClass().getResourceAsStream("test-configuration.xml");
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

            //
            String aipName = "philips/aipexample1.tar";
            testPackage = getClass().getResourceAsStream(aipName);
            if (null != log) {
                log.info("### Processing " + aipName);
            }

            File tmpOutput = File.createTempFile("processed", ".package");
            System.out.println("Created mutated output file: " + tmpOutput.getAbsolutePath());

            mutatedPackage = new BufferedOutputStream(new FileOutputStream(tmpOutput));

            BasicProcessorContext context = new BasicProcessorContext(aipName);
            manager.apply(aipName, testPackage, mutatedPackage, context);

            Collection<ProcessorContext> contextStack = context.getContextStack();
            if (!contextStack.isEmpty()) {
                String info = "Mismatched push/pop on ProcessorContext stack: ";
                info += "contains " + contextStack.size() + " elements";
                log.warn(info);
            }

            Collection<? extends AssociatedInformation> assocInfo = context.extractAssociatedInformation();

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to process test package: " + cause.getMessage();
            System.err.println(info + "\n" + Stacktrace.asString(cause));
            fail(info);

        } catch (IOException ioe) {
            String info = "Failed: " + ioe.getMessage();
            System.err.println(info);
            fail(info);

        } finally {
            try {
                if (null != testPackage) testPackage.close();
                if (null != mutatedPackage) mutatedPackage.close();
            } catch (Throwable t) {
                String info = "Failed: ";
                Throwable base = Stacktrace.getBaseCause(t);
                info += base.getMessage();
                System.err.println(info);
                log.error(info, base);
                fail(info);
            }
        }
    }
}
