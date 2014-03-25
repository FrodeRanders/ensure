/*
 * Copyright (C) 2011-2014 Luleå University of Technology.
 * All rights reserved.
 */
package eu.ensure.packvalid;

import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.packproc.internal.TrackingProcessorContext;
import eu.ensure.packproc.model.AssociatedInformation;
import eu.ensure.packproc.model.EvaluationStatement;
import eu.ensure.packproc.model.ProcessorContext;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.junit.Test;

import java.io.*;
import java.util.*;

// Used when loading the Log4J configuration manually
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 */
public class ProcessingTest extends TestCase {
    private static Logger log = LoggingUtils.setupLoggingFor(ProcessingTest.class, "log-configuration.xml");

    /*
     * Small utility to create a DICOM tag maps file from a (manually) manipulated dump of
     * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/DICOM.html
     *
    void dump(File outputFile) {
        / *
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">

        <properties>
            <comment>Maps DICOM tags "gggg,eeee" to description</comment>
            <entry key="0002,0002">UIMedia Storage SOP Class UID</entry>
            <entry key="0002,0003">UIMedia Storage SOP Inst UID</entry>
            <entry key="0002,0010">UITransfer Syntax UID</entry>
            <entry key="0002,0012">UIImplementation Class UID</entry>
            <entry key="0002,0013">SHImplementation Version Name</entry>
            ...
        </properties>
        * /
        org.dom4j.Document document = DocumentHelper.createDocument();
        document.addDocType("properties", null, "http://java.sun.com/dtd/properties.dtd");
        Element root = document.addElement("properties");
        root.addElement("comment").addText("Maps DICOM tags \"xxxx,yyyy\" to description");

        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("dicom-tags.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\t");
                if (parts.length == 2) {
                    root.addElement("entry")
                        .addAttribute("key", parts[0])
                        .addText(parts[1]);
                }
            }

            Writer writer = null;
            try {
                writer = new OutputStreamWriter(new FileOutputStream(outputFile, false));
                document.write(writer);
                writer.flush();
            } catch (IOException ioe) {
                try  {
                    if (null != writer) writer.close();
                } catch (Exception ignore) {}

                System.err.println("Failure! " + ioe.getMessage());
            }
        } catch (IOException ioe) {
            try  {
                if (null != is) is.close();
            } catch (Exception ignore) {}

            System.err.println("Failure! " + ioe.getMessage());
        }
    }
    */

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

            } finally {
                try {
                    if (null != config) config.close();
                } catch (Throwable ignore) {
                }
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
