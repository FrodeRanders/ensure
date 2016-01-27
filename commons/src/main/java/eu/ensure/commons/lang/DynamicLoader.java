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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

/**
 * A dynamic loader is used to dynamically load objects.
 * <p>
 * Example:
 * <p>
 * We have an XML-file describing processors, containing a processor name
 * (the key) and a class to implement that processor. In the general case
 * this could be any mapping to any kind of plugin:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
 * 
 * &lt;properties&gt;
 *     &lt;comment&gt;
 *         This file contains mappings from processor names (of your choosing)
 *         to classes that implement them.
 *     &lt;/comment&gt;
 *     &lt;entry key="SillyProcessor"&gt;
 *         eu.ensure.ltu.silly.SillyProcessor
 *     &lt;/entry&gt;
 * &lt;/properties&gt;
 * </pre>
 * We load this configuration file, using the Class.getResourceAsStream()
 * mechanism, and feed the resulting InputStream to the load method.
 * During load the individual classes are resolved and a new instance
 * is wrought from each class:
 * <pre>
 * <b>DynamicLoader</b>&lt;Processor&gt; <b>processors</b> = new <b>DynamicLoader</b>&lt;Processor&gt;(<i>"processor"</i>);
 * InputStream is = null;
 * try {
 *     is = getResourceAsStream("processors.xml");
 *     if (null == is) {
 *         throw new IOException("Processors mapping file not found");
 *     }
 *
 *     <b>processors.load</b>(<i>is</i>);
 *
 * } catch (IOException ioe) {
 *     String info =
 *         "Failed to load processors to class mapping";
 *     info += ": ";
 *     info += ioe.getMessage();
 *     throw new IOException(info);
 * } finally {
 *     if (null != is) is.close();
 * }
 * </pre>
 * Being a Hashtable&lt;String, C&gt;, we may now refer to the individual
 * processors (plugins) through the <i>processors</i> object.
 * <p>
 * The next example is the process() method of the processor.
 * <pre>
 * public boolean process(String action, String path, FileChannel fileChannel) throws ProcessorException {
 *
 *     // Get processor implementation
 *     Processor processorImpl = <b>processors.get</b>(<i>action</i>); // "SillyProcessor", ...
 *
 *     if (null == processorImpl) {
 *         reply.append("Unknown processor: ");
 *         reply.append(action);
 *         return 1;
 *     }
 *
 *     // Call processor
 *     return processorImpl.process(path, fileChannel);
 * }
 * </pre>
 * Remains the problem of initializing the Processor object (the plugin).
 * In our case, a processor should really be initiated with
 * a reference to a ProcessorContext. The problem at hand
 * is to inject some kind of initialization into the loading
 * process, reading class names from an XML file and instantiating
 * objects.
 * <p>
 * The solution is to use a
 * {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;Processor&gt;,
 * specifically targeted at Processor plugins.
 * <p>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class DynamicLoader<C> extends Hashtable<String, C> {
    private static final Logger log = LogManager.getLogger(DynamicLoader.class);

    private String description;

    /**
     * Constructor.
     * <p>
     * The <i>description</i> parameter is used when producing
     * log output and has no other function. It makes the log
     * a whole lot easier to read - do use it!
     */
    public DynamicLoader(String description) {
        this.description = description;
    }

    /**
     * Dynamically loads objects from an InputStream, containing the content of
     * a Properties object (either XML or normal Properties-files layout).
     * <p>
     * Supports the use of a dynamic initializer
     * (see {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;C&gt;)
     * <p>
     * The <i>assignKey</i> parameter instructs the loader to
     * assign the key name to the dynamic object by calling
     * the method assignKey(String key) - if it exists.
     */
    public void load(InputStream is, DynamicInitializer<C> di, boolean assignKey) throws IOException, ClassNotFoundException {
        Properties map = new Properties();
        map.loadFromXML(is);
        load(map, di, assignKey);
    }

    /**
     * Dynamically loads objects from an InputStream, containing the content of
     * a Properties object (either XML or normal Properties-files layout).
     * <p>
     * Supports the use of a dynamic initializer
     * (see {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;C&gt;)
     */
    public void load(InputStream is, DynamicInitializer<C> di) throws IOException, ClassNotFoundException {
        load(is, di, /* assign key? */ false);
    }

    /**
     * Dynamically loads objects from an InputStream, containing the content of
     * a Properties object (either XML or normal Properties-files layout).
     */
    public void load(InputStream is) throws IOException, ClassNotFoundException {
        load(is, /* dynamic initializer */ null, /* assign key? */ false);
    }

    /**
     * Dynamically loads objects from a Properties object.
     * <p>
     * Supports the use of a dynamic initializer
     * (see {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;C&gt;)
     * <p>
     * The <i>assignKey</i> parameter instructs the loader to
     * assign the key name to the dynamic object by calling
     * the method assignKey(String key) - if it exists.
     */       
    public void load(Properties map, DynamicInitializer<C> di, boolean assignKey) throws ClassNotFoundException {
        Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String _className = map.getProperty(key);
            String className = (_className != null ? _className.trim() : null);

            if (null == className || className.length() == 0) {
                String info = "Misconfiguration? No class name specified for " + description + " " + key;
                info += ": Check the configuration!";
                log.warn(info);
                continue;
            }

            C object = load(className, di);
            if (null != object) {
                if (assignKey) {
                    // Method name and parameters
                    String methodName = "assignKey"; // predefined
                    Object[] parameters = { key };

                    // Method call
                    try {
                        callMethodOn(object, methodName, parameters);
                    } catch (Throwable ignore) {
                    }
                }
                put(key, object);
            }
        }
    }

    /**
     * Dynamically loads objects from a Properties object.
     * <p>
     * Supports the use of a dynamic initializer
     * (see {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;C&gt;)
     */
    public void load(Properties map, DynamicInitializer<C> di) throws ClassNotFoundException {
        load(map, di, /* assign key? */ false);
    }

    /**
     * Dynamically loads objects from a Properties object.
     */
    public void load(Properties map) throws ClassNotFoundException {
        load(map, /* dynamic initializer */ null, /* assign key? */ false);
    }

    /**
     * Dynamically loads the named class (fully qualified classname) and
     * creates an instance from it.
     * <p>
     * Supports the use of a dynamic initializer
     * (see {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;C&gt;)
     */
    public C load(String className, DynamicInitializer<C> di) throws ClassNotFoundException {

        Class clazz = createClass(className);
        return createObject(className, clazz, di);
    }

    /**
     * Dynamically loads the named class (fully qualified classname).
     */
    public Class createClass(String className) throws ClassNotFoundException {
        Class clazz;
        try {
            clazz = Class.forName(className);
            return clazz;

        } catch (ExceptionInInitializerError eiie) {
            String info = "Could not load the " + description + " object: " + className
                    + ". Could not initialize static object in server: ";
            info += eiie.getMessage();
            throw new ClassNotFoundException(info, eiie);

        } catch (LinkageError le) {
            String info = "Could not load the " + description + " object: " + className
                    + ". This object is depending on a class that has been changed after compilation ";
            info += "or a class that was not found: ";
            info += le.getMessage();
            throw new ClassNotFoundException(info, le);

        } catch (ClassNotFoundException cnfe) {
            String info = "Could not find the " + description + " object: " + className + ": ";
            info += cnfe.getMessage();
            throw new ClassNotFoundException(info, cnfe);
        }
    }

    /**
     * Creates an instance from a Class.
     * <p>
     * Supports the use of a dynamic initializer
     * (see {@link  eu.ensure.commons.lang.DynamicInitializer}&lt;C&gt;)
     */
    public C createObject(String className, Class clazz, DynamicInitializer<C> di) throws ClassNotFoundException {
        C object;
        try {
            object = (C) clazz.newInstance();

        } catch (InstantiationException ie) {
            String info = "Could not create " + description + " object: " + className
                    + ". Could not access object constructor: ";
            info += ie.getMessage();
            throw new ClassNotFoundException(info, ie);

        } catch (IllegalAccessException iae) {
            String info = "Could not create " + description + " object: " + className
                    + ". Could not instantiate object. Does the object classname refer to an abstract class, "
                    + "an interface or the like?: ";
            info += iae.getMessage();
            throw new ClassNotFoundException(info, iae);

        } catch (ClassCastException cce) {
            String info = "Could not create " + description + " object: " + className
                    + ". The specified object classname does not refer to the proper type: ";
            info += cce.getMessage();
            throw new ClassNotFoundException(info, cce);
        }

        // Initialize object
        if (null != di) {
            di.initialize(object);
        }

        return object;
    }

    /**
     * Creates an instance from a Class.
     */
    public C createObject(String className, Class clazz) throws ClassNotFoundException {
        return createObject(className, clazz, /* no dynamic init */ null);
    }

    /**
     * Calls a method on an object. Beware that this version of callMethodOn() will
     * assume the exact parameter types in the designated method as has the parameters
     * to this call.
     * <p>
     * An effect of this is that if you call a method assuming a type T with an
     * object of the derived type D, the method call will fail. In this case (when
     * using polymorphous parameters) you should use the version also taking an
     * array of parameter types (see below).
     * <p>
     * Example:
     * <pre>
     * // Method name
     * String methodName = "assignKey"; // predefined
     *
     * // Parameter values
     * Object[] parameters = { key };
     *
     * // Method call
     * callMethodOn(object, methodName, parameters);
     * </pre>
     * <p>
     * @param object - any object
     * @param methodName - name of (public) method
     * @param parameters - an array of values matching the parameterTypes array
     * @throws ClassNotFoundException - if method is not found, method is not public, parameters does not match, etc.
     */
    public void callMethodOn(C object, String methodName, Object[] parameters)
        throws Throwable {

        // Dynamically determine parameter types
        Class[] parameterTypes = new Class[parameters.length];
        for (int i=0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getClass();
        }

        Class clazz = object.getClass();
        try {
            Method method = clazz.getMethod(methodName, parameterTypes);
            method.invoke(object, parameters);

        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (null != cause) {
                throw cause;
            }
            String info = "Could not invoke \"" + methodName + "\" on \"" + clazz.getName() + "\"";
            info += " as expected: ";
            info += ite.getMessage();
            throw new ClassNotFoundException(info);

        } catch (IllegalAccessException iae) {
            String info = "The specified class " + clazz.getName();
            info += " does not have a public method \"" + methodName + "\" as expected: ";
            info += iae.getMessage();
            throw new ClassNotFoundException(info);

        } catch (NoSuchMethodException nsme) {
            String info = "The specified class " + clazz.getName();
            info += " does not have a method \"" + methodName + "\" as expected: ";
            info += nsme.getMessage();
            throw new ClassNotFoundException(info);
        }
    }

    /**
     * Calls a method on an object. Will not dynamically determine parameter types.
     * <p>
     * May be used to call methods with polymorphous parameters, i.e. methods
     * taking parameters such as List, Map, etc (that are abstract).
     * <p>
     * Example:
     * <pre>
     * // Method name
     * String methodName = "assignList"; // predefined
     *
     * // Parameter types and values
     * Object[] parameters = { new Vector&lt;String&gt;() };
     * Class[] types = { List.class };
     *
     * // Method call
     * callMethodOn(object, methodName, parameters, types);
     * </pre>
     * <p>
     * @param object - any object
     * @param methodName - name of (public) method
     * @param parameters - an array of values matching the parameterTypes array
     * @param parameterTypes - an array of parameter types (Class) matching parameters array
     * @throws ClassNotFoundException - if method is not found, method is not public, parameters does not match, etc.
     */
    public void callMethodOn(C object, String methodName, Object[] parameters, Class[] parameterTypes)
        throws Throwable {

        Class clazz = object.getClass();
        try {
            Method method = clazz.getMethod(methodName, parameterTypes);
            method.invoke(object, parameters);

        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (null != cause) {
                throw cause;
            }
            String info = "Could not invoke \"" + methodName + "\" on \"" + clazz.getName() + "\"";
            info += " as expected: ";
            info += ite.getMessage();
            throw new ClassNotFoundException(info);

        } catch (IllegalAccessException iae) {
            String info = "The specified class " + clazz.getName();
            info += " does not have a public method \"" + methodName + "\" as expected: ";
            info += iae.getMessage();
            throw new ClassNotFoundException(info);

        } catch (NoSuchMethodException nsme) {
            String info = "The specified class " + clazz.getName();
            info += " does not have a method \"" + methodName + "\" as expected: ";
            info += nsme.getMessage();
            throw new ClassNotFoundException(info);
        }
    }
}
