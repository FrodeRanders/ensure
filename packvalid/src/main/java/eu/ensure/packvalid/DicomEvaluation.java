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
package eu.ensure.packvalid;

import org.gautelis.vopn.io.Closer;
import org.gautelis.vopn.lang.Stacktrace;
import eu.ensure.packproc.BasicProcessorContext;
import eu.ensure.packproc.ProcessorException;
import eu.ensure.packproc.ProcessorManager;
import eu.ensure.packproc.internal.TrackingProcessorContext;
import eu.ensure.packproc.model.AssociatedInformation;
import eu.ensure.packproc.model.EvaluationStatement;
import eu.ensure.packproc.model.ProcessorContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * User: Frode Randers
 * Date: 2012-03-08
 */
public class DicomEvaluation {
    private static final Logger log = LogManager.getLogger(DicomEvaluation.class);

    private ProcessorManager manager = null;

    public DicomEvaluation(Class clazz, String localConfigName) {
        InputStream config = null;
        try {
            config = clazz.getResourceAsStream(localConfigName);
            init(config);

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to initiate processor manager: " + cause.getMessage();
            info += "\n" + Stacktrace.asString(cause);
            log.warn(info);

        } finally {
            Closer.close(config);
        }
    }

    public DicomEvaluation(InputStream config) {
        try {
            init(config);

        } catch (ProcessorException pe) {
            Throwable cause = Stacktrace.getBaseCause(pe);
            String info = "Failed to initiate processor manager: " + cause.getMessage();
            info += "\n" + Stacktrace.asString(cause);
            log.warn(info);
        }
    }

    private void init(InputStream config) throws ProcessorException {
        Properties properties = new Properties();
        manager = new ProcessorManager(properties, config);
        manager.prepare();
    }

    public Collection<? extends AssociatedInformation> evaluateAip(
            String purpose, File aip) throws IOException, ProcessorException {

        InputStream is = null;
        try {
            is = new FileInputStream(aip);
            return evaluateAip(purpose, aip.getName(), is);
        }
        finally {
            Closer.close(is);
        }
    }

    public Collection<? extends AssociatedInformation> evaluateAip(
            String purpose, String aipName, InputStream is) throws IOException, ProcessorException {

        OutputStream ignore = null;
        BasicProcessorContext context = new BasicProcessorContext(aipName);
        manager.apply(aipName, is, ignore, context);

        Collection<ProcessorContext> contextStack = context.getContextStack();
        if (!contextStack.isEmpty()) {
            String info = "Mismatched push/pop on ProcessorContext stack: ";
            info += "contains " + contextStack.size() + " elements";
            log.warn(info);
        }

        Collection<? extends AssociatedInformation> assocInfo;
        assocInfo = context.extractAssociatedInformation();
        /*
        if (log.isInfoEnabled()) {
            TrackingProcessorContext.debugAssociatedInformation(assocInfo);
        }
        */
        return assocInfo;
    }

    private String getUniqueValue(Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> values,
                      String key) throws ProcessorException {
        Map</* value */ String, /* claimants */ Set<String>> value = values.get(key);
        if (null == value) {
            // No value(s) associated with this key
            return null;
        }

        if (value.size() == 0) {
            String info = "There were no values associated with key \"" + key + "\"";
            throw new ProcessorException(info);
        }

        if (value.size() > 1) {
            String info = "Unexpectedly found multiple values corresponding to key \"" + key + "\": ";
            Iterator<String> vit = value.keySet().iterator();
            while (vit.hasNext()) {
                String v = vit.next();
                info += /* value */ v;
                {
                    info += " [";
                    Set<String> claimants = value.get(v);
                    Iterator<String> cit = claimants.iterator();
                    while (cit.hasNext()) {
                        info += cit.next();
                        if (cit.hasNext()) {
                            info += ", ";
                        }
                    }
                    info += "]";
                }
                if (vit.hasNext()) {
                    info += ", ";
                }
            }
            throw new ProcessorException(info);
        }

        Set<String> var = value.keySet();
        String uniqueValue = (var.toArray(new String[var.size()]))[0];
        if (null == uniqueValue || uniqueValue.length() == 0) {
            String info = "Unexpectedly found empty value associated with key \"" + key + "\"";
            throw new ProcessorException(info);
        }
        return uniqueValue;
    }

