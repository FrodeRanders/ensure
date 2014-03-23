/*
 * Copyright (C) 2012-2014 Frode Randers
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
package  eu.ensure.commons.lang;

import eu.ensure.commons.io.FileIO;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.util.*;

public class LoggingUtils {
    public static final String JNDI_ENVIRONMENT = "java:comp/env";
    public static final String DEFAULT_CONFIGURATION_FILE_NAME = "log-configuration.xml";

    public interface Configuration {
        @Configurable(property = "log-configuration-file", value = DEFAULT_CONFIGURATION_FILE_NAME)
        String file();

        @Configurable(property = "log-configuration-check-interval-in-seconds", value = /* 2 minutes */ "" + 2 * 60)
        int delay();
    }

    private enum ConfigurationOption {
        PULL_FROM_FILE_ON_DISK,
        PULL_FROM_RESOURCES,
        CREATE_FROM_TEMPLATE
    }

    /**
     * Initializes (and sets up) logging.
     * <p/>
     * @param clazz
     * @param resourceName
     * @return
     */
    public static Logger setupLoggingFor(Class clazz, String resourceName) {

        // --------- Determine name of log configuration file ---------
        Collection<ConfigurationTool.ConfigurationResolver> resolvers =
                new ArrayList<ConfigurationTool.ConfigurationResolver>();

        // <<<"Check in the JNDI tree">>>-resolver
        resolvers.add(new JndiConfigurationResolver());

        // <<<"Check among system properties">>>-resolver
        resolvers.add(new SystemPropertiesConfigurationResolver());

        // Wire up the configuration
        Properties defaults = new Properties();
        Configuration configuration = ConfigurationTool.bindProperties(Configuration.class, defaults, resolvers);

        // --------- Determine location of configuration file and strategy ---------
        String fileName = configuration.file();
        File configFile = new File(fileName);

        ConfigurationOption strategy;

        if (configFile.exists() && configFile.canRead()) {
            // Configuration file exists in file system - use it.
            strategy = ConfigurationOption.PULL_FROM_FILE_ON_DISK;
        }
        else {
            // Configuration file does not exist in file system (yet?)
            if (configFile.isAbsolute()) {
                strategy = ConfigurationOption.CREATE_FROM_TEMPLATE;
            }
            else {
                // If configuration.file() returns a single name without File.separator ('/')
                // then we will just try to pull it from clazz' resources.
                // If configuration.file() contains separators (even if relative) but does not exist
                // on disk, then we will try to write to it using the template from clazz' resources.
                if (fileName.contains(File.separator)) {
                    strategy = ConfigurationOption.CREATE_FROM_TEMPLATE;
                }
                else {
                    strategy = ConfigurationOption.PULL_FROM_RESOURCES;
                }
            }
        }

        Logger log = null;
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            /*
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            docBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/validation/schema", false);
            */
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            docBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
                    if (systemId.contains("log4j.dtd")) {
                        return new InputSource(new StringReader(""));
                    } else {
                        return null;
                    }
                }
            });
            InputStream config = null;
            try {
                switch (strategy) {
                    case CREATE_FROM_TEMPLATE:
                        config = clazz.getResourceAsStream(resourceName);
                        try {
                            configFile = FileIO.writeToFile(config, configFile);
                            DOMConfigurator.configureAndWatch(configFile.getAbsolutePath(), configuration.delay() * 1000);
                            System.out.println("Pulling log configuration from file: " + configFile.getAbsolutePath());
                            break;
                        } catch (Exception e) {
                            String info = "Failed to create log configuration file from internal template: ";
                            info += e.getMessage();
                            info += " - [falling back on default configuration]";
                            System.out.println(info);

                            // OBSERVE! We are doing a fall-through to the next clause...
                        } finally {
                            config.close();
                        }

                    case PULL_FROM_RESOURCES:
                        config = clazz.getResourceAsStream(resourceName);
                        Document doc = docBuilder.parse(config);
                        DOMConfigurator.configure(doc.getDocumentElement());
                        System.out.println(
                                "Pulling log configuration from resources (default): " + clazz.getName() + "#" + fileName
                        );
                        break;

                    case PULL_FROM_FILE_ON_DISK:
                        DOMConfigurator.configureAndWatch(configFile.getAbsolutePath(), configuration.delay() * 1000);
                        System.out.println("Pulling log configuration from file: " + configFile.getAbsolutePath());
                        break;

                }

                log = Logger.getLogger(clazz);
                System.out.println("Logging initiated...");

            } catch (IOException ioe) {
                String info = "Could not load logging configuration from resource \"" + resourceName + "\" ";
                info += "for class " + clazz.getName() + ": ";
                Throwable t = Stacktrace.getBaseCause(ioe);
                info += t.getMessage();

                System.err.println(info);
                throw new RuntimeException(info, t);
            } finally {
                if (null != config) config.close();
            }
        } catch (Exception e) {
            String info = "Could not setup xml-reader for logging configuration: ";
            Throwable t = Stacktrace.getBaseCause(e);
            info += t.getMessage();

            System.err.println(info);
            e.printStackTrace();
            throw new RuntimeException(info, t);
        }
        return log;
    }

    /**
     * Initializes (and sets up) logging _if_ not done already.
     * <p/>
     * @param clazz
     * @param resourceName
     * @return
     */
    public static Logger conditionallySetupLoggingFor(Class clazz, String resourceName) {
        boolean doInitialize = true;

        Logger rootLogger = Logger.getRootLogger();
        if (null != rootLogger) {
            Enumeration _appenders = rootLogger.getAllAppenders();
            if (null != _appenders) {
                List<String> appenders = new LinkedList<String>();
                while (_appenders.hasMoreElements()) {
                    appenders.add(((Appender)_appenders.nextElement()).getName());
                }

                boolean hasConsoleAppender = appenders.contains("CONSOLE");
                int count = appenders.size();
                doInitialize = (0 == count) || (1 == count && hasConsoleAppender);

                if (!doInitialize) {
                    StringBuilder buf = new StringBuilder();
                    buf.append("Logging already initialized for appenders: ");
                    for (int i = 0; i < count; i++) {
                        buf.append(appenders.get(i));
                        if (i < count)
                            buf.append(", ");
                    }
                    System.out.println(buf.toString());
                }
            }
        }

        if (doInitialize) {
            return setupLoggingFor(clazz, resourceName);
        }
        else {
            return Logger.getLogger(clazz);
        }
    }
}
