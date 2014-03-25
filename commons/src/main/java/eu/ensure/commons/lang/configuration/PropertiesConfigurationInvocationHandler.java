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
package  eu.ensure.commons.lang.configuration;

import eu.ensure.commons.lang.Configurable;
import eu.ensure.commons.lang.ConfigurationTool;
import eu.ensure.commons.lang.StringMapConfigurationResolver;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.util.*;

/**
 * Description of PropertiesConfigurationInvocationHandler:
 * <p/>
 * <p/>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class PropertiesConfigurationInvocationHandler implements InvocationHandler {
    private static final String DESCRIPTION =
            "Dynamic configuration object handled by proxy by " +
                    PropertiesConfigurationInvocationHandler.class.getSimpleName();

    private Collection<ConfigurationTool.ConfigurationResolver> resolvers = new ArrayList<ConfigurationTool.ConfigurationResolver>();

    public PropertiesConfigurationInvocationHandler(Properties properties) {
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<?,?> entry : properties.entrySet()) {
            map.put((String)entry.getKey(), (String)entry.getValue());
        }
        resolvers.add(new StringMapConfigurationResolver(map));
    }

    public PropertiesConfigurationInvocationHandler(
            Properties properties, Collection<ConfigurationTool.ConfigurationResolver> resolvers
    ) {
        for (ConfigurationTool.ConfigurationResolver resolver : resolvers) {
            this.resolvers.add(resolver);
        }
        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry<?,?> entry : properties.entrySet()) {
            map.put((String)entry.getKey(), (String)entry.getValue());
        }
        resolvers.add(new StringMapConfigurationResolver(map));
    }

    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        if ("toString".equals(method.getName())) {
            return DESCRIPTION;
        }

        Configurable binding = method.getAnnotation(Configurable.class);
        if (null == binding) {
            throw new RuntimeException("Method is not bound to a Configurable property: " + method);
        }

        // If we have a Configurable property name, use it - otherwise fall back on method name...
        String key = (null != binding.property() && binding.property().length() > 0) ? binding.property() : method.getName();

        //-----------------------------------------------------------------------
        // Properties only deals with Strings, so we will have to cast the
        // value into the correct type - identified by the method return type.
        //-----------------------------------------------------------------------
        Object value = null;
        for (ConfigurationTool.ConfigurationResolver resolver : resolvers) {
            value = resolver.resolve(key);
            if (null != value) {
                return cast(method.getName(), ((String)value).trim(), method.getReturnType());
            }
        }
        return cast(method.getName(), binding.value().trim(), method.getReturnType());
    }

    private Object cast(String name, String value, Class targetType) {
        if (null == value) {
            return null;
        }

        if (targetType.isAssignableFrom(String.class)) {
            return value;
        } else if (targetType.isAssignableFrom(Integer.class) || targetType.isAssignableFrom(int.class)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
                throw new RuntimeException("Could not treat return value of " + name + " as integer.");
            }
        } else if (targetType.isAssignableFrom(Boolean.class) || targetType.isAssignableFrom(boolean.class)) {
            return value.equalsIgnoreCase("true");
        } else {
            return value;
        }
        // Expect to add more casts :-)
    }
}