    private String getUniqueValue(AssociatedInformation info, String key) throws ProcessorException {
        return getUniqueValue(info.getValues(), key);
    }

    // Year 1 Demo - DICOM specific
    public void evaluateDelta(
            String purpose,
            Collection<? extends AssociatedInformation> originalAssoc,
            Collection<? extends AssociatedInformation> transformedAssoc
    ) {
        final String source = "DELTA";
        Collection<EvaluationStatement> evaluationStatements = TrackingProcessorContext.getEvaluationStatements();

        //-------------------------------------------------------
        // DICOM Information Hierarchy
        // (0010,0020) Patient ID (Patient level)
        // (0020,000D) Study Instance UID (Study level)
        // (0020,000E) Series Instance UID (Series level)
        // (0008,0018) SOP Instance UID (Image level)
        //
        // Derivation information
        // (0008,2111) Derivation Description
        //-------------------------------------------------------

        //-------------------------------------------------------
        // Prepare a hashtable with the original AIP SOP Instance UIDs (for searching) so that we may cross-reference
        // images between the original and the transformed AIP.
        Map<String, AssociatedInformation> originalDicomInfo = new HashMap<String, AssociatedInformation>();

        for (AssociatedInformation originalInfo : originalAssoc) {
            try {
                // Get (0008,0018) SOP Instance UID from original AIP for this file
                String sopInstanceUID = getUniqueValue(originalInfo, "0008,0018");
                if (null != sopInstanceUID && sopInstanceUID.length() > 0) {
                    originalDicomInfo.put(sopInstanceUID, originalInfo);
                }
            }
            catch (ProcessorException pe) {
                log.info(pe.getMessage(), pe);
            }
        }

        //-------------------------------------------------------
        // Now, perform cross-reference between original and transformed AIPs, based on shared (or non-shared)
        // SOP Instance UIDs.
        for (AssociatedInformation transformedInfo : transformedAssoc) {
            Map</* key */ String, Map</* value */ String, /* claimants */ Set<String>>> transformedValues = transformedInfo.getValues();

            try {
                // Get (0008,0018) SOP Instance UID from transformed AIP for this file
                String transformedSopInstanceUID = getUniqueValue(transformedInfo, "0008,0018");
                if (null != transformedSopInstanceUID && transformedSopInstanceUID.length() > 0) {
                    // Now, cross-reference with original SOP instance
                    AssociatedInformation originalInfo = originalDicomInfo.get(transformedSopInstanceUID);
                    if (null != originalInfo) {
                        // There exists an image in the original AIP that matches this image in the transformed AIP
                        String info = "SOPInstanceUID (0008,0018) of transformed file ";
                        String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                        if (null != transformedFileName && transformedFileName.length() > 0) {
                            info += transformedFileName;
                        }
                        info += " matches original file ";
                        String originalFilename = getUniqueValue(originalInfo, "fileName");
                        if (null != originalFilename && originalFilename.length() > 0) {
                            info += originalFilename;
                        }
                        log.debug(info);

                        //
                        String statement = source + "-processor states that ";
                        statement += info;
                        evaluationStatements.add(new EvaluationStatement(transformedInfo.getPath(), EvaluationStatement.POSITIVE, statement));

                        validateTransformation(
                                source, evaluationStatements, transformedInfo.getPath(),
                                originalInfo, transformedInfo, transformedSopInstanceUID
                        );

                    } else {
                        // New image, should be derived from some original!

                        // Get (0008,2111) Derivation Description from transformed AIP for this file
                        String derivationDescription = getUniqueValue(transformedInfo, "0008,2111");

                        if (null != derivationDescription && derivationDescription.length() > 0) {
                            String presumedOriginalSopInstanceUID = null;

                            // Typical value "PHILIPS UFS V1.5.4315 | Quality=2 | DWT=1 | Compressor=hulsken | ENSURE DP xform v1.0 | original=1.3.46.670589.45.1.1.129547003566848.1.6268.1305538332217.6 | region=(898,1606,1531,1444)"
                            String[] parts = derivationDescription.split("\\|");
                            for (String part : parts) {
                                part = part.trim();
                                if (part.startsWith("original=")) {
                                    presumedOriginalSopInstanceUID = part.substring(/* lengthOf("original=") */ 9).trim();
                                    break;
                                }
                            }

                            if (null != presumedOriginalSopInstanceUID && presumedOriginalSopInstanceUID.length() > 0) {
                                originalInfo = originalDicomInfo.get(presumedOriginalSopInstanceUID);
                                if (null != originalInfo) {
                                    // There exists an image in the original AIP that matches this derived image in the transformed AIP
                                    String info = "SOPInstanceUID (0008,0018) of transformed file ";
                                    String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                                    if (null != transformedFileName && transformedFileName.length() > 0) {
                                        info += transformedFileName;
                                    }
                                    info += " does (correctly) not match the original file ";
                                    String originalFilename = getUniqueValue(originalInfo, "fileName");
                                    if (null != originalFilename && originalFilename.length() > 0) {
                                        info += originalFilename;
                                    }
                                    info += ", from which it was derived";
                                    log.debug(info);


                                    //
                                    String statement = source + "-processor states that ";
                                    statement += info;
                                    evaluationStatements.add(new EvaluationStatement(transformedInfo.getPath(), EvaluationStatement.POSITIVE, statement));

                                    validateTransformation(
                                            source, evaluationStatements, transformedInfo.getPath(),
                                            originalInfo, transformedInfo, presumedOriginalSopInstanceUID
                                    );
                                } else {
                                    // There is no original for this transformed image
                                    String info = "SOPInstanceUID (0008,0018) of transformed file ";
                                    String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                                    if (null != transformedFileName && transformedFileName.length() > 0) {
                                        info += transformedFileName;
                                    }
                                    info += " refers to a non-existing SOPInstanceUID in the original AIP";
                                    log.debug(info);

                                    //
                                    String statement = source + " claims that ";
                                    statement += info;
                                    evaluationStatements.add(new EvaluationStatement(transformedInfo.getPath(), EvaluationStatement.NEGATIVE, statement));
                                }

                            } else {
                                String info = "a DICOM image was added (";
                                String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                                if (null != transformedFileName && transformedFileName.length() > 0) {
                                    info += transformedFileName;
                                }
                                info += "), that does not derive from any original image ";
                                log.warn(info);

                                //
                                String statement = source + "-processor states that ";
                                statement += info;
                                evaluationStatements.add(new EvaluationStatement(transformedInfo.getPath(), EvaluationStatement.NEGATIVE, statement));
                            }
                        } else {
                            String info = "there is no DerivationDescription (0008,2111) for DICOM image modified or added in transformation (";
                            String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                            if (null != transformedFileName && transformedFileName.length() > 0) {
                                info += transformedFileName;
                            }
                            info += ")";
                            log.warn(info);

                            //
                            String statement = source + "-processor states that ";
                            statement += info;
                            evaluationStatements.add(new EvaluationStatement(transformedInfo.getPath(), EvaluationStatement.NEGATIVE, statement));
                        }
                    }
                }
            }
            catch (ProcessorException pe) {
                log.info(pe.getMessage(), pe);
            }
        }
    }

