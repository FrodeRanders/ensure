/*
 * Copyright (C) 2014 Frode Randers
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
package eu.ensure.aging;

import eu.ensure.commons.io.FileIO;
import eu.ensure.commons.lang.LoggingUtils;
import eu.ensure.commons.lang.Stacktrace;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

/**
 * Description of AgingSimulator:
 * <p/>
 * Utility program for invoking aging actions on an AIP.
 * <p/>
 * Created by Frode Randers at 2013-02-18 14:49
 */
public class AgingSimulator {
    static final String appName = "java -jar age-simulator-1.1.jar";

    /**
     * Write "help" to the provided OutputStream.
     */
    public static void printHelp(
            final Options options,
            final OutputStream out) {

        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(out);

        final int printedRowWidth = 80;
        final int spacesBeforeOption = 1;
        final int spacesBeforeOptionDescription = 3;

        String syntax = appName + " [options] <source> [<destination>]";

        String header = "\nAvailable options:";

        String footer = "\n" +
                "The options <arg> is either 'true' or 'false' and by default the -n option is\n" +
                "'true'.\n\n" +
                "<source> and optionally <destination> are paths to Archival Information Packages\n" +
                "(AIPs) and this utility is used to flip bits in AIPs, which are assumed to use\n" +
                "the TAR format, in order to simulate aging of stored AIPs.\n";

        formatter.printHelp(pw, printedRowWidth, syntax, header, options, spacesBeforeOption, spacesBeforeOptionDescription, footer, false);

        pw.flush();
    }

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption("n", "flip-bit-in-name", true, "Flip bit in first byte of name of first file");
        options.addOption("u", "flip-bit-in-uname", true, "Flip bit in first byte of username of first file");
        options.addOption("c", "update-checksum", true, "Update header checksum (after prior bit flips)");

        Properties properties = new Properties();
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine line = parser.parse(options, args);

            //
            if (line.hasOption("flip-bit-in-name")) {
                properties.put("flip-bit-in-name", line.getOptionValue("flip-bit-in-name"));
            } else {
                // On by default (if not explicitly de-activated above)
                properties.put("flip-bit-in-name", "true");
            }

            //
            if (line.hasOption("flip-bit-in-uname")) {
                properties.put("flip-bit-in-uname", line.getOptionValue("flip-bit-in-uname"));
            } else {
                properties.put("flip-bit-in-uname", "false");
            }

            //
            if (line.hasOption("update-checksum")) {
                properties.put("update-checksum", line.getOptionValue("update-checksum"));
            } else {
                properties.put("update-checksum", "false");
            }

            String[] fileArgs = line.getArgs();

            if (fileArgs.length < 1) {
                printHelp(options, System.err);
                System.exit(1);
            }

            String srcPath = fileArgs[0];

            File srcAIP = new File(srcPath);
            if (!srcAIP.exists()) {
                String info = "The source path does not locate a file: " + srcPath;
                System.err.println(info);
                System.exit(1);
            }

            if (srcAIP.isDirectory()) {
                String info = "The source path locates a directory, not a file: " + srcPath;
                System.err.println(info);
                System.exit(1);
            }

            if (!srcAIP.canRead()) {
                String info = "Cannot read source file: " + srcPath;
                System.err.println(info);
                System.exit(1);
            }

            boolean doReplace = false;
            File destAIP = null;

            if (fileArgs.length > 1) {
                String destPath = fileArgs[1];
                destAIP = new File(destPath);

                if (destAIP.exists()) {
                    String info = "The destination path locates an existing file: " + destPath;
                    System.err.println(info);
                    System.exit(1);
                }
            } else {
                doReplace = true;
                try {
                    destAIP = File.createTempFile("tmp-aged-aip-", ".tar");
                } catch (IOException ioe) {
                    String info = "Failed to create temporary file: ";
                    info += ioe.getMessage();
                    System.err.println(info);
                    System.exit(1);
                }
            }

            String info = "Age simulation\n";
            info += "  Reading from " + srcAIP.getName() + "\n";
            if (doReplace) {
                info += "  and replacing it's content";
            } else {
                info += "  Writing to " + destAIP.getName();
            }
            System.out.println(info);

            //
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(new FileInputStream(srcAIP));
                os = new BufferedOutputStream(new FileOutputStream(destAIP));

                simulateAging(srcAIP.getName(), is, os, properties);

            } catch (FileNotFoundException fnfe) {
                info = "Could not locate file: " + fnfe.getMessage();
                System.err.println(info);
            } finally {
                try {
                    if (null != os) os.close();
                    if (null != is) is.close();
                } catch (IOException ignore) {
                }
            }

            //
            if (doReplace) {
                File renamedOriginal = new File(srcAIP.getName() + "-backup");
                srcAIP.renameTo(renamedOriginal);

                ReadableByteChannel rbc = null;
                WritableByteChannel wbc = null;
                try {
                    rbc = Channels.newChannel(new FileInputStream(destAIP));
                    wbc = Channels.newChannel(new FileOutputStream(srcAIP));
                    FileIO.fastChannelCopy(rbc, wbc);
                } catch (FileNotFoundException fnfe) {
                    info = "Could not locate temporary output file: " + fnfe.getMessage();
                    System.err.println(info);
                } catch (IOException ioe) {
                    info = "Could not copy temporary output file over original AIP: " + ioe.getMessage();
                    System.err.println(info);
                } finally {
                    try {
                        if (null != wbc) wbc.close();
                        if (null != rbc) rbc.close();

                        destAIP.delete();
                    } catch (IOException ignore) {
                    }
                }
            }
        } catch (ParseException pe) {
            String info = "Failed to parse command line: " + pe.getMessage();
            System.err.println(info);
            printHelp(options, System.err);
            System.exit(1);
        }
    }

    private static void simulateAging(String name, InputStream is, OutputStream os, Properties properties) {

        Logger log = LoggingUtils.setupLoggingFor(AgingSimulator.class, "log4j2.xml");

        try {
            ProcessorManager manager = null;

            InputStream config = null;
            try {
                config = AgingSimulator.class.getResourceAsStream("aging-configuration.xml");
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

            BasicProcessorContext context = new BasicProcessorContext(name);
            manager.apply(name, is, os, context);

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to simulate aging on AIP: " + cause.getMessage();
            System.err.println(info + "\n" + Stacktrace.asString(cause));

        } catch (IOException ioe) {
            String info = "Failed: " + ioe.getMessage();
            System.err.println(info);
        }
    }
}
