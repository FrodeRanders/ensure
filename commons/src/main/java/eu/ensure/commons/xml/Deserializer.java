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

import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

/*
 * Description of Deserializer.
 * <p>
 * Deserializes an XML node hierarchy into an object. The class of that object need to have the JAXB annotations:
 * <pre>
 * @XmlRootElement  ???
 * @XmlAccessorType(XmlAccessType.FIELD)
 * set on the type (class)
 * </pre>
 * <p>
 * Created by Frode Randers at 2013-02-17 10:43
 */
public class Deserializer {

    public static Object xml2Obj(Class clazz, Node node)  throws JAXBException {
        JAXBContext pContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = pContext.createUnmarshaller();

        return unmarshaller.unmarshal(node);
    }

    public static Object xml2Obj(Class clazz, Reader xml)  throws JAXBException {
        JAXBContext pContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = pContext.createUnmarshaller();

        return unmarshaller.unmarshal(xml);
    }

    public static Object xml2Obj(Class clazz, String xml)  throws JAXBException {
        StringReader reader = new StringReader(xml);
        return xml2Obj(clazz, reader);
    }

    public static Object xml2Obj(Class clazz, InputStream is)  throws JAXBException {
        JAXBContext pContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = pContext.createUnmarshaller();

        return unmarshaller.unmarshal(is);
    }
}
