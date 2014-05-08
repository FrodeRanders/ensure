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
package  eu.ensure.commons.xml;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/*
 * Description of Serializer.
 * <p>
 * Serializes any object into XML (String) that has the JAXB annotations:
 * <pre>
 * @XmlRootElement
 * @XmlAccessorType(XmlAccessType.FIELD)
 * set on the type (class)
 * </pre>
 * <p>
 * Created by Frode Randers at 2013-01-27 23:21
 */
public class Serializer {

    public static String obj2XmlUtf16(Class clazz, Object o)  throws JAXBException, ParserConfigurationException {
        Document doc = obj2Doc(clazz, o);

        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
        if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
            lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        }
        lsSerializer.setNewLine("\n");
        return lsSerializer.writeToString(doc); // Produces UTF-16
    }


    /** Serializes an object into XML
     * <p>
     * You need to catch the JAXBException, but beware that the cause is not conveyed correctly through getCause().
     * Take a peek at the internal 'cause' member and look out for detailed information about the problem.
     *
     *      com.sun.xml.internal.bind.v2.runtime.IllegalAnnotationsException: 2 counts of IllegalAnnotationExceptions
     *      x.y.A does not have a no-arg default constructor.
     *          this problem is related to the following location:
     *              at x.y.A
     *              at public java.util.Collection x.y.A.getEntries()
     *              at x.y.Z
     *      x.y.B does not have a no-arg default constructor.
     *          this problem is related to the following location:
     *              at x.y.B
     *              at public java.util.Collection x.y.A.getEntries()
     *              at x.y.A
     *              at public java.util.Collection x.y.Z.getEntries()
     *              at x.y.Z
     *
     * You need to take a peek at 'cause' in a debugger when initially developing the application.
     */
    public static String obj2Xml(Class clazz, Object o)  throws JAXBException, ParserConfigurationException {
        Document doc = obj2Doc(clazz, o);

        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
        if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
            lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
        }
        lsSerializer.setNewLine("\n");

        try {
            // Encode as UTF-8
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            LSOutput lsOutput = domImplementation.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            lsOutput.setByteStream(baos);
            lsSerializer.write(doc, lsOutput);

            return baos.toString("UTF-8");
        }
        catch (UnsupportedEncodingException uee) {
            return lsSerializer.writeToString(doc); // Produces UTF-16
        }
    }

    public static Document obj2Doc(Class clazz, Object o) throws JAXBException, ParserConfigurationException {
        JAXBContext pContext = JAXBContext.newInstance(clazz);

        Marshaller marshaller = pContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        marshaller.marshal(o, doc);

        doc.setXmlStandalone(true);
        return doc;
    }
}


/*
    public static void canonicalizeWithDOM3LS(Document document,
            OutputStream out) {
        DOMImplementationLS domImpl = getDOMImplementationLS(document);
        LSSerializer lsSerializer = domImpl.createLSSerializer();
        DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
        if (domConfiguration.canSetParameter("canonical-form", Boolean.TRUE)) {
            lsSerializer.getDomConfig().setParameter("canonical-form",
                    Boolean.TRUE);
            LSOutput lsOutput = domImpl.createLSOutput();
            lsOutput.setEncoding("UTF-8");
            lsOutput.setByteStream(out);
            lsSerializer.write(document, lsOutput);
        } else {
            throw new RuntimeException(
                    "DOMConfiguration 'canonical-form' parameter isn't settable.");
        }
    }
*/