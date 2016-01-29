package eu.ensure.visualizr.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcm4che3.data.*;
import org.dcm4che3.util.TagUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by froran on 2016-01-28.
 */
public class DicomObject {
    private static final Logger log = LogManager.getLogger(DicomObject.class);

    private static final boolean isVerbose = false;

    private final String name;
    private String heuristicName = null;
    private final Attributes attributes;
    private final List<DicomObject> sequences = new ArrayList<>();
    private final List<DicomTag> tags = new ArrayList<>();

    public DicomObject(String name, Attributes attributes) {
        this.name = name;
        this.attributes = attributes;
        populate(tags);

        String sopClassUID = sopClassUID(attributes);
        if (null == sopClassUID) {
            String recordType = directoryRecordType(attributes);
            heuristicName = recordType;

        } else {
            DicomFile.Type type = DicomFile.Type.find(sopClassUID);
            heuristicName = type.getDescription();
        }
    }

    public String getName() {
        return (null != heuristicName ? heuristicName : name);
    }

    public List<DicomObject> getSequences() {
        return sequences;
    }

    private String compose(String description, Object actualValue, boolean verbose) {
        return (verbose ? "{" + description + "} " + actualValue : "" + actualValue);
    }

    public String getSopClassUID() {
        return sopClassUID(attributes);
    }


    public List<DicomTag> getDicomTags() {
        return tags;
    }

    public static String directoryRecordType(final Attributes dataset) {
        String directoryRecordType = dataset.getString(TagUtils.toTag(0x0004, 0x1430));
        if (null != directoryRecordType) {
            log.debug("directoryRecordType = " + directoryRecordType);
        }
        return directoryRecordType;
    }

    public static String patientID(final Attributes dataset) {
        String patientId = dataset.getString(TagUtils.toTag(0x0010, 0x0020));
        if (null != patientId) {
            log.debug("patientID = " + patientId);
        }
        return patientId;
    }

    public static String studyInstanceUID(final Attributes dataset) {
        String studyInstanceUid = dataset.getString(TagUtils.toTag(0x0020, 0x000D));
        if (null != studyInstanceUid) {
            log.debug("studyInstanceUID = " + studyInstanceUid);
        }
        return studyInstanceUid;
    }

    public static String seriesInstanceUID(final Attributes dataset) {
        String seriesInstanceUid = dataset.getString(TagUtils.toTag(0x0020, 0x000E));
        if (null != seriesInstanceUid) {
            log.debug("seriesInstanceUID = " + seriesInstanceUid);
        }
        return seriesInstanceUid;
    }

    public static String seriesDescription(final Attributes dataset) {
        String seriesDescription = dataset.getString(TagUtils.toTag(0x0008, 0x103E));
        if (null != seriesDescription) {
            log.debug("seriesDescription = " + seriesDescription);
        }
        return seriesDescription;
    }

    public static String sopInstanceUID(final Attributes dataset) {
        String sopInstanceUid = dataset.getString(TagUtils.toTag(0x0008, 0x0018));
        if (null != sopInstanceUid) {
            log.debug("SOPInstanceUID = " + sopInstanceUid);
        }
        return sopInstanceUid;
    }

    public static String sopClassUID(final Attributes dataset) {
        String sopClassUid = dataset.getString(TagUtils.toTag(0x0008, 0x0016));
        if (null != sopClassUid) {
            log.debug("SOPClassUID = " + sopClassUid);
        }
        return sopClassUid;
    }

    public static String modality(final Attributes dataset) {
        String modality = dataset.getString(TagUtils.toTag(0x0008, 0x0060));
        if (null != modality) {
            log.debug("modality = " + modality);
        }
        return modality;
    }

    public static String performingPhysicianName(final Attributes dataset) {
        String performingPhysicianName = dataset.getString(TagUtils.toTag(0x0008, 0x1050));
        if (null != performingPhysicianName) {
            log.debug("performingPhysicianName = " + performingPhysicianName);
        }
        return performingPhysicianName;
    }