    // Year 1 Demo - DICOM specific
    private void validateTransformation(String source,
                                        Collection<EvaluationStatement> evaluationStatements,
                                        String path,
                                        AssociatedInformation originalInfo,
                                        AssociatedInformation transformedInfo,
                                        String sopInstanceUID) throws ProcessorException {

        // Possibly same image, check info hierarchy
        {
            String transformedSeriesInstanceUID = getUniqueValue(transformedInfo, "0020,000E");
            String originalSeriesInstanceUID = getUniqueValue(originalInfo, "0020,000E");

            if (null != transformedSeriesInstanceUID && transformedSeriesInstanceUID.length() > 0 &&
                null != originalSeriesInstanceUID && originalSeriesInstanceUID.length() > 0 &&
                transformedSeriesInstanceUID.equalsIgnoreCase(originalSeriesInstanceUID)) {

                String info = "SeriesInstanceUID (0020,000E) of transformed file ";
                String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                if (null != transformedFileName && transformedFileName.length() > 0) {
                    info += transformedFileName;
                }
                info += " matches original file ";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.POSITIVE, statement));

            } else {
                String info = "SeriesInstanceUID (0020,000E) differs between original and transformation for DICOM image (";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                info += ")";
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEGATIVE, statement));
            }
        }
        {
            String transformedStudyInstanceUID = getUniqueValue(transformedInfo, "0020,000D");
            String originalStudyInstanceUID = getUniqueValue(originalInfo, "0020,000D");

            if (null != transformedStudyInstanceUID && transformedStudyInstanceUID.length() > 0 &&
                null != originalStudyInstanceUID && originalStudyInstanceUID.length() > 0 &&
                transformedStudyInstanceUID.equalsIgnoreCase(originalStudyInstanceUID)) {

                String info = "StudyInstanceUID (0020,000D) of transformed file ";
                String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                if (null != transformedFileName && transformedFileName.length() > 0) {
                    info += transformedFileName;
                }
                info += " matches original file ";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.POSITIVE, statement));

            } else {
                String info = "StudyInstanceUID (0020,000D) differs between original and transformation for DICOM image ";
                info += sopInstanceUID + "(";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                info += ")";
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEGATIVE, statement));
            }
        }
        {
            String transformedPatientID = getUniqueValue(transformedInfo, "0010,0020");
            String originalPatientID = getUniqueValue(originalInfo, "0010,0020");

            if (null != transformedPatientID && transformedPatientID.length() > 0 &&
                null != originalPatientID && originalPatientID.length() > 0 &&
                transformedPatientID.equalsIgnoreCase(originalPatientID)) {

                String info = "PatientID (0010,0020) of transformed file ";
                String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                if (null != transformedFileName && transformedFileName.length() > 0) {
                    info += transformedFileName;
                }
                info += " matches original file ";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.POSITIVE, statement));

            } else {
                String info = "PatientID (0010,0020) differs between original and transformation for DICOM image ";
                info += sopInstanceUID + "(";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                info += ")";
                log.warn(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEGATIVE, statement));
            }
        }

        // Checksums
        {
            String transformedChecksum = getUniqueValue(transformedInfo, "SHA-512");
            String originalChecksum = getUniqueValue(originalInfo, "SHA-512");

            if (null != transformedChecksum && transformedChecksum.length() > 0 &&
                null != originalChecksum && originalChecksum.length() > 0 &&
                transformedChecksum.equalsIgnoreCase(originalChecksum)) {

                String info = "checksum (SHA-512) of transformed file ";
                String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                if (null != transformedFileName && transformedFileName.length() > 0) {
                    info += transformedFileName;
                }
                info += " matches original file ";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.POSITIVE, statement));

            } else {
                String info = "checksum (SHA-512) differs between original (";
                String originalFilename = getUniqueValue(originalInfo, "fileName");
                if (null != originalFilename && originalFilename.length() > 0) {
                    info += originalFilename;
                }
                info += ") and transformed file (";
                String transformedFileName = getUniqueValue(transformedInfo, "fileName");
                if (null != transformedFileName && transformedFileName.length() > 0) {
                    info += transformedFileName;
                }
                info += ")";
                log.debug(info);

                //
                String statement = source + "-processor states that ";
                statement += info;
                evaluationStatements.add(new EvaluationStatement(path, EvaluationStatement.NEUTRAL, statement));
            }
        }
        log.debug("");
    }
}
