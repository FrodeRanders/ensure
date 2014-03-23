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

import eu.ensure.commons.lang.Number;
import eu.ensure.commons.lang.Date;
import eu.ensure.packproc.ProcessorException;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomElement;

import java.lang.String;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Description of ValueHandler:
 * <p/>*
 * Created by Frode Randers at 2011-12-16 12:04
 */
public class ValueHandler {
    private static final Logger log = Logger.getLogger(ValueHandler.class);

    private static final int MAX_DEBUG_ARRAY_LENGTH = 256;

    /*
     * MAX or EXACT lengths for various value representations
     */
    public static final int CS_MAX_LENGTH = 16;
    public static final int SH_MAX_LENGTH = 16;
    public static final int LO_MAX_LENGTH = 64;
    public static final int ST_MAX_LENGTH = 1024;
    public static final int LT_MAX_LENGTH = 10240;
    public static final long UT_MAX_LENGTH = Math.round(Math.pow(2,32)) - 2;
    public static final int AE_MAX_LENGTH = 16;
    public static final int PN_MAX_LENGTH = 64;
    public static final int UI_MAX_LENGTH = 64;
    public static final int DA_EXACT_LENGTH = 8;
    public static final int TM_MAX_LENGTH = 16;
    public static final int DT_MAX_LENGTH = 26;
    public static final int AS_EXACT_LENGTH = 4;
    public static final int IS_MAX_LENGTH = 12;
    public static final int DS_MAX_LENGTH = 16;
    public static final int SS_EXACT_LENGTH = 2;
    public static final int US_EXACT_LENGTH = 2;
    public static final int SL_EXACT_LENGTH = 4;
    public static final int UL_EXACT_LENGTH = 4;
    public static final int AT_EXACT_LENGTH = 4;
    public static final int FL_EXACT_LENGTH = 4;
    public static final int FD_EXACT_LENGTH = 8;

    /*-----------------------------------------------------------------------
     * Handlers for the various types of Value Representations
     *
     * String value type: AE, AS, AT, DT, IS, LO, LT, SH, TM, UI, UT
     * String[] value type: CS, PN, ST
     * Date value type: DA
     * float (*) value type: FL
     * double (*) value type: FD
     * double[] value type: DS
     * int (*) value type: SL, US
     * int[] value type: -
     * short (*) value type: SS
     * long (*) value type: UL
     * null: OB, OF, OW, SQ, UN
     *
     * (*) may return null as well
     *----------------------------------------------------------------------*/
    public enum ReturnValueType {
        NULL,
        STRING,
        STRING_ARRAY,
        DATE,
        FLOAT,
        FLOAT_ARRAY,
        DOUBLE,
        DOUBLE_ARRAY,
        INTEGER,
        INTEGER_ARRAY,
        SHORT,
        LONG
    }

    private static class ReturnValue {
        public ReturnValueType type = ReturnValueType.NULL;
        public Object value = null;

        public ReturnValue() {}

        public ReturnValue(String value) {
            this.type = ReturnValueType.STRING;
            this.value = value;
        }

        public ReturnValue(String[] value) {
            this.type = ReturnValueType.STRING_ARRAY;
            this.value = value;
        }

        public ReturnValue(java.util.Date value) {
            this.type = ReturnValueType.DATE;
            this.value = value;
        }

        public ReturnValue(Float value) {
            this.type = ReturnValueType.FLOAT;
            this.value = value;
        }

        public ReturnValue(Float[] value) {
            this.type = ReturnValueType.FLOAT_ARRAY;
            this.value = value;
        }

        public ReturnValue(Double value) {
            this.type = ReturnValueType.DOUBLE;
            this.value = value;
        }

        public ReturnValue(Double[] value) {
            this.type = ReturnValueType.DOUBLE_ARRAY;
            this.value = value;
        }

        public ReturnValue(Integer value) {
            this.type = ReturnValueType.INTEGER;
            this.value = value;
        }

