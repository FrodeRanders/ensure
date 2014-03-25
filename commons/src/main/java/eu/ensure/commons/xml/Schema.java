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

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import org.apache.log4j.Logger;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Makes assessments of message digest/hash/fixity algorithms.
 * <p/>
 * Created by Frode Randers at 2012-10-25 16:59
 */
public class Schema {
    private static final Logger log = Logger.getLogger(Schema.class);

    private InputStream schemaStream = null;

    public Schema(InputStream schemaStream) {
        this.schemaStream = schemaStream;
    }

    public boolean validate(XMLStreamReader reader) {
        try {
            javax.xml.validation.Schema schema = createSchema();
            javax.xml.validation.Validator validator = schema.newValidator();

            final List<String> errors = new LinkedList<String>();

            ErrorHandler errorHandler = new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException {
                    String info = "WARN: Failed to validate (";
                    info += "system-id=" + e.getSystemId();
                    info += ", public-id=" + e.getPublicId();
                    info += ") at " + e.getLineNumber() + ":";
                    info += e.getColumnNumber();
                    errors.add(info);
                    log.warn(info, e);
                }

                public void error(SAXParseException e) throws SAXException {
                    String info = "ERROR: Failed to validate (";
                    info += "system-id=" + e.getSystemId();
                    info += ", public-id=" + e.getPublicId();
                    info += ") at " + e.getLineNumber() + ":";
                    info += e.getColumnNumber();
                    errors.add(info);
                    log.warn(info, e);
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    String info = "FATAL: Failed to validate (";
                    info += "system-id=" + e.getSystemId();
                    info += ", public-id=" + e.getPublicId();
                    info += ") at " + e.getLineNumber() + ":";
                    info += e.getColumnNumber();
                    errors.add(info);
                    log.warn(info, e);
                }
            };
            validator.setErrorHandler(errorHandler);
            validator.validate(new StAXSource(reader));

            return errors.isEmpty();
        }
        catch (SAXException saxe) {
            return false;
        }
        catch (IOException ioe) {
            return false;
        }
    }

    private javax.xml.validation.Schema createSchema() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
        LSResourceResolver resolver = new LocalSchemaLSResourceResolver();
        factory.setResourceResolver(resolver);
        javax.xml.validation.Schema schema = factory.newSchema();
        return schema;
    }

    private class LocalSchemaLSResourceResolver implements LSResourceResolver {
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
            String info = "Resolving resource: type=" + type;
            info += ", namespaceURI=" + namespaceURI;
            info += ", publicId=" + publicId;
            info += ", systemId=" + systemId;
            info += ", baseURI=" + baseURI;
            log.info(info);

            LSInput input = new DOMInputImpl();
            input.setByteStream(schemaStream);
            return input;
        }
    }
}
