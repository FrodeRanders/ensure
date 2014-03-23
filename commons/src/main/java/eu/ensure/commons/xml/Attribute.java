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

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;

import javax.xml.namespace.QName;

/*
 * Description of Attribute
 * <p/>
 * <p/>
 * Created by Frode Randers at 2012-04-11 21:50
 */
public class Attribute {
    private Namespaces namespaces;

    public Attribute(Namespaces namespaces) {
        this.namespaces = namespaces;
    }

    //
    public String getValueFrom(OMElement element, String name) throws XmlException {
        return getValueFrom(element, namespaces, name);
    }

    public String getValueFrom(OMElement element, String name, boolean acceptFailure) throws XmlException {
	return getValueFrom(element, namespaces, null, name, acceptFailure);
    }

    public static String getValueFrom(OMElement element, Namespaces namespaces, String name) throws XmlException {
        return getValueFrom(element, namespaces, null, name, /* accept fail? */ false);
    }

    //
    public String getValueFrom(OMElement element, String namespacePrefix, String name) throws XmlException {
        return getValueFrom(element, namespaces, namespacePrefix, name);
    }

    public String getValueFrom(OMElement element, String namespacePrefix, String name, boolean acceptFailure) throws XmlException {
        return getValueFrom(element, namespaces, namespacePrefix, name, acceptFailure);
    }

    public static String getValueFrom(OMElement element, Namespaces namespaces, String namespacePrefix, String name) throws XmlException {
        return getValueFrom(element, namespaces, namespacePrefix, name, /* accept fail? */ false);
    }

    //
    public static String getValueFrom(OMElement element, Namespaces namespaces, String namespacePrefix, String name, boolean acceptFail) throws XmlException {
        OMAttribute attrib;
        if (null != namespacePrefix && namespacePrefix.length() > 0) {
            OMNamespace ns = namespaces.get(namespacePrefix);
            if (null == ns) {
                String info = "There is no defined namespace that matches prefix: " + namespacePrefix;
                throw new XmlException(info);
            }
            attrib = element.getAttribute(new QName(ns.getNamespaceURI(), name));
        } else {
            attrib = element.getAttribute(new QName(name));
        }
        if (null == attrib) {
            if (acceptFail) {
                return null;
            }

            String info = "Node does not have an attribute named \"";
            if (null != namespacePrefix && namespacePrefix.length() > 0) {
                info += namespacePrefix + ":";
            }
            info += name + "\" as expected. ";
            info += "The current node is ";
            info += element.toString();
            throw new XmlException(info);
        }
        return attrib.getAttributeValue();
    }
}
