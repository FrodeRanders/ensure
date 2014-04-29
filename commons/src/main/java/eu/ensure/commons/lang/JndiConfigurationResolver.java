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
package  eu.ensure.commons.lang;

import org.apache.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This configuration resolver will (try to) lookup configuration in the JNDI tree.
 * <p/>
 * Created by Frode Randers at 2014-02-06 16:07
 */
public class JndiConfigurationResolver implements ConfigurationTool.ConfigurationResolver {
    private static final Logger log = Logger.getLogger(JndiConfigurationResolver.class);
    public static final String DEFAULT_JNDI_ENVIRONMENT = "java:comp/env";

    private final String jndiEnvironment;

    public JndiConfigurationResolver() {
        this.jndiEnvironment = DEFAULT_JNDI_ENVIRONMENT;
    }

    public JndiConfigurationResolver(final String jndiEnvironment) {
        this.jndiEnvironment = jndiEnvironment;
    }

    @Override
    public Object resolve(String name) {
        try {
            if (name.contains("/")) {
                // Handles things like "jdbc/..."
                Context ctx = new InitialContext();
                Object value = ctx.lookup(name);

                if (null != value && log.isDebugEnabled()) {
                    String info = "Successfully resolved \"" + name + "\" from JNDI: " + value.toString();
                    log.debug(info);
                }
                return value;

            } else {
                // Handles the rest typically at "java:comp/env/..."
                Context ctx = (Context) new InitialContext().lookup(jndiEnvironment);
                Object value = ctx.lookup(name);

                if (null != value && log.isDebugEnabled()) {
                    String info = "Successfully resolved \"" + name + "\" from JNDI: " + value.toString();
                    log.debug(info);
                }
                return value;
            }
        } catch (NamingException ignore) {}
        return null; // failed to lookup resource, next resolver have to cope with this
    }
}