        public ReturnValue(Integer[] value) {
            this.type = ReturnValueType.INTEGER_ARRAY;
            this.value = value;
        }

        public ReturnValue(Short value) {
            this.type = ReturnValueType.SHORT;
            this.value = value;
        }

        public ReturnValue(Long value) {
            this.type = ReturnValueType.LONG;
            this.value = value;
        }

        private ReturnValue(ReturnValueType type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private static final Integer[] TEMPLATE_INTEGER_ARRAY = new Integer[] {};
    private static final Double[] TEMPLATE_DOUBLE_ARRAY = new Double[] {};
    private static final Float[] TEMPLATE_FLOAT_ARRAY = new Float[] {};

    /*-----------------------------------------------------------------------
     * Handlers for the various types of Value Representations
     *----------------------------------------------------------------------*/
    public interface ValueRepresentationCallable {
        ReturnValue call(DicomElement element, DicomProcessorContext context) throws ProcessorException;
    }

    private static final HashMap<String, ValueRepresentationCallable> vrHandlers =
            new HashMap<String, ValueRepresentationCallable>();

    private static boolean validatedMaxElementLength(DicomElement element, long max) {
        int length = element.length();

        // First thing, element length should be even!
        if (length % 2 != 0) {
            log.warn("! Element length " + length + " not an even number: " + getTagInfo(element));
            return false;
        }

        // Second, element length should not exceed max
        if (length > max) {
            log.warn("! Element length " + length + " exceeds max length " + max + ": " + getTagInfo(element));
            return false;
        }

        return true;
    }

    private static boolean validatedExactElementLength(DicomElement element, int exact) {
        int length = element.length();

        // First thing, element length should be even!
        if (length % 2 != 0) {
            log.warn("! Element length " + length + " not an even number: " + getTagInfo(element));
            return false;
        }

        // Second, element length should match demanded length
        if (length != exact) {
            log.warn("! Element length " + length + " is not " + exact + " as required: " + getTagInfo(element));
            return false;
        }

        return true;
    }

    private static boolean validatedMultipleExactElementLength(DicomElement element, int exact) {
        int length = element.length();

        // First thing, element length should be even!
        if (length % 2 != 0) {
            log.warn("! Element length " + length + " not an even number: " + getTagInfo(element));
            return false;
        }

        // Second, element length should match demanded length
        if (length != exact) {
            // Is it a multiple of the demanded length then?
            if (0 == length % exact) {
                return true;
            }
            log.warn("! Element length " + length + " is not " + exact + " or a multiple thereof as required: " + getTagInfo(element));
            return false;
        }

        return true;
    }

    static {
        // [AE] Application entity, Character data [Naming devices, people, and instances]
        vrHandlers.put("AE", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedMaxElementLength(element, AE_MAX_LENGTH)) {
                    String s = context.getCharacterSet().decode(element.getBytes()).trim();
                    context.updateState(DicomDictionary.element2Tag(element), s);
                    return new ReturnValue(s.trim());
                }
                return new ReturnValue();
            }
        });

