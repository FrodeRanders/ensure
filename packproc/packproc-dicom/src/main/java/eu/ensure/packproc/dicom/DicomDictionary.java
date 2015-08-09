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
package eu.ensure.packproc.dicom;

import eu.ensure.commons.lang.Stacktrace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UIDDictionary;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Properties;

/**
 * Description of DicomDictionary:
 * <p>
 * Helper class that maps DicomElement tag to a name (tag2name) and tag UID-values to a description.
 * <p>
 * Created by Frode Randers at 2011-12-16 12:07
 */
public class DicomDictionary {
    private static final Logger log = LogManager.getLogger(DicomDictionary.class);

    private static final String DICOM_TAG_MAPPING_FILE = "dicom-tags.xml";
    //
    private static final UIDDictionary uid2nameDict = UIDDictionary.getDictionary();
    private static final HashMap<String, String> tag2NameDict = new HashMap<String, String>();

    static {
        /* First shot, pull descriptions from dcm4che */
        for (Field field : Tag.class.getFields()) {
            try {
                String name = field.getName();
                int value = field.getInt(/* static field */ null);
                int low = value & 0xFFFF;
                int high = (value & 0xFFFF0000) >> 16;

                String key = String.format("%04x", high) + "," + String.format("%04x", low);
                tag2NameDict.put(key.toUpperCase(), name);

            } catch (IllegalAccessException iae) {
                log.info("Tag field not accessible: " + Stacktrace.asString(iae));
            }
        }

        /* Second shot, pull descriptions from external (configurable) source */
        try {
            InputStream is = null;
            try {
                is = DicomDictionary.class.getResourceAsStream(DICOM_TAG_MAPPING_FILE);
                Properties map = new Properties();
                map.loadFromXML(is);
                log.debug("Adding auxilliary tag mappings from " + DICOM_TAG_MAPPING_FILE);

                for (Object key : map.keySet()) {
                    if (key instanceof String) {
                        if (!tag2NameDict.containsKey(key)) {
                            //log.debug("Adding " + key);
                            tag2NameDict.put(((String)key).toUpperCase(), (String)map.get(key));
                        }
                    }
                }
            } finally {
                if (null != is) is.close();
            }
        } catch (IOException ioe) {
            String info = "Could not load auxilliary DICOM tags mappings file " + DICOM_TAG_MAPPING_FILE;
            info += ": " + ioe.getMessage();
            log.warn(info);
        }
    }

    private DicomDictionary() {}

    public static String uid2name(String uid) {
        return uid2nameDict.nameOf(uid);
    }

    public static boolean tagIsPrivate(int tag) {
        return ((tag>>16)&1)!=0;
    }

    public static boolean tagIsPrivate(String tag) {
        if (tag.length() == 9) { /* len("gggg,eeee") = 9 */
            char c = tag.charAt(3);
            int i = c - '0';
            return (i%2 == 1);
        }
        return false;
    }

    public static String int2Tag(int tag) {
        int low = tag & 0xFFFF;
        int high = (tag & 0xFFFF0000) >> 16;
        return String.format("%04x", high).toUpperCase() + "," + String.format("%04x", low).toUpperCase();
    }

    public static String element2Tag(DicomElement element) {
        return int2Tag(element.tag());
    }

    public static String tag2Name(DicomElement element) {
        String key = element2Tag(element);
        String name = tag2NameDict.get(key);
        return (name == null ? key : name);
    }

    public static String tag2Name(String tag) {
        String name = tag2NameDict.get(tag.toUpperCase());
        return (name == null ? tag : name);
    }

    public static String tag2Name(int tag) {
        String _tag = int2Tag(tag);
        String name = tag2NameDict.get(_tag);
        return (name == null ? _tag : name);
    }
}
