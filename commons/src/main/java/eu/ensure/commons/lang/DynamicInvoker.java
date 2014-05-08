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

import org.apache.log4j.Logger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

/**
 * Invokes methods on dynamically compiled Java programs, i.e. where we do not know the class
 * on compile-time and thus cannot use the DynamicLoader&lt;C&gt; functionality. Some methods
 * from DynamicLoader are replicated, but using compile-time "anonymous" Class and Object
 * parameters.
 * <p>
 * Created by Frode Randers at 2012-07-28 17:24
*/
public class DynamicInvoker {
    private static final Logger log = Logger.getLogger(DynamicInvoker.class);

    private final File root;
    private final String description;

    /**
     * Constructor.
     * <p>
     * The <i>description</i> parameter is used when producing
     * log output and has no other function. It makes the log
     * a whole lot easier to read - do use it!
     */
    public DynamicInvoker(File root, String description) {
        this.root = root;
        this.description = description;
    }

    /**
     * Invokes a method on an object. Will not dynamically determine parameter types.
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
     * @param className name of class in classpath
     * @param methodName name of (public) method
     * @param parameters an array of values matching the parameterTypes array
     * @param parameterTypes an array of parameter types (Class) matching parameters array
     * @throws ClassNotFoundException - if method is not found, method is not public, parameters does not match, etc.
     */
    public void invoke(String className, String methodName, Object[] parameters, Class[] parameterTypes)
        throws Throwable {

        Class clazz = createClass(className);
        Object object = createObject(className, clazz);
        callMethodOn(clazz, object, methodName, parameters, parameterTypes);
    }

    /**
     * Dynamically loads the named class (fully qualified classname).
     */
    private Class createClass(String className) throws ClassNotFoundException {
        Class clazz;
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{root.toURI().toURL()});
            if (log.isDebugEnabled()) {
                log.debug("Classloader URLs:");
                for (URL url : classLoader.getURLs()) {
                    log.debug("  " + url.toString());
                }
            }

            clazz = Class.forName(className, true, classLoader);
            return clazz;

        } catch (MalformedURLException mue) {
            String info = "Could not load the " + description + " object " + className
                    + ". Malformed URL: ";
            info += mue.getMessage();
            throw new ClassNotFoundException(info, mue);

        } catch (ExceptionInInitializerError eiie) {
            String info = "Could not load the " + description + " object " + className
                    + ". Could not initialize static object in server: ";
            info += eiie.getMessage();
            throw new ClassNotFoundException(info, eiie);

        } catch (LinkageError le) {
            String info = "Could not load the " + description + " object " + className
                    + ". This object is depending on a class that has been changed after compilation ";
            info += "or a class that was not found: ";
            info += le.getMessage();
            throw new ClassNotFoundException(info, le);

        } catch (ClassNotFoundException cnfe) {
            String info = "Could not find the " + description + " object " + className + ": ";
            info += cnfe.getMessage();
            throw new ClassNotFoundException(info, cnfe);
        }
    }

    /**
     * Dynamically creates the object
     */
    public Object createObject(String className, Class clazz) throws ClassNotFoundException {
        Object object;
        try {
            object = clazz.newInstance();

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

        return object;
    }

    /**
     * Dynamically calls a method
     */
    private Object callMethodOn(Class clazz, Object object, String methodName, Object[] parameters, Class[] parameterTypes)
        throws Throwable {

        try {
            Method method = clazz.getMethod(methodName, parameterTypes);
            return method.invoke(object, parameters);

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