        // [AS] Age string, Character data [Date and time]
        // Format: nnnW or nnnM or nnnY
        vrHandlers.put("AS", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, AS_EXACT_LENGTH)) {
                    String s = context.getCharacterSet().decode(element.getBytes()).trim().toUpperCase();

                    context.updateState(DicomDictionary.element2Tag(element), s);
                    return new ReturnValue(s);
                }
                return new ReturnValue();
            }
        });

        // [AT] Attribute tag, Two 2-byte integers [Numbers in binary format]
        // Format: gggg,eeee
        vrHandlers.put("AT", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, AT_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(AT_EXACT_LENGTH);
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    try
                    {
                        int v = byteBuf.getInt();
                        String tag = DicomDictionary.int2Tag(v);
                        context.updateState(DicomDictionary.element2Tag(element), tag);
                        return new ReturnValue(tag);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "Insufficient number of bytes to read into an 'integer': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                }
                return new ReturnValue();
            }
        });

        // [CS] Code string, Character data, Max length: 16
        // Note: Only upper-case letters, 0-9, ' ' and '_' allowed
        vrHandlers.put("CS", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, CS_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                //
                final String re = "([A-Z0-9_\\ \\\\])*";
                if (!s.matches(re)) {
                    log.warn("! Value of CS element " + getTagInfo(element) + " contains illegal letter(s): " + s);
                    return new ReturnValue();
                }

                // This could be a (0008,0005) SpecificCharacterSet (say with ISO_IR 138)
                context.updateState(DicomDictionary.element2Tag(element), s);

                if (!element.vr().isSingleValue(s)) {
                    String[] parts = s.split("\\\\");
                    return new ReturnValue(parts);
                }
                else {
                    return new ReturnValue(new String[] { s });
                }
            }
        });

        // [DA] Date, Eight characters [Date and time]
        // Format: yyyymmdd (check for yyyy.mm.dd also and convert)
        vrHandlers.put("DA", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, DA_EXACT_LENGTH)) {
                    java.util.Date date = element.getDate(false);
                    return new ReturnValue(date);
                }
                return new ReturnValue();
            }
        });

        // [DS] Decimal string, Character data [Numbers in text format]
        // NOTE: may start with + or - and may be padded with l or t space
        vrHandlers.put("DS", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, DS_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                try {
                    if (!element.vr().isSingleValue(s)) {
                        String[] parts = s.split("\\\\");

                        List<Double> list = new LinkedList<Double>();
                        for (String part : parts) {
                            list.add(Double.parseDouble(part));
                        }
                        return new ReturnValue(list.toArray(TEMPLATE_DOUBLE_ARRAY));
                    }
                    else {
                        return new ReturnValue(new Double[] { Double.parseDouble(s) });
                    }
                }
                catch (NumberFormatException ignore) {
                    String info = "! Incorrect number format does not match value representation DS for ";
                    info += DicomDictionary.element2Tag(element);
                    log.warn(info);
                }
                return new ReturnValue();
             }
        });

        // [DT] Date time, Character data [Date and time]
        // Format: YYYYMMDDHHMMSS.FFFFFF&ZZZZ (&ZZZ is optional & = + or -)
        vrHandlers.put("DT", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedMaxElementLength(element, DT_MAX_LENGTH)) {
                    String s = context.getCharacterSet().decode(element.getBytes()).trim();
                    context.updateState(DicomDictionary.element2Tag(element), s);

                    return new ReturnValue(s.trim());
                }
                return new ReturnValue();
            }
        });

        // [FL] Floating point single, 4-byte floating point [Numbers in binary format]
        // Single precision floating point number (float)
        vrHandlers.put("FL", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedMultipleExactElementLength(element, FL_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(element.length());
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    try
                    {
                        if (element.length() > FL_EXACT_LENGTH) {
                            List<Float> ary = new Vector<Float>();
                            while (byteBuf.hasRemaining()) {
                                float v = byteBuf.getFloat();
                                ary.add(v);
                            }
                            return new ReturnValue(ary.toArray(TEMPLATE_FLOAT_ARRAY));
                        } else {
                            float v = byteBuf.getFloat();
                            return new ReturnValue(v);
                        }
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into a 'float': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                }
                return new ReturnValue();
            }
        });

        // [FD] Floating point double, 8-byte floating point [Numbers in binary format]
        // Double precision floating point number (double)
        vrHandlers.put("FD", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, FD_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(FD_EXACT_LENGTH);
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    try
                    {
                        double v = byteBuf.getDouble();
                        return new ReturnValue(v);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into a 'double': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                }
                return new ReturnValue();
            }
        });

        // [IS] Integer string, Character data [Numbers in text format]
        // Integer encoded as string. May be padded
        vrHandlers.put("IS", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, IS_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();
                return new ReturnValue(s.trim());
            }
        });

        // [LO] Long string, Character data, Max length: 64 [Text]
        // Character string. Can be padded.
        // NOTE: May not contain \ or any control chars except ESC
        vrHandlers.put("LO", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, LO_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                context.updateState(DicomDictionary.element2Tag(element), s);

                return new ReturnValue(s.trim());
            }
        });

        // [LT] Long text, Character data, Max length: 10,240 [Text]
        // NOTE: Leading spaces are significant, trailing spaces are not
        vrHandlers.put("LT", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, LT_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();
                return new ReturnValue(s.trim());
            }
        });

        // [OB] Other byte string, 1-byte integers [Numbers in binary format]
        // NOTE: Has single trailing 0x00 to make even number of bytes. Transfer Syntax determines length
        vrHandlers.put("OB", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                // TODO - String of bytes
                if (element.length() > 0) {
                    if (element.length() <= MAX_DEBUG_ARRAY_LENGTH) {
                        String s = context.getCharacterSet().decode(element.getBytes());
                        return new ReturnValue(s.trim());
                    } else {
                        String s = Number.asHumanApproximate(element.length()) + " of binary data";
                        return new ReturnValue(s.trim());
                    }
                }
                return new ReturnValue();
            }
        });

        // [OF] Other float string, 4-byte floating point [Numbers in binary format]
        vrHandlers.put("OF", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                // TODO - String of 4-byte floating point numbers
                if (element.length() == 4) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(element.length());
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    try
                    {
                        float v = byteBuf.getFloat();
                        return new ReturnValue(v);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into a 'float': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                } else {
                    log.warn("! Incorrect number of bytes " + element.length() + " does not match value representation OF");
                }
                return new ReturnValue();
            }
        });

        // [OW] Other word string, 2-byte integers [Numbers in binary format]
        // Max length: -
        vrHandlers.put("OW", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)  throws ProcessorException
            {
                // TODO - String of 2-byte integers
                if (element.length() > 0) {
                    String s = Number.asHumanApproximate(2L*element.length()) + " of binary data";
                    return new ReturnValue(s);

                    // This will read the whole byte stream into a ByteBuffer - good or bad
                    /*
                    ByteBuffer byteBuf = ByteBuffer.allocate(Math.max(0, element.length()));
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    */
                    //ByteBuffer byteBuf = ByteBuffer.allocate(element.length());
                    //byteBuf.order(context.getByteOrder());
                    //ShortBuffer shortBuf = byteBuf.asShortBuffer(); // view buffer onto byte buffer
                    //shortBuf.put(element.getShorts(/* do cache? */ false));
                    //shortBuf.flip();

                    /*
                    if (shortBuf.order() == ByteOrder.BIG_ENDIAN) {
                        log.debug("Short is big endian");
                    } else if (shortBuf.order() == ByteOrder.LITTLE_ENDIAN) {
                        log.debug("Short is little endian");
                    }
                    */

                    //File tempFile = FileTool.writeToTempFile(byteBuf, /* prefix */ DicomDictionary.tag2Name(element));
                    //log.debug("Bytes written to " + tempFile.getAbsolutePath());
                }
                return new ReturnValue();
            }
        });

        // [PN] Person name, Character data [Naming devices, people, and instances]
        // NOTE: 64 byte max per component, 5 components with delimiter = ^
        vrHandlers.put("PN", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, PN_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                String[] parts = s.split("^");
                return new ReturnValue(parts);
            }
        });

        // [SH] Short string, Character data, Max length: 16 [Text]
        // NOTE: may be padded
        vrHandlers.put("SH", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, SH_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                context.updateState(DicomDictionary.element2Tag(element), s);

                return new ReturnValue(s.trim());
            }
        });

        // [SL] Signed long, 4-byte integer [Numbers in binary format]
        vrHandlers.put("SL", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, SL_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(SL_EXACT_LENGTH);
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    try
                    {
                        int v = byteBuf.getInt();
                        return new ReturnValue(v);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into an 'integer': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                }
                return new ReturnValue();
            }
        });

        // [SQ] Sequence of zero or more items
        // We are not likely to get a call to this handler, since we intercept SQ elements
        // in DicomProcessor.
        vrHandlers.put("SQ", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                // === HAS NO VALUE! ===
                return new ReturnValue();
            }
        });

        // [SS] Signed short, 2-byte integer [Numbers in binary format]
        vrHandlers.put("SS", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, SS_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(SS_EXACT_LENGTH);
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.flip();
                    try
                    {
                        short v = byteBuf.getShort();
                        return new ReturnValue(v);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into a 'short': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                }
                return new ReturnValue();
            }
        });

        // [ST] Short text, Character data, Max length: 1024 [Text]
        vrHandlers.put("ST", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, ST_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                context.updateState(DicomDictionary.element2Tag(element), s);

                if (null != s && !element.vr().isSingleValue(s)) {
                    String[] parts = s.split("\\\\");
                    return new ReturnValue(parts);
                }
                else {
                    return new ReturnValue(new String[] { s });
                }
            }
        });

        // [TM] Time [Date and time]
        // Format: hhmmss.frac (or older format: hh:mm:ss.frac)
        vrHandlers.put("TM", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedMaxElementLength(element, TM_MAX_LENGTH)) {
                    String s = context.getCharacterSet().decode(element.getBytes()).trim();

                    context.updateState(DicomDictionary.element2Tag(element), s);

                    if (s.length() >= 6) {
                        String tm = s.substring(0,2) + ":" + s.substring(2,4) + ":" + s.substring(4,6);
                        if (s.length() > 6) {
                            tm += s.substring(6);
                        }
                        return new ReturnValue(tm);
                    }
                    else {
                        return new ReturnValue(s);
                    }
                }
                return new ReturnValue();
            }
        });

        // [UI] Unique identifier, Character data [Naming devices, people, and instances]
        // Format: delimiter = ., 0-9 characters only, trailing space to make even number
        vrHandlers.put("UI", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                validatedMaxElementLength(element, UI_MAX_LENGTH); // but continue even if wrong
                String s = context.getCharacterSet().decode(element.getBytes()).trim();

                // UIDs are special, we may want to remember these
                context.updateState(DicomDictionary.element2Tag(element), s);

                return new ReturnValue(s);
            }
        });

        // [UL] Unsigned long, 4-byte integer [Numbers in binary format]
        vrHandlers.put("UL", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, UL_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(2 * UL_EXACT_LENGTH);
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.put(new byte[] {0, 0, 0, 0}); // empty MSB (since Java is Big Endian)
                    byteBuf.flip();
                    try
                    {
                        // Since we want to access an _unsigned_ int, we really have to
                        // treat this as a long in Java.
                        long v = byteBuf.getLong();
                        return new ReturnValue(v);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into a 'long': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                }
                return new ReturnValue();
            }
        });

        // [UN] Unknown
        vrHandlers.put("UN", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                // String s = context.getCharacterSet().decode(element.getBytes());
                return new ReturnValue();
            }
        });

        // [US] Unsigned short, 2-byte integer [Numbers in binary format]
        vrHandlers.put("US", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedExactElementLength(element, US_EXACT_LENGTH)) {
                    ByteBuffer byteBuf = ByteBuffer.allocate(2 * US_EXACT_LENGTH);
                    byteBuf.order(context.getByteOrder());
                    byteBuf.put(element.getBytes());
                    byteBuf.put(new byte[] {0, 0}); // empty MSB (since Java is Big Endian)
                    byteBuf.flip();
                    try
                    {
                        // Since we want to access an _unsigned_ short, we really have to
                        // treat this as an integer in Java.
                        int v = byteBuf.getInt();
                        return new ReturnValue(v);
                    }
                    catch (BufferUnderflowException bufe) {
                        String info = "! Insufficient number of bytes to read into an 'integer': ";
                        info += bufe.getClass().getName();
                        log.info(info);
                    }
                } else {
                    log.warn("! Incorrect number of bytes " + element.length() + " does not match value representation US");
                }
                return new ReturnValue();
            }
        });

        // [UT] Unlimited text, Character data, Max length: 4,294,967,294 [Text]
        // NOTE: Trailing spaces ignored
        vrHandlers.put("UT", new ValueRepresentationCallable() {
            public ReturnValue call(DicomElement element, DicomProcessorContext context)
            {
                if (validatedMaxElementLength(element, UT_MAX_LENGTH)) {
                    String s = context.getCharacterSet().decode(element.getBytes()).trim();
                    return new ReturnValue(s);
                }
                return new ReturnValue();
            }
        });
    }

    /*--------------------------------------------------------------------------
     * Handlers for the various Object types that we get from the various VRs
     * which converts values to String.
     *-------------------------------------------------------------------------*/
    public interface ValueCallable<V,R> {
        R call(V value, DicomElement element, DicomProcessorContext context) throws ProcessorException;
    }

    //
    public interface ValueTypeCallable<V,R> {
         R call(ReturnValue value, ValueCallable<V,R> callable, DicomElement element, DicomProcessorContext context) throws ProcessorException;
    }

    private static final HashMap<String, ValueTypeCallable> valueType2StringHandlers =
            new HashMap<String, ValueTypeCallable>();

    static {
        // String value type: AE, AS, AT (gggg,vvvv), DT, IS, LO, LT, SH, TM, UI, UT
        valueType2StringHandlers.put(ReturnValueType.STRING.name(), new ValueTypeCallable<String, String>() {
            public String call(ReturnValue value, ValueCallable<String, String> callable, DicomElement element, DicomProcessorContext context)
            {
                try {
                    if (value.value instanceof String) {
                        return callable.call((String) value.value, element, context);
                    }
                    else {
                        String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                        log.warn(info);
                        throw new RuntimeException(info);
                    }
                }
                catch (Exception e) {
                    String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                    info += e.getMessage();
                    log.warn(info);
                }
                return null;
            }
        });

       // String[] value type: CS, PN, ST
       valueType2StringHandlers.put(ReturnValueType.STRING_ARRAY.name(), new ValueTypeCallable<String[], String>() {
           public String call(ReturnValue value, ValueCallable<String[], String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof String[]) {
                        return callable.call((String[]) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // Date value type: DA
       valueType2StringHandlers.put(ReturnValueType.DATE.name(), new ValueTypeCallable<java.util.Date, String>() {
           public String call(ReturnValue value, ValueCallable<java.util.Date, String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof java.util.Date) {
                        return callable.call((java.util.Date) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // float (*) value type: FL
       valueType2StringHandlers.put(ReturnValueType.FLOAT.name(), new ValueTypeCallable<Float, String>() {
           public String call(ReturnValue value, ValueCallable<Float, String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Float) {
                        return callable.call((Float) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // float[] (*) value type: FL
       valueType2StringHandlers.put(ReturnValueType.FLOAT_ARRAY.name(), new ValueTypeCallable<Float[], String>() {
           public String call(ReturnValue value, ValueCallable<Float[], String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Float[]) {
                        return callable.call((Float[]) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // double (*) value type: FD
       valueType2StringHandlers.put(ReturnValueType.DOUBLE.name(), new ValueTypeCallable<Double, String>() {
           public String call(ReturnValue value, ValueCallable<Double, String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Double) {
                        return callable.call((Double) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // double[] (*) value type: DS
       valueType2StringHandlers.put(ReturnValueType.DOUBLE_ARRAY.name(), new ValueTypeCallable<Double[], String>() {
           public String call(ReturnValue value, ValueCallable<Double[], String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Double[]) {
                        return callable.call((Double[]) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // int (*) value type: SL, US
       valueType2StringHandlers.put(ReturnValueType.INTEGER.name(), new ValueTypeCallable<Integer, String>() {
           public String call(ReturnValue value, ValueCallable<Integer, String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Integer) {
                       return callable.call((Integer) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // Integer[] value type: <none>
       valueType2StringHandlers.put(ReturnValueType.INTEGER_ARRAY.name(), new ValueTypeCallable<Integer[], String>() {
           public String call(ReturnValue value, ValueCallable<Integer[], String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Integer[]) {
                       return callable.call((Integer[]) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // short (*) value type: SS
       valueType2StringHandlers.put(ReturnValueType.SHORT.name(), new ValueTypeCallable<Short, String>() {
           public String call(ReturnValue value, ValueCallable<Short, String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Short) {
                       return callable.call((Short) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });

       // long (*) value type: UL
       valueType2StringHandlers.put(ReturnValueType.LONG.name(), new ValueTypeCallable<Long, String>() {
           public String call(ReturnValue value, ValueCallable<Long, String> callable, DicomElement element, DicomProcessorContext context)
           {
               try {
                   if (value.value instanceof Long) {
                        return callable.call((Long) value.value, element, context);
                   }
                   else {
                       String info = "Impertinent ValueCallable<> for tag " + DicomDictionary.element2Tag(element);
                       log.warn(info);
                       throw new RuntimeException(info);
                   }
               }
               catch (Exception e) {
                   String info = "Failed to evaluate " + DicomDictionary.element2Tag(element);
                   info += e.getMessage();
                   log.warn(info);
               }
               return null;
           }
       });
    }

    /*-----------------------------------------------------------------------
     * Debuggers for the various return values
     *----------------------------------------------------------------------*/

    public static String getTagInfo(DicomElement element) {
        String tag = DicomDictionary.element2Tag(element);

        StringBuilder sb = new StringBuilder();
        sb.append(tag).append(DicomDictionary.tagIsPrivate(tag) ? "p" : " ")
          .append(" (").append(DicomDictionary.tag2Name(element)).append(") ")
          .append(element.vr().toString())
          .append(" [").append(element.length()).append("]").append(": ");
        return sb.toString();
    }

    public static String getTagInfo(String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(tag).append(DicomDictionary.tagIsPrivate(tag) ? "p" : " ")
          .append(" (").append(DicomDictionary.tag2Name(tag)).append(") ");
        sb.append(": ");
        return sb.toString();
    }

    public static String indentDepth(DicomProcessorContext context) {
        StringBuilder sb = new StringBuilder();
        int max = context.getDepth();
        for (int i=1; i<max; i++) {
            sb.append(">");
        }
        if (max > 1) {
            sb.append(" ");
        }
        return sb.toString();
    }

    private static final HashMap<String, ValueCallable> value2StringConverters = new HashMap<String, ValueCallable>();

    static {
        // String
        value2StringConverters.put(ReturnValueType.STRING.name(), new ValueCallable<String, String>() {
            public String call(String value, DicomElement element, DicomProcessorContext context) {
                return value;
            }
        });

        // String[]
        value2StringConverters.put(ReturnValueType.STRING_ARRAY.name(), new ValueCallable<String[], String>() {
            public String call(String[] value, DicomElement element, DicomProcessorContext context) {
                String s = "";
                for (int i=0; i<value.length; i++) {
                    s += value[i];
                    if (i < value.length - 1) {
                        s += ", ";
                    }
                }
                return s;
            }
        });

        // Date
        value2StringConverters.put(ReturnValueType.DATE.name(), new ValueCallable<java.util.Date, String>() {
            public String call(java.util.Date value, DicomElement element, DicomProcessorContext context) {
                return Date.date2String(value, Locale.getDefault());
            }
        });

        // Float
        value2StringConverters.put(ReturnValueType.FLOAT.name(), new ValueCallable<Float, String>() {
            public String call(Float value, DicomElement element, DicomProcessorContext context) {
                return "" + value;
            }
        });

        // Float[]
        value2StringConverters.put(ReturnValueType.FLOAT_ARRAY.name(), new ValueCallable<Float[], String>() {
            public String call(Float[] value, DicomElement element, DicomProcessorContext context) {
                String s = "";
                for (int i=0; i<value.length; i++) {
                    s += value[i];
                    if (i < value.length - 1) {
                        s += ", ";
                    }
                }
                return s;
            }
        });

        // Double
        value2StringConverters.put(ReturnValueType.DOUBLE.name(), new ValueCallable<Double, String>() {
            public String call(Double value, DicomElement element, DicomProcessorContext context) {
                return "" + value;
            }
        });

        // Double[]
        value2StringConverters.put(ReturnValueType.DOUBLE_ARRAY.name(), new ValueCallable<Double[], String>() {
            public String call(Double[] value, DicomElement element, DicomProcessorContext context) {
                String s = "";
                for (int i=0; i<value.length; i++) {
                    s += value[i];
                    if (i < value.length - 1) {
                        s += ", ";
                    }
                }
                return s;
            }
        });

        // Integer
        value2StringConverters.put(ReturnValueType.INTEGER.name(), new ValueCallable<Integer, String>() {
            public String call(Integer value, DicomElement element, DicomProcessorContext context) {
                return "" + value;
            }
        });

        // Integer[]
        value2StringConverters.put(ReturnValueType.INTEGER_ARRAY.name(), new ValueCallable<Integer[], String>() {
            public String call(Integer[] value, DicomElement element, DicomProcessorContext context) {
                String s = "";
                for (int i=0; i<value.length; i++) {
                    s += value[i];
                    if (i < value.length - 1) {
                        s += ", ";
                    }
                }
                return s;
            }
        });

        // Short
        value2StringConverters.put(ReturnValueType.SHORT.name(), new ValueCallable<Short, String>() {
            public String call(Short value, DicomElement element, DicomProcessorContext context) {
                return "" + value;
            }
        });

        // Long
        value2StringConverters.put(ReturnValueType.LONG.name(), new ValueCallable<Long, String>() {
            public String call(Long value, DicomElement element, DicomProcessorContext context) {
                return "" + value;
            }
        });
    }


    public static ReturnValue process(DicomElement element, DicomProcessorContext context) throws ProcessorException
    {
        ReturnValue value = null;

        String vr = element.vr().toString().toUpperCase();
        if (vrHandlers.containsKey(vr)) {
            // Call a handler for this value representation (VR)
            ValueRepresentationCallable vrc = vrHandlers.get(vr);
            value = vrc.call(element, context);

            if (log.isDebugEnabled()) {
                // Call a handler for the return value as well
                if (ReturnValueType.NULL != value.type) {
                    String valueTypeName = value.type.name();
                    if (valueType2StringHandlers.containsKey(valueTypeName)
                        && value2StringConverters.containsKey(valueTypeName)) {

                        ValueTypeCallable vtc = valueType2StringHandlers.get(valueTypeName);
                        ValueCallable debugger = value2StringConverters.get(valueTypeName);

                        String info = indentDepth(context);
                        info += getTagInfo(element);
                        info += vtc.call(value, debugger, element, context);

                        log.debug(info);
                    }
                }
                else {
                    String info = indentDepth(context);
                    info += getTagInfo(element);
                    info += "<null value>";
                    log.debug(info);
                }
            }
        }
        else {
            String info = indentDepth(context);
            info += getTagInfo(element);
            info += "<unknown value representation!>";
            log.warn(info);
            throw new ProcessorException(info);
        }
        return value;
    }
}
