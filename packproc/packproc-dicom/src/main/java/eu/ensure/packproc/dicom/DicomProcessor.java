/*
 * Copyright (C) 2016 Frode Randers
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
 */
package eu.ensure.packproc.dicom;

import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.internal.Action;
import eu.ensure.packproc.internal.BasicFileProcessor;
import eu.ensure.packproc.model.*;
import org.apache.axiom.om.OMElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.TagUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Operates on DICOM (container) streams
 */
public class DicomProcessor extends BasicFileProcessor {
    private static final Logger log = LogManager.getLogger(DicomProcessor.class);

    public DicomProcessor() {
        alias = "dicom-processor";
    }

    // Defines tags that we are especially interested in collecting
    public final String[] interestingTags = {
            // Tag.TransferSyntaxUID = 131088 = 0x20010
            "0002,0010", // Transfer Syntax UID := Implicit VR Little Endian (1.2.840.10008.1.2)
            "0008,0005", // Specific CharacterSet := ISO_IR 138

            // DICOM Information Hierarchy
            "0010,0020", // Patient ID (Patient level)
            "0020,000D", // Study Instance UID (Study level)
            "0020,000E", // Series Instance UID (Series level)
            "0008,0018", // SOP Instance UID (Image level)

            // Derivation information
            "0008,2111"  // Derivation Description
    };

    private FileProcessorUsingChannelsCallable getCallable() {
        return new FileProcessorUsingChannelsCallable() {
            public void call(ReadableByteChannel inputChannel, WritableByteChannel outputChannel, FileProcessor p, ProcessorContext context) throws Exception {

                final String name = context.getContextName();
                DicomProcessorContext dicomContext = context.push(new DicomProcessorContext(name, interestingTags));
                final boolean isMutableCall = null != outputChannel;

                DicomInputStream dicomInputStream = new DicomInputStream(Channels.newInputStream(inputChannel));
                try {
                    //DicomOutputStream dicomOutputStream = null;
                    if (isMutableCall) {
                        log.warn(me() + " does not support mutable output");

                        //final String tsuid = dicomInputStream.getTransferSyntax();
                        //dicomOutputStream = new DicomOutputStream(Channels.newOutputStream(outputChannel), tsuid);
                    }


                    Attributes fmi = dicomInputStream.readFileMetaInformation();
                    Attributes ds = dicomInputStream.readDataset(-1, -1);

                    if (null == configElement) {
                        String info = "Cannot process Dicom-file - no configuration";
                        log.warn(info);
                        throw new ProcessorException(info);
                    }

                    // Retrieve XPath expressions from the configuration
                    for (Iterator<OMElement> ei = configElement.getChildElements(); ei.hasNext(); ) {
                        OMElement element = ei.next();
                        String operation = element.getLocalName(); // Ignore namespace!!!

                        if ("extractInformation".equalsIgnoreCase(operation)) {
                            extractInformation(ds, dicomContext);

                        } else if ("dump".equalsIgnoreCase(operation)) {
                            dump(ds, dicomContext);

                        } else {
                            throw new ProcessorException("Unknown processor operation: " + operation);
                        }
                    }


                    // In case we have been mutating the document, dump it to the output
                    if (null != outputChannel) {
                        //Writer writer = Channels.newWriter(outputChannel, "utf-8");
                        //document.serialize(writer);
                    }
                } finally {

                    Map<String, String> collectedValues = dicomContext.getCollectedValues();
                    List<String> toRemove = new Vector<String>();
                    for (String key : collectedValues.keySet()) {
                        if (null == collectedValues.get(key)) {
                            toRemove.add(key);
                        }
                    }
                    for (String key : toRemove) {
                        collectedValues.remove(key);
                    }

                    if (!collectedValues.isEmpty()) {
                        // Create a package-relative path...
                        File top = new File("/");
                        File contentStream = top; // starting point relative to top

                        // ...and reassemble
                        int start = name.startsWith("/") ? 0 : 1; /* skip [example1]/content/... */

                        String[] parts = name.split("/");
                        for (int i = start; i < parts.length; i++) {
                            contentStream = new File(contentStream, parts[i]);
                        }

                        String path = contentStream.getPath().replace("\\", "/");  // in case we're on Windoze
                        context.associate("DICOM", path, path, collectedValues);
                    }

                    context.pop();
                }

                // Don't close the input channel!
            }
        };
    }


