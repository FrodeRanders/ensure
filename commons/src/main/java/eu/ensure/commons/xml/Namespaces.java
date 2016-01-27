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
package  eu.ensure.commons.xml;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import java.util.*;

/*
 * Description of Namespaces
 * <p>
 * <p>
 * Created by Frode Randers at 2012-04-11 21:50
 */
public class Namespaces {
    private Map<String, OMNamespace> namespaces = new HashMap<>();

    public Namespaces() {
        // No default namespaces defined yet...
    }

    public Namespaces(Iterator<OMNamespace> nsit) {
        while (nsit.hasNext()) {
            OMNamespace ns = nsit.next();
            defineNamespace(ns.getNamespaceURI(), ns.getPrefix());
        }
    }

    public Namespaces(Map<String,String> namespaces) {
        for (String prefix : namespaces.keySet()) {
            defineNamespace(namespaces.get(prefix), prefix);
        }
    }

    public OMNamespace defineNamespace(String uri, String prefix) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace namespace = factory.createOMNamespace(uri, prefix);
        namespaces.put(prefix, namespace);
        return namespace;
    }

    public void applyNamespacesOn(OMElement element) {
        for (OMNamespace ns : namespaces.values()) {
            element.declareNamespace(ns);
        }
    }

    public void applyNamespacesOn(AXIOMXPath xpath) throws JaxenException {
        for (OMNamespace ns : namespaces.values()) {
            xpath.addNamespace(ns.getPrefix(), ns.getNamespaceURI());
        }
    }

    public OMNamespace get(String prefix) {
        return namespaces.get(prefix);
    }

    public Map<String, OMNamespace> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }
}