    private void populate(List<DicomTag> tags) {
        SpecificCharacterSet characterSet = attributes.getSpecificCharacterSet();
        ElementDictionary dict = ElementDictionary.getStandardElementDictionary();
        boolean isBE = attributes.bigEndian();

        try {
            attributes.accept(new Attributes.Visitor() {
                public boolean visit(Attributes attributes, int tag, VR vr, Object _value) throws Exception {

                    boolean isNull = (_value instanceof Value && _value == Value.NULL);

                    String value = "";
                    try {
                        switch (vr) {
                            /****************
                             * String
                             ****************/
                            case AE: // Application Entity
                                // Character data [Naming devices, people, and instances]
                                // StringValueType.ASCII
                                value = compose("Application entity", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case AS: // Age string
                                // Character data [Date and time]
                                // Format: nnnW or nnnM or nnnY
                                // StringValueType.ASCII
                                value = compose("Age string", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case AT: // Attribute tag
                                // Two 2-byte integers [Numbers in binary format]
                                // Format: gggg,eeee
                                // BinaryValueType.TAG
                                value = "";
                            {
                                Object o = vr.toStrings(_value, isBE, characterSet);
                                if (isNull) {
                                    value += "<null>";
                                } else if (o instanceof String[]) {
                                    for (String s : (String[]) o) {
                                        int _tag = Integer.parseInt(s, 16);
                                        value += TagUtils.toString(_tag);
                                        value += " (" + dict.keywordOf(_tag) + "), ";
                                    }
                                } else {
                                    int _tag = Integer.parseInt((String)o, 16);
                                    value += TagUtils.toString(_tag);
                                    value += " (" + dict.keywordOf(_tag) + ")";
                                }
                            }
                            value = compose("Attribute tag", value, isVerbose);
                            break;

                            case DT: // Date time
                                // Character data [Date and time]
                                // Format: YYYYMMDDHHMMSS.FFFFFF&ZZZZ (&ZZZ is optional & = + or -)
                                // StringValueType.DT
                                value = compose("Date and time", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case IS: // Integer string
                                // Character data [Numbers in text format]
                                // Integer encoded as string. May be padded
                                // StringValueType.IS
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
                                    value += o;
                                }
                            }
                            value = compose("Integer string", value, isVerbose);
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
                                    value += o;
                                }
                            }
                            value = compose("Long string", value, isVerbose);
                            break;

                            case LT: // Long text
                                // Character data, Max length: 10,240 [Text]
                                // NOTE: Leading spaces are significant, trailing spaces are not
                                // StringValueType.TEXT
                                value = compose("Long text", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case SH: // Short string
                                // Character data, Max length: 16 [Text]
                                // NOTE: may be padded
                                // StringValueType.STRING
                                value = compose("Short string", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case TM: // Time
                                // Format: hhmmss.frac (or older format: hh:mm:ss.frac)
                                // StringValueType.TM
                                value = compose("Time", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case UI: // Unique identifier (UID)
                                // Character data [Naming devices, people, and instances]
                                // Format: delimiter = ., 0-9 characters only, trailing space to make even number
                                // StringValueType.ASCII
                                value = compose("Unique identifier", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case UT: // Unlimited text
                                // Character data, Max length: 4,294,967,294 [Text]
                                // NOTE: Trailing spaces ignored
                                // StringValueType.TEXT
                                value = compose("Unlimited text", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case UC: // Unlimited characters
                                // StringValueType.STRING
                                value = compose("Unlimited characters", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case UR: // URI or URL
                                // StringValueType.UR
                                value = compose("URI/URL", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
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
                                    value += o;
                                }
                            }
                            value = compose("Code string", value, isVerbose);
                            break;

                            case PN: // Person name
                                // Character data [Naming devices, people, and instances]
                                // NOTE: 64 byte max per component, 5 components with delimiter = ^
                                // StringValueType.PN
                                value = compose("Person name", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case ST: // Short text
                                // Character data, Max length: 1024 [Text]
                                // StringValueType.TEXT
                                value = compose("Short text", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            /****************
                             * Date
                             ****************/
                            case DA: // Date
                                // Eight characters [Date and time]
                                // Format: yyyymmdd (check for yyyy.mm.dd also and convert)
                                // StringValueType.DA
                                value = compose("Date", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
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
                                    value += o;
                                }
                            }
                            value = compose("Decimal string", value, isVerbose);
                            break;

                            /****************
                             * double
                             ****************/
                            case FD: // Floating point double
                                // 8-byte floating point [Numbers in binary format]
                                // Double precision floating point number (double)
                                // BinaryValeuType.DOUBLE
                                value = compose("Floating point double", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            /****************
                             * float
                             ****************/
                            case FL: // Floating point single
                                // 4-byte floating point [Numbers in binary format]
                                // Single precision floating point number (float)
                                // BinaryValueType.FLOAT
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
                                    value += o;
                                }
                            }
                            value = compose("Floating point single", value, isVerbose);
                            break;

                            /****************
                             * int
                             ****************/
                            case SL: // Signed long
                                // 4-byte integer [Numbers in binary format]
                                // BinaryValueType.INT
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
                                    value += o;
                                }
                            }
                            value = compose("Signed long", value, isVerbose);
                            break;

                            case US: // Unsigned short
                                // 2-byte integer [Numbers in binary format]
                                // BinaryValueType.USHORT
                            {
                                byte[] _us = vr.toBytes(_value, characterSet);
                                value = "";
                                if (_us.length <= 80) {
                                    for (byte b : _us) {
                                        value += "" + b + ", ";
                                    }
                                } else {
                                    value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_us.length) + ">";
                                }
                                value = compose("Unsigned short", value, isVerbose);
                            }
                            break;

                            /****************
                             * short
                             ****************/
                            case SS: // Signed short
                                // 2-byte integer [Numbers in binary format]
                                // BinaryValueType.SHORT
                                value = compose("Signed short", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            /****************
                             * long
                             ****************/
                            case UL: // Unsigned long
                                // 4-byte integer [Numbers in binary format]
                                // BinaryValueType.INT
                            {
                                int[] _ul = vr.toInts(_value, isBE);
                                value = "";
                                if (_ul.length <= 80) {
                                    for (int i : _ul) {
                                        value += "" + i + ", ";
                                    }
                                } else {
                                    value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_ul.length) + ">";
                                }
                                value = compose("Unsigned long", value, isVerbose);
                            }
                            break;

                            /****************
                             * NO VALUE!
                             ****************/
                            case OB: // Other byte string
                                // 1-byte integers [Numbers in binary format]
                                // NOTE: Has single trailing 0x00 to make even number of bytes. Transfer Syntax determines length
                                // BinaryValueType.BYTE
                            {
                                byte[] _ob = vr.toBytes(_value, characterSet);
                                value = "";
                                if (_ob.length <= 80) {
                                    for (byte b : _ob) {
                                        value += "" + b + ", ";
                                    }
                                } else {
                                    value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_ob.length) + ">";
                                }
                                value = compose("Other byte", value, isVerbose);
                            }
                            break;

                            case OD: // Other double string
                                // BinaryValyeType.DOUBLE
                                value = compose("Other byte", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            case OF: // Other float string
                                // 4-byte floating point [Numbers in binary format]
                                // BinaryValueType.FLOAT
                                value = compose("Other float", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;

                            //case OL: // Other long string
                            // BinaryValueType.INT
                            //    value = "Other long} " + (isNull ? "<null>" : _value)
                            //     break;

                            case OW: // Other word string
                                // 2-byte integers [Numbers in binary format]
                                // Max length: -
                                // BinaryValueType.SHORT
                            {
                                byte[] _ow = vr.toBytes(_value, characterSet);
                                value = "";
                                if (_ow.length <= 80) {
                                    for (byte b : _ow) {
                                        value += "" + b + ", ";
                                    }
                                } else {
                                    value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_ow.length) + ">";
                                }
                                value = compose("Other word", value, isVerbose);
                            }
                            break;

                            case SQ: // Sequence of items
                                // zero or more items
                                // SequenceValueType.SQ
                                if (!isNull) {
                                    Sequence sequence = (Sequence) _value;
                                    String name = dict.keywordOf(tag);
                                    log.debug("Found sequence: " + name);

                                    for (Attributes sequenceAttributes : sequence) {
                                        log.debug("Loading sub-sequence (have " + sequences.size() + " already)");
                                        sequences.add(new DicomObject(name, sequenceAttributes));
                                    }
                                }
                                return true;

                            case UN: // Unknown
                                // BinaryValueType.BYTE
                            {
                                byte[] _un = vr.toBytes(_value, characterSet);
                                if (_un.length <= 80) {
                                    value = new String(_un, "ISO-8859-1");
                                } else {
                                    value = "<data size=" + eu.ensure.commons.lang.Number.asHumanApproximate(_un.length) + ">";
                                }
                                value = compose("Unknown", value, isVerbose);
                            }
                            break;

                            default: //
                                value = compose("<unknown>", (isNull ? "<null>" : vr.toStrings(_value, isBE, characterSet)), isVerbose);
                                break;
                        }

                        String keyword = dict.keywordOf(tag);
                        if (null != keyword && keyword.length() > 0 && keyword.contains("SOPClassUID")) {
                            DicomFile.Type type = DicomFile.Type.find(value);
                            value = value + "\n(" + type.getDescription() + ")";
                        }
                        tags.add(new DicomTag(tag, value));

                    } catch (Throwable t) {
                        String info = "Could not determine value of tag " + TagUtils.toString(tag) + " " + dict.keywordOf(tag) + " (" + vr.name() + "): ";
                        info += t.getMessage();
                        log.warn(info);
                    }

                    return true;
                }
            }, /* visit nested? */ false);

        } catch (Throwable t) {
            String info = "Could not process dicom file: ";
            info += t.getMessage();
            log.info(info, t);
        }
    }
}
