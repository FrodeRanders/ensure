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

import eu.ensure.commons.io.Closer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This configuration resolver will (try to) lookup configuration among the bundled properties, bundled
 * together with some class.
 * <p/>
 * Format of the properties file should be as follows:
 * <pre>
 *   &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *   &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
 *
 *   &lt;properties&gt;
 *     &lt;comment&gt;
 *       This file contains ...
 *     &lt;/comment&gt;
 *     &lt;entry key="key 1"&gt;
 *       value 1
 *     &lt;/entry>
 *     &lt;entry key="key 2"&gt;
 *       value 2
 *     &lt;/entry&gt;
 *   &lt;/properties&gt;
 * </pre>
 * <p/>
 * Created by Frode Randers at 2014-02-06 22:13
 */
public class BundledPropertiesConfigurationResolver implements ConfigurationTool.ConfigurationResolver {
    private static final Logger log = Logger.getLogger(BundledPropertiesConfigurationResolver.class);

    private final Properties properties;

    public BundledPropertiesConfigurationResolver(Class clazz, String bundledFileName) throws IOException {
        properties = new Properties();

        if (null != bundledFileName && bundledFileName.length() > 0) {
            InputStream is = null;
            try {
                is = clazz.getResourceAsStream(bundledFileName);
                if (null != is) {
                    if (bundledFileName.toLowerCase().endsWith("xml")) {
                        // Treat as an XML file
                        properties.loadFromXML(is);
                    }
                    else {
                        // Threat as a .properties file
                        properties.load(is);
                    }
                }
            }
            finally {
                Closer.close(is);
            }
        }
    }

    @Override
    public Object resolve(String name) {
        String value = properties.getProperty(name);

        if (null != value && log.isDebugEnabled()) {
            String info = "Successfully resolved \"" + name + "\" from bundled properties: " + value;
            log.debug(info);
        }
        return value;
    }
}
