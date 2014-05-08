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

import eu.ensure.packproc.BasicProcessorContext;
import org.apache.log4j.Logger;
import org.dcm4che2.data.SpecificCharacterSet;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of DicomProcessorContext:
 * <p>*
 * Created by Frode Randers at 2011-12-14 00:46
 */
public class DicomProcessorContext extends BasicProcessorContext {
    private static final Logger log = Logger.getLogger(DicomProcessorContext.class);

    private int cachedDepth = -1;

    // Setup specific behaviours, i.e. how to treat specific tags
    public interface BehaviourCallable {
        void call(String tag, String value, DicomProcessorContext context);
    }
    private static final HashMap<String, BehaviourCallable> behaviours = new HashMap<String, BehaviourCallable>();

    static {
        //-------------------------------------------------------------
        // Behaviour: Transfer syntax
        //
        // Example:
        //    (0002,0010) UI #18 [1.2.840.10008.1.2]
        //    -> TransferSyntaxUID := Implicit VR Little Endian
        //-------------------------------------------------------------
        behaviours.put("0002,0010", new BehaviourCallable() {
            public void call(String tag, String value, DicomProcessorContext context)
            {
                if (null != value && value.length() > 0) {
                    if (value.equals("1.2.840.10008.1.2")) { // Implicit VR little endian
                        log.debug("# Transfer syntax: Implicit VR little endian");
                        context.byteOrder = ByteOrder.LITTLE_ENDIAN;
                    }
                    else if (value.equals("1.2.840.10008.1.2.1")) { // Explicit VR little endian
                        log.debug("# Transfer syntax: Explicit VR little endian");
                        context.byteOrder = ByteOrder.LITTLE_ENDIAN;
                    }
                    else if (value.equals("1.2.840.10008.1.2.1.99")) { // Deflated explicit VR little endian
                        log.debug("# Transfer syntax: Deflated explicit VR little endian");
                        context.byteOrder = ByteOrder.LITTLE_ENDIAN;
                    }
                    else if (value.equals("1.2.840.10008.1.2.2")) { // Explict VR big endian
                        log.debug("# Transfer syntax: Explict VR big endian");
                        context.byteOrder = ByteOrder.BIG_ENDIAN;
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.50")) { // JPEG baseline (process 1)
                        log.debug("# Transfer syntax: JPEG baseline (process 1)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.51")) { // JPEG extended (process 2 and 4)
                        log.debug("# Transfer syntax: JPEG extended (process 2 and 4)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.55")) { // JPEG progressive (process 10 and 12)
                        log.debug("# Transfer syntax: JPEG progressive (process 10 and 12)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.57")) { // JPEG lossless (process 14)
                        log.debug("# Transfer syntax: JPEG lossless, nonhierarchical (process 14)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.58")) { // JPEG lossless (process 15)
                        log.debug("# Transfer syntax: JPEG lossless, nonhierarchical (process 14)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.70")) { // JPEG lossless (process 14, selection value 1)
                        log.debug("# Transfer syntax: JPEG lossless, nonhierarchical (process 14, selection value 1)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.80")) { // JPEG-LS lossless
                        log.debug("# Transfer syntax: JPEG-LS lossless");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.81")) { // JPEG-LS lossy (near-lossless)
                        log.debug("# Transfer syntax: JPEG-LS lossy (near-lossless)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.90")) { // JPEG 2000 (lossless only)
                        log.debug("# Transfer syntax: JPEG 2000 (lossless only)");
                    }
                    else if (value.equals("1.2.840.10008.1.2.4.91")) { // JPEG 2000
                        log.debug("# Transfer syntax: JPEG 2000");
                    }
                    else if (value.equals("1.2.840.10008.1.2.5")) { // RLE lossless
                        log.debug("# Transfer syntax: RLE lossless");
                    }
                }
            }
        });

        //-------------------------------------------------------------
        // Behaviour: Specific character set
        //
        // Example:
        //    (0008,0005) CS #10 [ISO_IR 138]
        //    -> SpecificCharacterSet := ISO_IR 138
        //-------------------------------------------------------------
        //
        //
        behaviours.put("0008,0005", new BehaviourCallable() {
            public void call(String tag, String value, DicomProcessorContext context)
            {
                if (null != value && value.length() > 0) {
                    log.debug("# Specific character set: " + value);
                    context.charSet = SpecificCharacterSet.valueOf(new String[]{ value });
                }
                else {
                    log.debug("# Specific character set: <unknown>");
                }
            }
        });
    }

    private ByteOrder byteOrder = ByteOrder.nativeOrder();

    //
    public final SpecificCharacterSet iso8859_1 = new SpecificCharacterSet("ISO8859-1");
    private final HashMap<String, String> collectedValues = new HashMap<String, String>();

    private SpecificCharacterSet charSet = null;

    public DicomProcessorContext(String name, final String[] interestingTags) {
        super(name);
        if (null != interestingTags) {
            for (String key : interestingTags) {
                collectedValues.put(key, null);
            }
        }
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public SpecificCharacterSet getCharacterSet() {
        if (null == charSet)
            return iso8859_1;
        return charSet;
    }

    public void updateState(String tag, String value) {
        // First, check what we are interested in collecting
        if (collectedValues.containsKey(tag)) {
            collectedValues.put(tag, value);

            // Second, check how we want to treat this value - what behaviour does it trigger...
            if (behaviours.containsKey(tag)) {
                if (null != value && value.length() > 0) {
                    behaviours.get(tag).call(tag, value.trim(), this);
                }
            }
        }
    }

    public Map<String, String> getCollectedValues() {
        return collectedValues;
    }

    public int getDepth() {
        if (cachedDepth > -1) {
            return cachedDepth;
        }
        int cachedDepth = super.getDepth(DicomProcessorContext.class.getCanonicalName());
        return cachedDepth;
    }
}

class DicomProcessorSubContext extends DicomProcessorContext {

    private DicomProcessorContext parent = null;

    DicomProcessorSubContext(DicomProcessorContext ctx) {
        super("<subcontext>", /* no interesting tags */ null);
        parent = ctx;
    }

    public String getContextName() {
        return null; // Sub context has no name
    }

    public ByteOrder getByteOrder() {
        return parent.getByteOrder();
    }

    public SpecificCharacterSet getCharacterSet() {
        return parent.getCharacterSet();
    }

    public void updateState(String tag, String value) {
        parent.updateState(tag, value);
    }

    public Map<String, String> getCollectedValues() {
        return parent.getCollectedValues();
    }

    public int getDepth() {
        return parent.getDepth()+1;
    }
}