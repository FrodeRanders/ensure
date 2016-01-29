package eu.ensure.visualizr.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.util.TagUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by froran on 2016-01-28.
 */
public class DicomLoader {
    private static final Logger log = LogManager.getLogger(DicomLoader.class);

    private List<DicomFile> loadedFiles = new ArrayList<DicomFile>();
    private String fileName;

    /**
     * Load a DICOM file.
     *
     * @param dicomFile
     * @throws IOException In case of IO error.
     */
    public DicomLoader(File dicomFile) throws IOException {
        fileName = dicomFile.getName();

        try (DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(dicomFile))) {
            Attributes ds = dicomInputStream.readDataset(-1, -1);

            if ("DICOMDIR".equals(fileName)) {
                loadDICOMDIR(ds, dicomFile);
            }
        }
    }

    /**
     * List all the classes of the scanned jar.
     *
     * @return The list of classes.
     */
    public List<DicomFile> getFiles() {
        return this.loadedFiles;
    }

    /**
     * Return true if we failed to read file
     *
     * @return Return true if an error encoured.
     */
    public boolean failure() {
        return false;
    }

    public String getFileName() {
        return this.fileName;
    }



    /**
     * >(0008,0021) (SeriesDate) index=8 vr=DA value={Date} 20140522
     * >(0008,0023) (ContentDate) index=8 vr=DA value={Date} 20140522
     * >(0008,0031) (SeriesTime) index=8 vr=TM value={Time} 101638.000
     * >(0008,0033) (ContentTime) index=8 vr=TM value={Time} 101638.000
     * >(0008,0060) (Modality) index=8 vr=CS value={Code string} SR
     * >(0008,0080) (InstitutionName) index=8 vr=LO value={Long string} NLL
     * >(0008,1010) (StationName) index=8 vr=SH value={Short string} MMK1
     * >(0008,103E) (SeriesDescription) index=8 vr=LO value={Long string} Anamnes
     * >(0008,1050) (PerformingPhysicianName) index=8 vr=SH value={Short string} <null>
     * >(0020,000D) (StudyInstanceUID) index=8 vr=UI value={Unique identifier} 1.2.752.99.1.1.1.0140131203
     * >(0020,000E) (SeriesInstanceUID) index=8 vr=UI value={Unique identifier} 1.2.276.0.69.10.29.1.2135.20140522101638561.253295
     * >(0020,0011) (SeriesNumber) index=8 vr=IS value={Integer string} 1
     * >(0020,0013) (InstanceNumber) index=8 vr=IS value={Integer string} 3
     * >(0040,0275) (RequestAttributesSequence) index=8 vr=SQ size=1
     * >>(0040,1001) (RequestedProcedureID) index=0 vr=SH value={Short string} 0140131203
     * >(0040,A043) (ConceptNameCodeSequence) index=8 vr=SQ size=1
     * >>(0008,0100) (CodeValue) index=0 vr=SH value={Short string} 111400
     * >>(0008,0102) (CodingSchemeDesignator) index=0 vr=SH value={Short string} DCM
     * >>(0008,0103) (CodingSchemeVersion) index=0 vr=SH value={Short string} 1.0
     * >>(0008,0104) (CodeMeaning) index=0 vr=LO value={Long string} Breast Imaging Report
     * >(0040,A491) (CompletionFlag) index=8 vr=CS value={Code string} COMPLETE
     * >(0040,A493) (VerificationFlag) index=8 vr=CS value={Code string} VERIFIED
     */
    public void loadDICOMDIR(final Attributes dataset, final File file) throws IOException {

        Map<String, String> data = new HashMap<>();
        DicomFile dicomdirFile = new DicomFile(new DicomObject("DICOMDIR", dataset), file);
        loadedFiles.add(dicomdirFile);

        // (0004,1220) (DirectoryRecordSequence)
        Sequence sequence = dataset.getSequence(TagUtils.toTag(0x0004, 0x1220));
        if (null != sequence) {
            for (int i = 0; i < sequence.size(); i++) {
                Attributes record = sequence.get(i);
                String recordType = DicomObject.directoryRecordType(record);

                switch (recordType) {
                    case "PATIENT":
                        data.put("PatientID", DicomObject.patientID(record));
                        break;

                    case "STUDY":
                        data.put("StudyInstanceUID", DicomObject.studyInstanceUID(record));
                        break;

                    case "SR DOCUMENT": {
                        String seriesInstanceUid = DicomObject.seriesInstanceUID(record);
                        String seriesDescription = DicomObject.seriesDescription(record);
                        String sopInstanceUid = DicomObject.sopInstanceUID(record);
                        String modality = DicomObject.modality(record);
                        String physicianName = DicomObject.performingPhysicianName(record);

                        File referencedFile = file.getParentFile(); // Start relative to DICOMDIR
                        String[] referencedFileId = record.getStrings(TagUtils.toTag(0x0004, 0x1500));
                        if (null != referencedFileId) {
                            for (String part : referencedFileId) {
                                referencedFile = new File(referencedFile, part);
                            }
                        }

                        if (referencedFile.exists() && referencedFile.canRead()) {
                            String info = "Referenced file: " + referencedFile.getPath();
                            log.debug(info);

                            try (DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(referencedFile))) {
                                Attributes ds = dicomInputStream.readDataset(-1, -1);
                                loadFile(ds, referencedFile, dicomdirFile);
                            }
                        } else {
                            String info = "Referenced file does not exist: " + referencedFile.getPath();
                            log.warn(info);
                        }
                    }
                    break;

                    case "IMAGE": {
                        String seriesInstanceUid = DicomObject.seriesInstanceUID(record);
                        String seriesDescription = DicomObject.seriesDescription(record);
                        String sopInstanceUid = DicomObject.sopInstanceUID(record);
                        String modality = DicomObject.modality(record);
                        String physicianName = DicomObject.performingPhysicianName(record);

                        File referencedFile = file.getParentFile(); // Start relative to DICOMDIR
                        String[] referencedFileId = record.getStrings(TagUtils.toTag(0x0004, 0x1500));
                        if (null != referencedFileId) {
                            for (String part : referencedFileId) {
                                referencedFile = new File(referencedFile, part);
                            }
                        }

                        if (referencedFile.exists() && referencedFile.canRead()) {
                            String info = "Referenced file: " + referencedFile.getPath();
                            log.debug(info);

                            try (DicomInputStream dicomInputStream = new DicomInputStream(new FileInputStream(referencedFile))) {
                                Attributes ds = dicomInputStream.readDataset(-1, -1);
                                loadFile(ds, referencedFile, dicomdirFile);
                            }
                        } else {
                            String info = "Referenced file does not exist: " + referencedFile.getPath();
                            log.warn(info);
                        }
                    }
                    break;

                    case "SERIES":
                    default:
                        break;
                }

                log.debug("------------------------------------------------------------------------------------------");
            }
        }
    }

    public void loadFile(final Attributes dataset, final File file, final DicomFile parentFile) throws IOException {

        Map<String, String> data = new HashMap<>();
        DicomFile dicomFile = new DicomFile(new DicomObject(file.getName(), dataset), file, parentFile);
        loadedFiles.add(dicomFile);

        Sequence sequence = dataset.getSequence(TagUtils.toTag(0x0004, 0x1220));
        if (null != sequence) {
            for (int i = 0; i < sequence.size(); i++) {
                Attributes record = sequence.get(i);

                log.debug("------------------------------------------------------------------------------------------");
            }
        }
    }
}
