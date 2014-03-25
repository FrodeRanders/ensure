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
package  eu.ensure.commons.lang;

import eu.ensure.commons.lang.configuration.GeneralizedConfigurationInvocationHandler;
import eu.ensure.commons.lang.configuration.PropertiesConfigurationInvocationHandler;
import eu.ensure.commons.io.Closer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/**
 * Implements an abstraction over properties-files (or other configuration schemas) and
 * allows <strong>type safe</strong> access to configuration parameters - even though the
 * Properties-class only deals with Strings.
 * <p/>
 * Use as follows:
 * <pre>
 * public class MyClass {
 *     private interface MyConfiguration {
 *         @Configurable(property = "key", value = "default value, if needed")
 *         String aString();
 *
 *         @Configurable(property = "some other key")
 *         int anInt();
 *     }
 *
 *     public MyClass() {
 *         Properties props = ConfigurationTool.loadFromResource(MyClass.class, "configuration.xml");
 *         MyConfiguration config = ConfigurationTool.bindProperties(MyConfiguration.class, props);
 *
 *         // Now you may access the configuration in a type safe manner
 *         String myString = config.aString();
 *         int myInt = config.anInt();
 *
 *         // Also, if no value was provided in the properties file, the default "value" will be
 *         // returned. This works even for integers, booleans etc.
 *     }
 * }
 * </pre>
 * <p/>
 * Other means to load the configuration is provided by ConfigurationTool, such as:
 * <pre>
 *     Properties props1 = ConfigurationTool.load("/path/to/my/configuration.properties");
 *     Properties props2 = ConfigurationTool.load("/path/to/my/configuration.xml");
 *
 *     File file = new File("/some/other/path/to/a/configuration.xml");
 *     Properties props3 = ConfigurationTool.load(file);
 * </pre>
 * <p/>
 * Note that ConfigurationTool will handle both classic .properties files as well as XML-files using the
 * properties DTD:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
 *
 * &lt;properties&gt;
 *     &lt;comment&gt;
 *         This file is used for testing ConfigurationTool and @Configurable
 *     &lt;/comment&gt;
 *     &lt;entry key="key"&gt;
 *         just some string value
 *     &lt;/entry&gt;
 *     &lt;entry key="some other key"&gt;
 *         42
 *     &lt;/entry&gt;
 * &lt;/properties&gt;
 * </pre>
 * <p/>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class ConfigurationTool {

    /**
     * [Default] path to the environment location.
     * <p/>
     * The value is
     * <I>"java:comp/env"</I>
     */
    public static final String JNDI_ENVIRONMENT = "java:comp/env";


    public interface ConfigurationResolver {
        Object resolve(String name);
    }

    private ConfigurationTool() {} // may not be instantiated by user

    private static final Object lock = new Object();
    private static Properties globalProperties = null; // if provided

    @SuppressWarnings("unchecked")
    public static <T> T bind(Class<T> clazz, Map defaultValues) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[] { clazz },
                new GeneralizedConfigurationInvocationHandler(defaultValues)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> T bind(Class<T> clazz, Map defaultValues, Collection<ConfigurationResolver> resolvers) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[] { clazz },
                new GeneralizedConfigurationInvocationHandler(defaultValues, resolvers)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> T bindProperties(Class<T> clazz, Properties properties) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[] { clazz },
                new PropertiesConfigurationInvocationHandler(properties)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> T bindProperties(Class<T> clazz, Properties properties, Collection<ConfigurationResolver> resolvers) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[] { clazz },
                new PropertiesConfigurationInvocationHandler(properties, resolvers)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> T bindProperties(Class<T> clazz) {
        synchronized(lock) {
            if (null == globalProperties) {
                String info = "Tried to bind the configuration of " + clazz.getName();
                info += " to a non-existing *global* configuration!";
                throw new RuntimeException(info);
            }
            return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class[] { clazz },
                new PropertiesConfigurationInvocationHandler(globalProperties)
            );
        }
    }

    private static boolean isXML(File file) {
        return file.getName().toLowerCase().endsWith(".xml");
    }

    private static boolean isXML(String path) {
        return path.toLowerCase().endsWith(".xml");
    }

    public static Properties load(InputStream is, boolean isXML) throws IOException {
        Properties properties = new Properties();
        if (isXML) {
            properties.loadFromXML(is);
        } else {
            properties.load(is);
        }
        return properties;
    }

    public static Properties load(File propertiesFile) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(propertiesFile);
            return load(is, isXML(propertiesFile));
        } finally {
            Closer.close(is);
        }
    }

    public static Properties load(String path) throws IOException {
        return load(new File(path));
    }

    public static Properties loadFromResource(Class clazz, String resourceName) throws IOException {
        InputStream is = null;
        try {
            is = clazz.getResourceAsStream(resourceName);
            return load(is, isXML(resourceName));
        } finally {
            Closer.close(is);
        }
    }

    public static void useGlobalConfiguration(Properties properties) {
        synchronized(lock) {
            globalProperties = properties;
        }
    }

    /**
     * Looks up (configuration) resources in JNDI or system environment.
     * @param resourceName
     * @return
     */
    public static String lookup(String resourceName) {
        String resource = null;

        // 1. Check in the JNDI tree
        Context ctx = null;
        try {
            ctx = (Context) new InitialContext().lookup(JNDI_ENVIRONMENT);
            resource = (String) ctx.lookup(resourceName);
        }
        catch (NamingException ignore) {}

        // 2. Check among system properties
        if (null == resource || resource.length() == 0) {
            System.out.println("Resource \"" + resourceName + "\" not published through JNDI...");
            try {
                resource = System.getProperty(resourceName);
            }
            catch (Exception ignore) {}
        }

        if (null == resource || resource.length() == 0) {
            System.out.println("Resource \"" + resourceName + "\" not published through system properties...");
        }
        return resource;
    }
}