    /**
     * Processes an individual Dicom file.
     * <p>
     *
     * @param inputStream
     * @param outputStream
     * @throws IOException
     * @throws ProcessorException
     * @throws ClassNotFoundException
     */
    public void process(
            InputStream inputStream,
            OutputStream outputStream,
            ProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {

        log.info(me() + ":process dicom-file");
        FileProcessorUsingChannelsCallable callable = getCallable();
        process(inputStream, outputStream, callable, this, context);
    }

    /**
     * Processes a file _entry_ in a structure.
     * <p>
     *
     * @param entry
     * @param entryInputStream
     * @param structureOutputStream
     * @throws IOException
     * @throws ProcessorException
     */
    public void process(
            final StructureEntry entry,
            final InputStream entryInputStream,
            final StructureOutputStream structureOutputStream,
            ProcessorContext context
    ) throws IOException, ProcessorException {

        log.info(me() + ":process dicom-file");
        FileProcessorUsingChannelsCallable callable = getCallable();
        process(entry, entryInputStream, structureOutputStream, callable, this, context);
    }


    /**
     * Dumps information from an entry in DICOM file
     * <p>
     *
     * @throws IOException
     * @throws eu.ensure.packproc.ProcessorException
     */
    public void extractInformation(
            final Attributes dataset,
            DicomProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {

        collect(dataset, context, /* level */ 0, /* index */ 0, /* collect? */ true);
    }

    /**
     * Dumps information from an entry in DICOM file
     * <p>
     *
     * @throws IOException
     * @throws eu.ensure.packproc.ProcessorException
     */
    public void dump(
            final Attributes dataset,
            DicomProcessorContext context
    ) throws IOException, ProcessorException, ClassNotFoundException {

        collect(dataset, context, /* level */ 0, /* index */ 0, /* collect? */ false);
    }

    /**
     *
     */
    private String compose(String description, Object actualValue, boolean verbose) {
        return (verbose ? "{" + description + "} " + actualValue : "" + actualValue);
    }


    /**
     *
     */
    private void collect(
            final Attributes dataset,
            DicomProcessorContext context,
            int currentLevel,
            int currentIndex,
            boolean doCollect
    ) throws IOException, ProcessorException, ClassNotFoundException {

        SpecificCharacterSet characterSet = dataset.getSpecificCharacterSet();
        ElementDictionary dict = ElementDictionary.getStandardElementDictionary();
        boolean isBE = dataset.bigEndian();

        try {
            dataset.accept(new Attributes.Visitor() {
                public boolean visit(Attributes attributes, int tag, VR vr, Object _value) throws Exception {

                    boolean isNull = (_value instanceof Value && _value == Value.NULL);

                    String value;
                    try {
                        switch (vr) {
                            /****************
                             * String
                             ****************/
                            case AE: // Application Entity
                                // Character data [Naming devices, people, and instances]
                                // StringValueType.ASCII
                                Object strings =
                                        value = compose("Application entity", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case AS: // Age string
                                // Character data [Date and time]
                                // Format: nnnW or nnnM or nnnY
                                // StringValueType.ASCII
                                value = compose("Age string", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case AT: // Attribute tag
                                // Two 2-byte integers [Numbers in binary format]
                                // Format: gggg,eeee
                                // BinaryValueType.TAG
                                value = compose("Attribute tag", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case DT: // Date time
                                // Character data [Date and time]
                                // Format: YYYYMMDDHHMMSS.FFFFFF&ZZZZ (&ZZZ is optional & = + or -)
                                // StringValueType.DT
                                value = compose("Date and time", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case IS: // Integer string
                                // Character data [Numbers in text format]
                                // Integer encoded as string. May be padded
                                // StringValueType.IS
                                value = compose("Integer string", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case LO: // Long string
                                // Character data, Max length: 64 [Text]
                                // Character string. Can be padded.
                                // NOTE: May not contain \ or any control chars except ESC
                                // StringValueType.STRING
                                value = "";
                                {
                                    Object o = vr.toStrings(_value, isBE, characterSet);
                                    if (isNull) {
                                        value += "<null>";
                                    } else if (o instanceof String[]) {
                                        for (String s : (String[]) o) {
                                            value += s + ", ";
                                        }
                                    } else {
                                        value += (String) o;
                                    }
                                }
                                value = compose("Long string", value, !doCollect);
                                break;

                            case LT: // Long text
                                // Character data, Max length: 10,240 [Text]
                                // NOTE: Leading spaces are significant, trailing spaces are not
                                // StringValueType.TEXT
                                value = compose("Long text", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case SH: // Short string
                                // Character data, Max length: 16 [Text]
                                // NOTE: may be padded
                                // StringValueType.STRING
                                value = compose("Short string", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case TM: // Time
                                // Format: hhmmss.frac (or older format: hh:mm:ss.frac)
                                // StringValueType.TM
                                value = compose("Time", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case UI: // Unique identifier (UID)
                                // Character data [Naming devices, people, and instances]
                                // Format: delimiter = ., 0-9 characters only, trailing space to make even number
                                // StringValueType.ASCII
                                value = compose("Unique identifier", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case UT: // Unlimited text
                                // Character data, Max length: 4,294,967,294 [Text]
                                // NOTE: Trailing spaces ignored
                                // StringValueType.TEXT
                                value = compose("Unlimited text", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case UC: // Unlimited characters
                                // StringValueType.STRING
                                value = compose("Unlimited characters", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case UR: // URI or URL
                                // StringValueType.UR
                                value = compose("URI/URL", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;


                            /****************
                             * String[]
                             ****************/
                            case CS: // Code string
                                // Character data, Max length: 16
                                // Note: Only upper-case letters, 0-9, ' ' and '_' allowed
                                // StringValueType.ASCII
                                value = "";
                                {
                                    Object o = vr.toStrings(_value, isBE, characterSet);
                                    if (isNull) {
                                        value += "<null>";
                                    } else if (o instanceof String[]) {
                                        for (String s : (String[]) o) {
                                            value += s + ", ";
                                        }
                                    } else {
                                        value += (String) o;
                                    }
                                }
                                value = compose("Code string", value, !doCollect);
                                break;

                            case PN: // Person name
                                // Character data [Naming devices, people, and instances]
                                // NOTE: 64 byte max per component, 5 components with delimiter = ^
                                // StringValueType.PN
                                value = compose("Person name", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case ST: // Short text
                                // Character data, Max length: 1024 [Text]
                                // StringValueType.TEXT
                                value = compose("Short text", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * Date
                             ****************/
                            case DA: // Date
                                // Eight characters [Date and time]
                                // Format: yyyymmdd (check for yyyy.mm.dd also and convert)
                                // StringValueType.DA
                                value = compose("Date", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * double[]
                             ****************/
                            case DS: // Decimal string
                                // Character data [Numbers in text format]
                                // NOTE: may start with + or - and may be padded with l or t space
                                // StringValueType.DS
                                value = "";
                                {
                                    Object o = vr.toStrings(_value, isBE, characterSet);
                                    if (isNull) {
                                        value += "<null>";
                                    } else if (o instanceof String[]) {
                                        for (String s : (String[]) o) {
                                            value += s + ", ";
                                        }
                                    } else {
                                        value += (String) o;
                                    }
                                }
                                value = compose("Decimal string", value, !doCollect);
                                break;

                            /****************
                             * double
                             ****************/
                            case FD: // Floating point double
                                // 8-byte floating point [Numbers in binary format]
                                // Double precision floating point number (double)
                                // BinaryValeuType.DOUBLE
                                value = compose("Floating point double", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * float
                             ****************/
                            case FL: // Floating point single
                                // 4-byte floating point [Numbers in binary format]
                                // Single precision floating point number (float)
                                // BinaryValueType.FLOAT
                                value = compose("Floating point single", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * int
                             ****************/
                            case SL: // Signed long
                                // 4-byte integer [Numbers in binary format]
                                // BinaryValueType.INT
                                value = compose("Signed long", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case US: // Unsigned short
                                // 2-byte integer [Numbers in binary format]
                                // BinaryValueType.USHORT
                                value = compose("Unsigned short", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * short
                             ****************/
                            case SS: // Signed short
                                // 2-byte integer [Numbers in binary format]
                                // BinaryValueType.SHORT
                                value = compose("Signed short", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * long
                             ****************/
                            case UL: // Unsigned long
                                // 4-byte integer [Numbers in binary format]
                                // BinaryValueType.INT
                                value = compose("Unsigned long", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            /****************
                             * NO VALUE!
                             ****************/
                            case OB: // Other byte string
                                // 1-byte integers [Numbers in binary format]
                                // NOTE: Has single trailing 0x00 to make even number of bytes. Transfer Syntax determines length
                                // BinaryValueType.BYTE
                                value = compose("Other byte", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case OD: // Other double string
                                // BinaryValyeType.DOUBLE
                                value = compose("Other byte", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case OF: // Other float string
                                // 4-byte floating point [Numbers in binary format]
                                // BinaryValueType.FLOAT
                                value = compose("Other float", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            //case OL: // Other long string
                            // BinaryValueType.INT
                            //    value = "Other long} " + (isNull ? "<null>" : _value)
                            //     break;

                            case OW: // Other word string
                                // 2-byte integers [Numbers in binary format]
                                // Max length: -
                                // BinaryValueType.SHORT
                                value = compose("Other word", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            case SQ: // Sequence of items
                                // zero or more items
                                // SequenceValueType.SQ
                                if (!isNull) {
                                    Sequence sequence = (Sequence) _value;
                                    {
                                        String info = "";
                                        for (int i = 0; i < currentLevel; i++) {
                                            info += ">";
                                        }

                                        info += TagUtils.toString(tag);
                                        info += " (" + dict.keywordOf(tag) + ")";
                                        if (currentLevel > 0) {
                                            info += " index=" + currentIndex;
                                        }
                                        info += " vr=" + vr.name() + " size=" + sequence.size();
                                        log.debug(info);
                                    }

                                    for (int i = 0; i < sequence.size(); i++) {
                                        collect(sequence.get(i), context, currentLevel + 1, i, doCollect);
                                    }
                                    return true;
                                }
                                value = compose("Sequence", "<null>", !doCollect);
                                break;

                            case UN: // Unknown
                                // StringValueType.BYTE
                                value = compose("Unknown", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;

                            default: //
                                value = compose("<unknown>", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), !doCollect);
                                break;
                        }
                    } catch (Throwable t) {
                        value = "## " + t.getMessage();
                    }

                    {
                        String info = "";
                        for (int i = 0; i < currentLevel; i++) {
                            info += ">";
                        }

                        info += TagUtils.toString(tag);
                        info += " (" + dict.keywordOf(tag) + ")";
                        if (currentLevel > 0) {
                            info += " index=" + currentIndex;
                        }
                        info += " vr=" + vr.name() + " value=" + value;
                        log.debug(info);
                    }

                    context.updateState(TagUtils.toString(tag), value);

                    /*
                    Map<String, String> collectedValues = dicomContext.getCollectedValues();
                    List<String> toRemove = new Vector<String>();
                    for (String key : collectedValues.keySet()) {
                        if (null == collectedValues.get(key)) {
                            toRemove.add(key);
                        }
                    }
                    for (String key : toRemove) {
                        collectedValues.remove(key);
                    }

                    if (! collectedValues.isEmpty()) {
                        // Create a package-relative path...
                        File top = new File("/");
                        File contentStream = top; // starting point relative to top

                        // ...and reassemble
                        int start = name.startsWith("/") ? 0 : 1; /* skip [example1]/content/... * /

                            String[] parts = name.split("/");
                            for (int i=start; i < parts.length; i++) {
                                contentStream = new File(contentStream, parts[i]);
                            }

                            String path = contentStream.getPath().replace("\\", "/");  // in case we're on Windoze
                            context.associate("DICOM", path, path, collectedValues);
                        }
                    }
                    */
                    return true;
                }
            }, /* visit nested? */ false);
        } catch (Throwable t) {
            String info = "Could not process dicom file: ";
            info += t.getMessage();
            log.info(info, t);
            throw new ProcessorException(info, t);
        }
    }

    /**
     * @return
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(me()).append("[ ");
        for (Map.Entry<String, String> stringStringEntry : configAttributes.entrySet()) {
            String value = stringStringEntry.getValue();
            buf.append(stringStringEntry.getKey()).append("=\"").append(null != value ? value : "").append("\" ");
        }
        buf.append("]{ ");
        for (Action action : actions) {
            buf.append(action);
            buf.append(" ");
        }
        buf.append("}");
        return buf.toString();
    }
}
