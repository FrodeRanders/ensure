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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import java.util.LinkedList;
import java.util.List;

/*
 * Description of XPath
 * <p>
 * <p>
 * Created by Frode Randers at 2012-04-11 21:50
 */
public class XPath {
    private Namespaces namespaces;

    public XPath(Namespaces namespaces) {
        this.namespaces = namespaces;
    }

    public String getTextFrom(OMNode element, String expression) throws XmlException {
        return getTextFrom(element, namespaces, expression, /* accept failure? */ false);
    }

    public String getTextFrom(OMNode element, String expression, boolean acceptFailure) throws XmlException {
        return getTextFrom(element, namespaces, expression, acceptFailure);
    }

    public static String getTextFrom(OMNode element, Namespaces namespaces, String expression) throws XmlException {
        return getTextFrom(element, namespaces, expression, /* accept failure? */ false);
    }

    public static String getTextFrom(OMNode element, Namespaces namespaces, String expression, boolean acceptFailure) throws XmlException {
        try {
            AXIOMXPath xpath = new AXIOMXPath(expression);
            namespaces.applyNamespacesOn(xpath);

            Object node = xpath.selectSingleNode(element);
            if (null != node) {
                if (node instanceof OMText) {
                    return ((OMText)node).getText().trim();
                }
                else if (node instanceof OMElement) {
                    return ((OMElement)node).getText().trim();
                }
            }

            if (acceptFailure) {
                return null;
            }

            String info = "XPath expression \"" + expression + "\" does not identify text. ";
            info += "The current element is ";
            info += element.toString();
            throw new XmlException(info);

        } catch (JaxenException je) {
            throw new XmlException("Could not query using expression \"" + expression + "\": " + je.getMessage(), je);
        }
    }

    /*
     * Upon failure to find element, the default behaviour is to throw an exception.
     */

    public OMElement getElementFrom(OMNode element, String expression) throws XmlException {
        return getElementFrom(element, namespaces, expression, /* accept failure? */ false);
    }

    public OMElement getElementFrom(OMNode element, String expression, boolean acceptFailure) throws XmlException {
        return getElementFrom(element, namespaces, expression, acceptFailure);
    }

    public static OMElement getElementFrom(OMNode element, Namespaces namespaces, String expression) throws XmlException {
        return getElementFrom(element, namespaces, expression, /* accept failure? */ false);
    }

    public static OMElement getElementFrom(OMNode element, Namespaces namespaces, String expression, boolean acceptFailure) throws XmlException {
        try {
            AXIOMXPath xpath = new AXIOMXPath(expression);
            namespaces.applyNamespacesOn(xpath);

            Object node = xpath.selectSingleNode(element);
            if (null != node && node instanceof OMElement) {
                return (OMElement) node;
            }

            if (acceptFailure) {
                return null;
            }

            String info = "XPath expression \"" + expression + "\" does not identify an element. ";
            info += "The current element is ";
            info += element.toString();
            throw new XmlException(info);

        } catch (JaxenException je) {
            throw new XmlException("Could not query using expression \"" + expression + "\": " + je.getMessage(), je);
        }
    }


    /*
     * Upon failure to find elements, the default behaviour is just to return an empty list of elements
     */
    public List<OMElement> getElementsFrom(OMNode element, String expression) throws XmlException {
        return getElementsFrom(element, namespaces, expression, /* accept failure? */ true);
    }

    public List<OMElement> getElementsFrom(OMNode element, String expression, boolean acceptFailure) throws XmlException {
        return getElementsFrom(element, namespaces, expression, acceptFailure);
    }

    public static List<OMElement> getElementsFrom(OMNode element, Namespaces namespaces, String expression) throws XmlException {
        return getElementsFrom(element, namespaces, expression, /* accept failure? */ true);
    }

    public static List<OMElement> getElementsFrom(OMNode element, Namespaces namespaces, String expression, boolean acceptFailure) throws XmlException {
        try {
            AXIOMXPath xpath = new AXIOMXPath(expression);
            namespaces.applyNamespacesOn(xpath);

            List<OMElement> elements = xpath.selectNodes(element);
            if (null == elements) {
                if (acceptFailure) {
                    elements = new LinkedList<OMElement>(); // empty list
                }
                else {
                    String info = "XPath expression \"" + expression + "\" does not identify any elements. ";
                    info += "The current element is ";
                    info += element.toString();
                    throw new XmlException(info);
                }
            }
            return elements;
        } catch (JaxenException je) {
            throw new XmlException("Could not query using expression \"" + expression + "\": " + je.getMessage(), je);
        }
    }
}
