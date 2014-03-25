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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Description of GeneralizedConfigurationInvocationHandler:
 * <p/>
 * <p/>
 * Created by Frode Randers at 2013-01-19 12:55
 */
public class GeneralizedConfigurationInvocationHandler implements InvocationHandler {
    private static final String DESCRIPTION =
            "Dynamic configuration object handled by proxy by " +
                    GeneralizedConfigurationInvocationHandler.class.getSimpleName();

    private Collection<ConfigurationTool.ConfigurationResolver> resolvers = new ArrayList<ConfigurationTool.ConfigurationResolver>();

    // We don't have to expose this default resolver
    private class DefaultResolver implements ConfigurationTool.ConfigurationResolver {
        private Map map;

        DefaultResolver(Map map) {
            this.map = map;
        }

        public Object resolve(String name) {
            return map.get(name);
        }
    }

    public GeneralizedConfigurationInvocationHandler(Map map) {
        resolvers.add(new DefaultResolver(map));
    }

    public GeneralizedConfigurationInvocationHandler(Map map, Collection<ConfigurationTool.ConfigurationResolver> resolvers) {
        for (ConfigurationTool.ConfigurationResolver resolver : resolvers) {
            this.resolvers.add(resolver);
        }
        this.resolvers.add(new DefaultResolver(map));
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

        Class targetType = method.getReturnType();
        Object value = null;
        for (ConfigurationTool.ConfigurationResolver resolver : resolvers) {
            value = resolver.resolve(key);
            if (null != value) {
                if (targetType.isAssignableFrom(value.getClass())) {
                    return value;
                }
                else {
                    throw new RuntimeException("Could not treat return value of " + method.getName() + " as \'" + targetType.getName() + "' when in fact it was '" + value.getClass().getName() + "'");
                }
            }
        }
        if (targetType.isAssignableFrom(String.class)) {
            // No configured value for this key - fall back on default value (if it is a String)
            return binding.value().trim();
        }
        return null;
    }
}
